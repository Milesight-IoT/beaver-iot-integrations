package com.milesight.beaveriot.integration.lavatory.service;

import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.api.ExchangeFlowExecutor;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.integration.model.event.ExchangeEvent;
import com.milesight.beaveriot.eventbus.annotations.EventSubscribe;
import com.milesight.beaveriot.eventbus.api.Event;
import com.milesight.beaveriot.integration.lavatory.entity.LavatoryConnectionPropertiesEntities;
import com.milesight.beaveriot.integration.lavatory.model.IntegrationStatus;
import com.milesight.msc.sdk.MscClient;
import com.milesight.msc.sdk.config.Credentials;
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;


@Slf4j
@Component
public class LavatoryConnectionService implements com.milesight.beaveriot.integration.lavatory.service.ILavatoryClientProvider {

    private static final String OPENAPI_STATUS_KEY = LavatoryConnectionPropertiesEntities.getKey(LavatoryConnectionPropertiesEntities.Fields.openapiStatus);

    @Autowired
    private EntityValueServiceProvider entityValueServiceProvider;

    @Autowired
    private ExchangeFlowExecutor exchangeFlowExecutor;

    @Getter
    private MscClient mscClient;

    @EventSubscribe(payloadKeyExpression = "lavatory.integration.openapi.*", eventType = ExchangeEvent.EventType.DOWN)
    public void onOpenapiPropertiesUpdate(Event<LavatoryConnectionPropertiesEntities.Openapi> event) {
        if (isConfigChanged(event)) {
            val openapiSettings = event.getPayload();
            initConnection(openapiSettings);
            exchangeFlowExecutor.syncExchangeDown(new ExchangePayload(Map.of(OPENAPI_STATUS_KEY, IntegrationStatus.NOT_READY.name())));
        }
        testConnection();
    }

    private void initConnection(LavatoryConnectionPropertiesEntities.Openapi openapiSettings) {
        mscClient = MscClient.builder()
                .endpoint(openapiSettings.getServerUrl())
                .credentials(Credentials.builder()
                        .clientId(openapiSettings.getClientId())
                        .clientSecret(openapiSettings.getClientSecret())
                        .build())
                .build();
    }

    private void testConnection() {
        try {
            //mscClient.test();
            exchangeFlowExecutor.syncExchangeDown(new ExchangePayload(Map.of(OPENAPI_STATUS_KEY, IntegrationStatus.READY.name())));
        } catch (Exception e) {
            log.error("Error occurs while testing connection", e);
            exchangeFlowExecutor.syncExchangeDown(new ExchangePayload(Map.of(OPENAPI_STATUS_KEY, IntegrationStatus.ERROR.name())));
        }
    }

    private boolean isConfigChanged(Event<LavatoryConnectionPropertiesEntities.Openapi> event) {
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

    public void init() {
        try {
            val settings = entityValueServiceProvider.findValuesByKey(
                    LavatoryConnectionPropertiesEntities.getKey(LavatoryConnectionPropertiesEntities.Fields.openapi), LavatoryConnectionPropertiesEntities.Openapi.class);
            if (!settings.isEmpty()) {
                initConnection(settings);
                testConnection();
            }
        } catch (Exception e) {
            log.error("Error occurs while initializing connection", e);
            exchangeFlowExecutor.syncExchangeDown(new ExchangePayload(Map.of(OPENAPI_STATUS_KEY, IntegrationStatus.NOT_READY.name())));
        }
    }

}
