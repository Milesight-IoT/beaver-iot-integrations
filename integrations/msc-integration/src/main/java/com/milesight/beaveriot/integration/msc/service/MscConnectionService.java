package com.milesight.beaveriot.integration.msc.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.security.TenantContext;
import com.milesight.beaveriot.eventbus.annotations.EventSubscribe;
import com.milesight.beaveriot.eventbus.api.Event;
import com.milesight.beaveriot.integration.msc.entity.MscConnectionPropertiesEntities;
import com.milesight.beaveriot.integration.msc.model.IntegrationStatus;
import com.milesight.beaveriot.pubsub.MessagePubSub;
import com.milesight.beaveriot.pubsub.api.annotation.MessageListener;
import com.milesight.beaveriot.pubsub.api.message.RemoteBroadcastMessage;
import com.milesight.cloud.sdk.client.retrofit2.RFC3339DateFormat;
import com.milesight.msc.sdk.MscClient;
import com.milesight.msc.sdk.config.Credentials;
import com.milesight.msc.sdk.error.MscApiException;
import com.milesight.msc.sdk.utils.LongStringNumberDeserializer;
import lombok.*;
import lombok.extern.slf4j.*;
import okhttp3.ConnectionPool;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;


@Slf4j
@Component
public class MscConnectionService implements IMscClientProvider {

    private static final String OPENAPI_STATUS_KEY = MscConnectionPropertiesEntities.getKey(MscConnectionPropertiesEntities.Fields.openapiStatus);
    private static final ConnectionPool HTTP_CONNECTION_POOL = new ConnectionPool(5, 65, TimeUnit.SECONDS);
    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
            .serializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(MapperFeature.ALLOW_COERCION_OF_SCALARS, false)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, true)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
            .enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING)
            .defaultDateFormat(new RFC3339DateFormat())
            .addModule(new SimpleModule().addDeserializer(Long.class, new LongStringNumberDeserializer()))
            .addModule(new JavaTimeModule())
            .addModule(new JsonNullableModule())
            .build();

    private final Map<String, MscClient> tenantIdToMscClient = new ConcurrentHashMap<>();
    @Autowired
    private EntityValueServiceProvider entityValueServiceProvider;
    @Autowired
    private MessagePubSub messagePubSub;

    @EventSubscribe(payloadKeyExpression = "msc-integration.integration.openapi.*")
    public void onOpenapiPropertiesUpdate(Event<MscConnectionPropertiesEntities.Openapi> event) {
        val tenantId = TenantContext.getTenantId();
        val openapiSettings = event.getPayload();
        val configChanged = isConfigChanged(event);
        if (configChanged) {
            initConnection(tenantId, openapiSettings.getServerUrl(), openapiSettings.getClientId(), openapiSettings.getClientSecret());
            updateConnectionStatus(IntegrationStatus.NOT_READY);
        }
        testConnection(tenantId);

        messagePubSub.publishAfterCommit(MscClientUpdateEvent.builder()
                .tenantId(tenantId)
                .clientId(openapiSettings.getClientId())
                .serverUrl(openapiSettings.getServerUrl())
                .clientSecret(openapiSettings.getClientSecret())
                .build());
    }

    @MessageListener
    public void onMscClientUpdated(MscClientUpdateEvent event) {
        val client = tenantIdToMscClient.get(event.tenantId);
        if (client != null
                && Objects.equals(client.getConfig().getEndpoint(), event.serverUrl)
                && Objects.equals(client.getConfig().getCredentials().getClientId(), event.clientId)
                && Objects.equals(client.getConfig().getCredentials().getClientSecret(), event.clientSecret)) {
            log.debug("msc client settings have not been changed: '{}'", event.tenantId);
            return;
        }
        log.info("recreate the msc client for tenant '{}'", event.tenantId);
        initConnection(event.tenantId, event.serverUrl, event.clientId, event.clientSecret);
    }

    public void updateConnectionStatus(IntegrationStatus status) {
        entityValueServiceProvider.saveValuesAndPublishSync(new ExchangePayload(Map.of(OPENAPI_STATUS_KEY, status.name())));
    }

    private void initConnection(String tenantId, String serverUrl, String clientId, String clientSecret) {
        tenantIdToMscClient.put(tenantId, MscClient.builder()
                .objectMapper(OBJECT_MAPPER)
                .httpConnectionPool(HTTP_CONNECTION_POOL)
                .endpoint(serverUrl)
                .credentials(Credentials.builder()
                        .clientId(clientId)
                        .clientSecret(clientSecret)
                        .build())
                .build());
    }

    private void testConnection(String tenantId) {
        try {
            val mscClient = tenantIdToMscClient.get(tenantId);
            mscClient.test();
            updateConnectionStatus(IntegrationStatus.READY);
        } catch (Exception e) {
            log.error("Error occurs while testing connection", e);
            updateConnectionStatus(IntegrationStatus.ERROR);
            var errorMessage = "Connection failed.";
            if (e instanceof MscApiException) {
                errorMessage = errorMessage + " Response: " + e.getMessage();
            }
            throw ServiceException
                    .with(ErrorCode.SERVER_ERROR.getErrorCode(), errorMessage)
                    .build();
        }
    }

    private boolean isConfigChanged(Event<MscConnectionPropertiesEntities.Openapi> event) {
        // check if required fields are set
        if (event.getPayload().getServerUrl() == null) {
            return false;
        }
        if (event.getPayload().getClientId() == null) {
            return false;
        }
        if (event.getPayload().getClientSecret() == null) {
            return false;
        }
        // check if mscClient is initiated
        val tenantId = TenantContext.getTenantId();
        val mscClient = tenantIdToMscClient.get(tenantId);
        if (mscClient == null) {
            return true;
        }
        if (mscClient.getConfig() == null) {
            return true;
        }
        if (mscClient.getConfig().getCredentials() == null) {
            return true;
        }
        // check if endpoint, clientId or clientSecret changed
        if (!Objects.equals(mscClient.getConfig().getEndpoint(), event.getPayload().getServerUrl())) {
            return true;
        }
        if (!Objects.equals(mscClient.getConfig().getCredentials().getClientId(), event.getPayload().getClientId())) {
            return true;
        }
        return !Objects.equals(mscClient.getConfig().getCredentials().getClientSecret(), event.getPayload().getClientSecret());
    }

    public void init(String tenantId) {
        try {
            val settings = entityValueServiceProvider.findValuesByKey(
                    MscConnectionPropertiesEntities.getKey(MscConnectionPropertiesEntities.Fields.openapi), MscConnectionPropertiesEntities.Openapi.class);
            if (!settings.isEmpty()) {
                initConnection(tenantId, settings.getServerUrl(), settings.getClientId(), settings.getClientSecret());
                testConnection(tenantId);
            }
        } catch (Exception e) {
            log.error("Error occurs while initializing connection", e);
            updateConnectionStatus(IntegrationStatus.NOT_READY);
        }
    }

    public void disable(String tenantId) {
        updateConnectionStatus(IntegrationStatus.NOT_READY);
        tenantIdToMscClient.remove(tenantId);
    }

    public MscClient getMscClient() {
        return tenantIdToMscClient.get(TenantContext.getTenantId());
    }

    public void stop() {
        tenantIdToMscClient.clear();
    }

    @Data
    @ToString(callSuper = true)
    @EqualsAndHashCode(callSuper = true)
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MscClientUpdateEvent extends RemoteBroadcastMessage {
        private String tenantId;
        private String serverUrl;
        private String clientId;
        private String clientSecret;
    }

}
