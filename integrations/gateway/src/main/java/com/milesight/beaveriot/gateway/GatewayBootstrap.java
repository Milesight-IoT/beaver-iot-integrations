package com.milesight.beaveriot.gateway;

import com.milesight.beaveriot.context.integration.bootstrap.IntegrationBootstrap;
import com.milesight.beaveriot.context.integration.model.Integration;
import com.milesight.beaveriot.gateway.handle.MqttDataHandle;
import com.milesight.beaveriot.gateway.service.DeviceDataSyncService;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GatewayBootstrap implements IntegrationBootstrap {

    @Autowired
    private DeviceDataSyncService deviceDataSyncService;
    @Autowired
    private MqttDataHandle mqttDataHandle;
    @Override
    public void onPrepared(Integration integration) {
        // do nothing
    }

    @Override
    public void onStarted(Integration integrationConfig) {
        // do nothing
        log.info("gateway integration starting");
        deviceDataSyncService.init();
        // 拉取所有payloadCodecContent
        try {
            mqttDataHandle.getAllPayloadCodecContent();
        } catch (Exception e) {
            log.error("getAllPayloadCodecContent error", e);
        }
        log.info("gateway integration started");
    }

    @Override
    public void onDestroy(Integration integration) {
        // do nothing
        log.info("gateway integration stopping");
        deviceDataSyncService.stop();
        log.info("gateway integration stopped");
    }

    @Override
    public void customizeRoute(CamelContext context) throws Exception {
        IntegrationBootstrap.super.customizeRoute(context);
    }
}
