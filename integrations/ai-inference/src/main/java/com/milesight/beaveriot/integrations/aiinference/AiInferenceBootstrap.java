package com.milesight.beaveriot.integrations.aiinference;

import com.milesight.beaveriot.context.integration.bootstrap.IntegrationBootstrap;
import com.milesight.beaveriot.context.integration.model.Integration;
import com.milesight.beaveriot.integrations.aiinference.service.AiInferenceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * author: Luxb
 * create: 2025/5/30 15:57
 **/
@Component
@Slf4j
public class AiInferenceBootstrap implements IntegrationBootstrap {
    private final AiInferenceService aiInferenceService;

    public AiInferenceBootstrap(AiInferenceService aiInferenceService) {
        this.aiInferenceService = aiInferenceService;
    }

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
        log.info("Ai inference integration starting");
        aiInferenceService.init();
        log.info("Ai inference integration started");
        IntegrationBootstrap.super.onEnabled(tenantId, integrationConfig);
    }

    @Override
    public void onDestroy(Integration integrationConfig) {
        log.info("Ai inference integration destroying");
        aiInferenceService.destroy();
        log.info("Ai inference integration destroyed");
    }
}
