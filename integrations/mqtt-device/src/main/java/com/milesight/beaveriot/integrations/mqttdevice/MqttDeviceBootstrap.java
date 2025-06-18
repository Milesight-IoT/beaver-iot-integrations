package com.milesight.beaveriot.integrations.mqttdevice;

import com.milesight.beaveriot.context.integration.bootstrap.IntegrationBootstrap;
import com.milesight.beaveriot.context.integration.model.Integration;
import com.milesight.beaveriot.integrations.mqttdevice.service.MqttDeviceMqttService;
import com.milesight.beaveriot.integrations.mqttdevice.service.MqttDeviceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * author: Luxb
 * create: 2025/5/14 13:54
 **/
@Slf4j
@Component
public class MqttDeviceBootstrap implements IntegrationBootstrap {
    private final MqttDeviceMqttService mqttDeviceMqttService;
    private final MqttDeviceService mqttDeviceService;

    public MqttDeviceBootstrap(MqttDeviceMqttService mqttDeviceMqttService, MqttDeviceService mqttDeviceService) {
        this.mqttDeviceMqttService = mqttDeviceMqttService;
        this.mqttDeviceService = mqttDeviceService;
    }

    @Override
    public void onPrepared(Integration integrationConfig) {
        // do nothing
    }

    @Override
    public void onStarted(Integration integrationConfig) {
        log.info("Mqtt device integration starting");
        subscribeTopic();
        log.info("Mqtt device integration started");
    }

    @Override
    public void onDestroy(Integration integrationConfig) {
        // do nothing
    }

    private void subscribeTopic() {
        mqttDeviceMqttService.subscribe();
    }

    @Override
    public void onEnabled(String tenantId, Integration integrationConfig) {
        mqttDeviceService.syncTemplates();
        IntegrationBootstrap.super.onEnabled(tenantId, integrationConfig);
    }
}
