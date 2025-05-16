package com.milesight.beaveriot.integrations.mqttdevice;

import com.milesight.beaveriot.context.api.DeviceTemplateServiceProvider;
import com.milesight.beaveriot.context.integration.bootstrap.IntegrationBootstrap;
import com.milesight.beaveriot.context.integration.model.DeviceTemplate;
import com.milesight.beaveriot.context.integration.model.Integration;
import com.milesight.beaveriot.integrations.mqttdevice.service.MqttDeviceMqttService;
import com.milesight.beaveriot.integrations.mqttdevice.support.DataCenter;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * author: Luxb
 * create: 2025/5/14 13:54
 **/
@Component
public class MqttDeviceBootstrap implements IntegrationBootstrap {
    private final DeviceTemplateServiceProvider deviceTemplateServiceProvider;
    private final MqttDeviceMqttService mqttDeviceMqttService;

    public MqttDeviceBootstrap(DeviceTemplateServiceProvider deviceTemplateServiceProvider, MqttDeviceMqttService mqttDeviceMqttService) {
        this.deviceTemplateServiceProvider = deviceTemplateServiceProvider;
        this.mqttDeviceMqttService = mqttDeviceMqttService;
    }

    @Override
    public void onPrepared(Integration integrationConfig) {

    }

    @Override
    public void onStarted(Integration integrationConfig) {
        List<DeviceTemplate> list = deviceTemplateServiceProvider.findAll(DataCenter.INTEGRATION_ID);
        if (!CollectionUtils.isEmpty(list)) {
            list.forEach(deviceTemplate -> {
                String topic = deviceTemplate.getAdditional().get(DataCenter.TOPIC_KEY).toString();
                DataCenter.getTemplateIdTopicMap().put(deviceTemplate.getId(), topic);
                mqttDeviceMqttService.subscribe(topic, deviceTemplate.getId(), deviceTemplate.getContent());
            });
        }
    }

    @Override
    public void onDestroy(Integration integrationConfig) {

    }
}
