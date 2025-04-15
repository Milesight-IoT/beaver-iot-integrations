package com.milesight.beaveriot.integration.msc.service;

import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.security.TenantContext;
import com.milesight.beaveriot.eventbus.annotations.EventSubscribe;
import com.milesight.beaveriot.eventbus.api.Event;
import com.milesight.beaveriot.integration.msc.entity.MscConnectionPropertiesEntities;
import com.milesight.beaveriot.integration.msc.model.IntegrationStatus;
import com.milesight.msc.sdk.MscClient;
import com.milesight.msc.sdk.config.Credentials;
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@Component
public class MscConnectionService implements IMscClientProvider {

    private static final String OPENAPI_STATUS_KEY = MscConnectionPropertiesEntities.getKey(MscConnectionPropertiesEntities.Fields.openapiStatus);

    @Autowired
    private EntityValueServiceProvider entityValueServiceProvider;

    private final Map<String, MscClient> tenantIdToMscClient = new ConcurrentHashMap<>();

    @EventSubscribe(payloadKeyExpression = "msc-integration.integration.openapi.*")
    public void onOpenapiPropertiesUpdate(Event<MscConnectionPropertiesEntities.Openapi> event) {
        val tenantId = TenantContext.getTenantId();
        if (isConfigChanged(event)) {
            val openapiSettings = event.getPayload();
            initConnection(tenantId, openapiSettings);
            entityValueServiceProvider.saveValuesAndPublishSync(new ExchangePayload(Map.of(OPENAPI_STATUS_KEY, IntegrationStatus.NOT_READY.name())));
        }
        testConnection(tenantId);
    }

    private void initConnection(String tenantId, MscConnectionPropertiesEntities.Openapi openapiSettings) {
        tenantIdToMscClient.put(tenantId, MscClient.builder()
                .endpoint(openapiSettings.getServerUrl())
                .credentials(Credentials.builder()
                        .clientId(openapiSettings.getClientId())
                        .clientSecret(openapiSettings.getClientSecret())
                        .build())
                .build());
    }

    private void testConnection(String tenantId) {
        try {
            val mscClient = tenantIdToMscClient.get(tenantId);
            mscClient.test();
            entityValueServiceProvider.saveValuesAndPublishSync(new ExchangePayload(Map.of(OPENAPI_STATUS_KEY, IntegrationStatus.READY.name())));
        } catch (Exception e) {
            log.error("Error occurs while testing connection", e);
            entityValueServiceProvider.saveValuesAndPublishSync(new ExchangePayload(Map.of(OPENAPI_STATUS_KEY, IntegrationStatus.ERROR.name())));
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
                initConnection(tenantId, settings);
                testConnection(tenantId);
            }
        } catch (Exception e) {
            log.error("Error occurs while initializing connection", e);
            entityValueServiceProvider.saveValuesAndPublishSync(new ExchangePayload(Map.of(OPENAPI_STATUS_KEY, IntegrationStatus.NOT_READY.name())));
        }
    }

    public MscClient getMscClient() {
        return tenantIdToMscClient.get(TenantContext.getTenantId());
    }

}
