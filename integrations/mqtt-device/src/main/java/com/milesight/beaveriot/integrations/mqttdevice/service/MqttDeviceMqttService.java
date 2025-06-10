package com.milesight.beaveriot.integrations.mqttdevice.service;

import com.milesight.beaveriot.context.api.*;
import com.milesight.beaveriot.context.model.response.DeviceTemplateDiscoverResponse;
import com.milesight.beaveriot.integrations.mqttdevice.support.DataCenter;
import com.milesight.beaveriot.integrations.mqttdevice.support.TopicConverter;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * author: Luxb
 * create: 2025/5/15 17:23
 **/
@Service
public class MqttDeviceMqttService {
    private final MqttPubSubServiceProvider mqttPubSubServiceProvider;
    private final DeviceTemplateParserProvider deviceTemplateParserProvider;
    private final DeviceServiceProvider deviceServiceProvider;
    private final EntityValueServiceProvider entityValueServiceProvider;

    public MqttDeviceMqttService(MqttPubSubServiceProvider mqttPubSubServiceProvider, DeviceTemplateParserProvider deviceTemplateParserProvider, DeviceServiceProvider deviceServiceProvider, EntityValueServiceProvider entityValueServiceProvider) {
        this.mqttPubSubServiceProvider = mqttPubSubServiceProvider;
        this.deviceTemplateParserProvider = deviceTemplateParserProvider;
        this.deviceServiceProvider = deviceServiceProvider;
        this.entityValueServiceProvider = entityValueServiceProvider;
    }

    public void subscribe(String subTopic, String deviceTemplateKey, String deviceTemplateContent) {
        String convertTopic = TopicConverter.convert(subTopic);
        mqttPubSubServiceProvider.subscribe(DataCenter.getUserName(), convertTopic, message -> {
            String jsonStr = new String(message.getPayload(), StandardCharsets.UTF_8);
            DeviceTemplateDiscoverResponse response = deviceTemplateParserProvider.discover(DataCenter.INTEGRATION_ID, jsonStr, deviceTemplateKey, deviceTemplateContent);
            if (response.getDevice() != null) {
                deviceServiceProvider.save(response.getDevice());
                if (response.getPayload() != null) {
                    entityValueServiceProvider.saveValuesAndPublishSync(response.getPayload());
                }
            }
        });
    }

    public void unsubscribe(String subTopic) {
        String convertTopic = TopicConverter.convert(subTopic);
        mqttPubSubServiceProvider.unsubscribe(DataCenter.getUserName(), convertTopic);
    }
}
