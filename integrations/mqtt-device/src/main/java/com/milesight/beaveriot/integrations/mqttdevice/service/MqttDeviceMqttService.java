package com.milesight.beaveriot.integrations.mqttdevice.service;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.api.DeviceTemplateParserProvider;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.api.MqttPubSubServiceProvider;
import com.milesight.beaveriot.context.model.response.DeviceTemplateInputResult;
import com.milesight.beaveriot.integrations.mqttdevice.support.DataCenter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;

/**
 * author: Luxb
 * create: 2025/5/15 17:23
 **/
@Slf4j
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

    public void subscribe() {
        mqttPubSubServiceProvider.subscribe(DataCenter.INTEGRATION_ID + "/#", message -> {
            try {
                String topic = message.getTopicSubPath().substring(DataCenter.INTEGRATION_ID.length());
                Long deviceTemplateId = DataCenter.getTemplateIdByTopic(topic);
                if (deviceTemplateId == null) {
                    throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), MessageFormat.format("No device template related to the sub topic ''{0}''", topic)).build();
                }
                String jsonData = new String(message.getPayload(), StandardCharsets.UTF_8);
                DeviceTemplateInputResult result = deviceTemplateParserProvider.input(DataCenter.INTEGRATION_ID, deviceTemplateId, jsonData);
                if (result.getDevice() != null) {
                    deviceServiceProvider.save(result.getDevice());
                    if (result.getPayload() != null) {
                        entityValueServiceProvider.saveValuesAndPublishSync(result.getPayload());
                    }
                }
            } catch (Exception e) {
                log.error("MqttDeviceMqttService.subscribe error: {}", e.getMessage());
            }
        });
    }
}
