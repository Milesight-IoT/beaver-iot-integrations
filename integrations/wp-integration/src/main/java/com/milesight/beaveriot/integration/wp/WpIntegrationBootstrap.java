package com.milesight.beaveriot.integration.wp;

import com.milesight.beaveriot.context.integration.bootstrap.IntegrationBootstrap;
import com.milesight.beaveriot.context.integration.model.Integration;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WpIntegrationBootstrap implements IntegrationBootstrap {


    @Override
    public void onPrepared(Integration integrationConfig) {

    }

    @Override
    public void onStarted(Integration integrationConfig) {
        log.info("WP integration started");
    }

    @Override
    public void onDestroy(Integration integrationConfig) {
        log.info("WP integration stopping");
    }

    @Override
    public void customizeRoute(CamelContext context) throws Exception {
        IntegrationBootstrap.super.customizeRoute(context);
    }
}
