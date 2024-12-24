package com.milesight.beaveriot.integration.lavatory;

import com.milesight.beaveriot.context.integration.bootstrap.IntegrationBootstrap;
import com.milesight.beaveriot.context.integration.model.Integration;
import com.milesight.beaveriot.integration.lavatory.service.LavatoryConnectionService;
import com.milesight.beaveriot.integration.lavatory.service.LavatoryDataSyncService;
import com.milesight.beaveriot.integration.lavatory.service.LavatoryWebhookService;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LavatoryIntegrationBootstrap implements IntegrationBootstrap {

    @Autowired
    private LavatoryConnectionService lavatoryConnectionService;

    @Autowired
    private LavatoryDataSyncService lavatoryDataFetchingService;

    @Autowired
    private LavatoryWebhookService lavatoryWebhookService;


    @Override
    public void onPrepared(Integration integrationConfig) {

    }

    @Override
    public void onStarted(Integration integrationConfig) {
        log.info("Lavatory integration starting");
        lavatoryConnectionService.init();
        lavatoryDataFetchingService.init();
        lavatoryWebhookService.init();
        log.info("Lavatory integration started");
    }

    @Override
    public void onDestroy(Integration integrationConfig) {
        log.info("Lavatory integration stopping");
        lavatoryDataFetchingService.stop();
        log.info("Lavatory integration stopped");
    }

    @Override
    public void customizeRoute(CamelContext context) throws Exception {
        IntegrationBootstrap.super.customizeRoute(context);
    }
}
