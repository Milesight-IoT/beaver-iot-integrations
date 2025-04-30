package com.milesight.beaveriot.integration.msc;

import com.milesight.beaveriot.context.integration.bootstrap.IntegrationBootstrap;
import com.milesight.beaveriot.context.integration.model.Integration;
import com.milesight.beaveriot.integration.msc.service.MscConnectionService;
import com.milesight.beaveriot.integration.msc.service.MscDataSyncService;
import com.milesight.beaveriot.integration.msc.service.MscWebhookService;
import lombok.extern.slf4j.*;
import org.apache.camel.CamelContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MscIntegrationBootstrap implements IntegrationBootstrap {

    @Autowired
    private MscConnectionService mscConnectionService;

    @Autowired
    private MscDataSyncService mscDataFetchingService;

    @Autowired
    private MscWebhookService mscWebhookService;


    @Override
    public void onPrepared(Integration integrationConfig) {
        // do nothing
    }

    @Override
    public void onStarted(Integration integrationConfig) {
        // do nothing
    }

    @Override
    public void onEnabled(String tenantId, Integration integrationConfig) {
        log.info("MSC integration starting");
        mscConnectionService.init(tenantId);
        mscWebhookService.init(tenantId);
        log.info("MSC integration started");
    }

    @Override
    public void onDisabled(String tenantId, Integration integrationConfig) {
        log.info("MSC integration starting");
        mscConnectionService.disable(tenantId);
        mscDataFetchingService.disable(tenantId);
        mscWebhookService.disable(tenantId);
        log.info("MSC integration started");
    }

    @Override
    public void onDestroy(Integration integrationConfig) {
        log.info("MSC integration stopping");
        mscConnectionService.stop();
        mscDataFetchingService.stop();
        mscWebhookService.stop();
        log.info("MSC integration stopped");
    }

    @Override
    public void customizeRoute(CamelContext context) throws Exception {
        IntegrationBootstrap.super.customizeRoute(context);
    }
}
