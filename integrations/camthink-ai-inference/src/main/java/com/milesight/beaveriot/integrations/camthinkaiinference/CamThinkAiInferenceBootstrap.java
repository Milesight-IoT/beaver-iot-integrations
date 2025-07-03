package com.milesight.beaveriot.integrations.camthinkaiinference;

import com.milesight.beaveriot.context.integration.bootstrap.IntegrationBootstrap;
import com.milesight.beaveriot.context.integration.model.Integration;
import com.milesight.beaveriot.integrations.camthinkaiinference.service.CamThinkAiInferenceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * author: Luxb
 * create: 2025/5/30 15:57
 **/
@Component
@Slf4j
public class CamThinkAiInferenceBootstrap implements IntegrationBootstrap {
    private final CamThinkAiInferenceService camThinkAiInferenceService;

    public CamThinkAiInferenceBootstrap(CamThinkAiInferenceService camThinkAiInferenceService) {
        this.camThinkAiInferenceService = camThinkAiInferenceService;
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
        log.info("CamThink Ai inference integration starting");
        camThinkAiInferenceService.init();
        log.info("CamThink Ai inference integration started");
        IntegrationBootstrap.super.onEnabled(tenantId, integrationConfig);
    }

    @Override
    public void onDestroy(Integration integrationConfig) {
        log.info("CamThink Ai inference integration destroying");
        camThinkAiInferenceService.destroy();
        log.info("CamThink Ai inference integration destroyed");
    }
}
