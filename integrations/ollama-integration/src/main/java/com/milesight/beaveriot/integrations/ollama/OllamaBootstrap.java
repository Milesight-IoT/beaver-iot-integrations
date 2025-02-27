package com.milesight.beaveriot.integrations.ollama;

import com.milesight.beaveriot.context.integration.bootstrap.IntegrationBootstrap;
import com.milesight.beaveriot.context.integration.model.Integration;
import com.milesight.beaveriot.integrations.ollama.service.OllamaApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OllamaBootstrap implements IntegrationBootstrap {
    @Autowired
    private OllamaApiService ollamaApiService;

    @Override
    public void onPrepared(Integration integration) {
        // do nothing
    }

    @Override
    public void onStarted(Integration integrationConfig) {
        log.info("Ollama integration starting");
        ollamaApiService.init();
        log.info("Ollama integration started");
    }

    @Override
    public void onDestroy(Integration integration) {
        // do nothing
    }
}
