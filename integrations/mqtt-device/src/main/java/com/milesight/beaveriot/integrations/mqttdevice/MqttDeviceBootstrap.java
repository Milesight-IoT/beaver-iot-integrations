package com.milesight.beaveriot.integrations.mqttdevice;

import com.milesight.beaveriot.context.integration.bootstrap.IntegrationBootstrap;
import com.milesight.beaveriot.context.integration.model.Integration;
import org.springframework.stereotype.Component;

/**
 * author: Luxb
 * create: 2025/5/14 13:54
 **/
@Component
public class MqttDeviceBootstrap implements IntegrationBootstrap {
    @Override
    public void onPrepared(Integration integrationConfig) {

    }

    @Override
    public void onStarted(Integration integrationConfig) {
        // TODO 订阅所有MQTT设备模板的 Topic
    }

    @Override
    public void onDestroy(Integration integrationConfig) {

    }
}
