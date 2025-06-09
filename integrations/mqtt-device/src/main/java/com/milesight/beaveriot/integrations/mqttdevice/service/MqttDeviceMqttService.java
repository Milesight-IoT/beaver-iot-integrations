package com.milesight.beaveriot.integrations.mqttdevice.service;

import com.milesight.beaveriot.context.api.DeviceTemplateParserProvider;
import com.milesight.beaveriot.context.api.MqttPubSubServiceProvider;
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

    public MqttDeviceMqttService(MqttPubSubServiceProvider mqttPubSubServiceProvider, DeviceTemplateParserProvider deviceTemplateParserProvider) {
        this.mqttPubSubServiceProvider = mqttPubSubServiceProvider;
        this.deviceTemplateParserProvider = deviceTemplateParserProvider;
    }

    public void subscribe(String subTopic, Long deviceTemplateId, String deviceTemplateContent) {
        String convertTopic = TopicConverter.convert(subTopic);
        mqttPubSubServiceProvider.subscribe(DataCenter.getUserName(), convertTopic, message -> {
            String jsonStr = new String(message.getPayload(), StandardCharsets.UTF_8);
            deviceTemplateParserProvider.discover(DataCenter.INTEGRATION_ID, jsonStr, deviceTemplateId, deviceTemplateContent);
        });
    }

    public void unsubscribe(String subTopic) {
        String convertTopic = TopicConverter.convert(subTopic);
        mqttPubSubServiceProvider.unsubscribe(DataCenter.getUserName(), convertTopic);
    }
}
