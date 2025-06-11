package com.milesight.beaveriot.integrations.mqttdevice.service;

import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.api.DeviceTemplateParserProvider;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.api.MqttPubSubServiceProvider;
import com.milesight.beaveriot.context.model.response.DeviceTemplateInputResult;
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

    public void subscribe(String subTopic, Long deviceTemplateId) {
        String convertTopic = TopicConverter.convert(subTopic);
        mqttPubSubServiceProvider.subscribe(DataCenter.getUserName(), convertTopic, message -> {
            String jsonData = new String(message.getPayload(), StandardCharsets.UTF_8);
            DeviceTemplateInputResult result = deviceTemplateParserProvider.input(DataCenter.INTEGRATION_ID, deviceTemplateId, jsonData);
            if (result.getDevice() != null) {
                deviceServiceProvider.save(result.getDevice());
                if (result.getPayload() != null) {
                    entityValueServiceProvider.saveValuesAndPublishSync(result.getPayload());
                }
            }
        });
    }

    public void unsubscribe(String subTopic) {
        String convertTopic = TopicConverter.convert(subTopic);
        mqttPubSubServiceProvider.unsubscribe(DataCenter.getUserName(), convertTopic);
    }
}
