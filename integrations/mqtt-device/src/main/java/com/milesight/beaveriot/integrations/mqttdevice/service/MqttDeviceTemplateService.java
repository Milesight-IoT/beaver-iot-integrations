package com.milesight.beaveriot.integrations.mqttdevice.service;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.context.api.DeviceTemplateServiceProvider;
import com.milesight.beaveriot.context.integration.model.DeviceTemplate;
import com.milesight.beaveriot.context.integration.model.DeviceTemplateBuilder;
import com.milesight.beaveriot.context.integration.model.event.ExchangeEvent;
import com.milesight.beaveriot.eventbus.annotations.EventSubscribe;
import com.milesight.beaveriot.eventbus.api.Event;
import com.milesight.beaveriot.integrations.mqttdevice.entity.MqttDeviceIntegrationEntities;
import com.milesight.beaveriot.integrations.mqttdevice.support.DataCenter;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * author: Luxb
 * create: 2025/5/14 14:25
 **/
@Service
public class MqttDeviceTemplateService {
    private final DeviceTemplateServiceProvider deviceTemplateServiceProvider;
    private final MqttDeviceMqttService mqttDeviceMqttService;

    public MqttDeviceTemplateService(DeviceTemplateServiceProvider deviceTemplateServiceProvider, MqttDeviceMqttService mqttDeviceMqttService) {
        this.deviceTemplateServiceProvider = deviceTemplateServiceProvider;
        this.mqttDeviceMqttService = mqttDeviceMqttService;
    }

    @EventSubscribe(payloadKeyExpression = DataCenter.INTEGRATION_ID + ".integration." + MqttDeviceIntegrationEntities.ADD_DEVICE_TEMPLATE_IDENTIFIER + ".*", eventType = ExchangeEvent.EventType.CALL_SERVICE)
    public void onAddDeviceTemplate(Event<MqttDeviceIntegrationEntities.AddDeviceTemplate> event) {
        MqttDeviceIntegrationEntities.AddDeviceTemplate addDeviceTemplate = event.getPayload();
        String deviceTemplateName = addDeviceTemplate.getAddDeviceTemplateName();
        String deviceTemplateContent = addDeviceTemplate.getAddDeviceTemplateContent();
        String deviceTemplateDescription = addDeviceTemplate.getAddDeviceTemplateDescription();
        String topic = event.getPayload().getTopic();
        DeviceTemplate deviceTemplate = new DeviceTemplateBuilder(DataCenter.INTEGRATION_ID)
                .name(deviceTemplateName)
                .content(deviceTemplateContent)
                .description(deviceTemplateDescription)
                .identifier(deviceTemplateName)
                .additional(Map.of(DataCenter.TOPIC_KEY, topic))
                .build();

        if (DataCenter.getTemplateIdTopicMap().containsValue(topic)) {
            throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), "topic exists").build();
        }
        deviceTemplateServiceProvider.save(deviceTemplate);
        DataCenter.getTemplateIdTopicMap().put(deviceTemplate.getId(), topic);
        mqttDeviceMqttService.subscribe(topic, deviceTemplate.getId(), deviceTemplateContent);
    }

    @EventSubscribe(payloadKeyExpression = DataCenter.INTEGRATION_ID + ".integration." + MqttDeviceIntegrationEntities.UPDATE_DEVICE_TEMPLATE_IDENTIFIER + ".*", eventType = ExchangeEvent.EventType.CALL_SERVICE)
    public void onUpdateDeviceTemplate(Event<MqttDeviceIntegrationEntities.UpdateDeviceTemplate> event) {
        MqttDeviceIntegrationEntities.UpdateDeviceTemplate updateDeviceTemplate = event.getPayload();
        Long deviceTemplateId = updateDeviceTemplate.getUpdateDeviceTemplateId();
        String deviceTemplateName = updateDeviceTemplate.getUpdateDeviceTemplateName();
        String deviceTemplateContent = updateDeviceTemplate.getUpdateDeviceTemplateContent();
        String deviceTemplateDescription = updateDeviceTemplate.getUpdateDeviceTemplateDescription();
        String topic = event.getPayload().getTopic();

        DeviceTemplate deviceTemplate = deviceTemplateServiceProvider.findById(deviceTemplateId);
        if (deviceTemplate == null) {
            throw ServiceException.with(ErrorCode.DATA_NO_FOUND).build();
        }

        deviceTemplate.setName(deviceTemplateName);
        String oldDeviceTemplateContent = deviceTemplate.getContent();
        deviceTemplate.setContent(deviceTemplateContent);
        deviceTemplate.setDescription(deviceTemplateDescription);
        String oldTopic = deviceTemplate.getAdditional().get(DataCenter.TOPIC_KEY).toString();
        deviceTemplate.setAdditional(Map.of(DataCenter.TOPIC_KEY, topic));

        DataCenter.getTemplateIdTopicMap().put(deviceTemplate.getId(), topic);

        deviceTemplateServiceProvider.save(deviceTemplate);
        if (oldTopic.equals(topic)) {
            if (!oldDeviceTemplateContent.equals(deviceTemplateContent)) {
                mqttDeviceMqttService.unsubscribe(oldTopic);
                mqttDeviceMqttService.subscribe(topic, deviceTemplate.getId(), deviceTemplateContent);
            }
        } else {
            mqttDeviceMqttService.unsubscribe(oldTopic);
            mqttDeviceMqttService.subscribe(topic, deviceTemplate.getId(), deviceTemplateContent);
        }
    }

    @EventSubscribe(payloadKeyExpression = DataCenter.INTEGRATION_ID + ".integration." + MqttDeviceIntegrationEntities.DELETE_DEVICE_TEMPLATE_IDENTIFIER, eventType = ExchangeEvent.EventType.CALL_SERVICE)
    public void onDeleteDeviceTemplate(Event<MqttDeviceIntegrationEntities.DeleteDeviceTemplate> event) {
        DeviceTemplate deviceTemplate = event.getPayload().getDeletedDeviceTemplate();
        deviceTemplateServiceProvider.deleteById(deviceTemplate.getId());

        DataCenter.getTemplateIdTopicMap().remove(deviceTemplate.getId());
        mqttDeviceMqttService.unsubscribe(deviceTemplate.getAdditional().get(DataCenter.TOPIC_KEY).toString());
    }
}
