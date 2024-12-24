package com.milesight.beaveriot.integration.aws;

import com.milesight.beaveriot.context.integration.bootstrap.IntegrationBootstrap;
import com.milesight.beaveriot.context.integration.bootstrap.IntegrationBootstrapManager;
import com.milesight.beaveriot.context.integration.model.Integration;
import com.milesight.beaveriot.integration.aws.sdk.config.DestinationProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AwsIntegrationBootstrap implements IntegrationBootstrap {

    @Autowired
    private IntegrationBootstrapManager integrationBootstrapManager;

    @Override
    public void onPrepared(Integration integrationConfig) {

    }

    @Override
    public void onStarted(Integration integrationConfig) {
//        Binder.get(integrationBootstrapManager.getIntegrationContext().getIntegrationEnvironment(AwsIntegrationConstants.INTEGRATION_IDENTIFIER)).bind("sdk.aws", AwsIotProperties.class);
//        Binder.get(integrationBootstrapManager.getIntegrationContext().getIntegrationEnvironment(AwsIntegrationConstants.INTEGRATION_IDENTIFIER)).bind("sdk.aws.config.destination", DestinationProperties.class);
//        Binder.get(integrationBootstrapManager.getIntegrationContext().getIntegrationEnvironment(AwsIntegrationConstants.INTEGRATION_IDENTIFIER)).bind("sdk.aws.config.device-profile", DeviceProfileProperties.class);
//        Binder.get(integrationBootstrapManager.getIntegrationContext().getIntegrationEnvironment(AwsIntegrationConstants.INTEGRATION_IDENTIFIER)).bind("sdk.aws.config.service-profile", ServiceProfileProperties.class);
        log.info("AWS integration starting");
    }

    @Override
    public void onDestroy(Integration integrationConfig) {
        log.info("AWS integration stopping");
    }

    @Override
    public void customizeRoute(CamelContext context) throws Exception {
        IntegrationBootstrap.super.customizeRoute(context);
    }
}
