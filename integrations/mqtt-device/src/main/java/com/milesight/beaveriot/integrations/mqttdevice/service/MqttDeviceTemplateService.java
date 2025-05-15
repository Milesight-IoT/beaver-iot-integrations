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
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * author: Luxb
 * create: 2025/5/14 14:25
 **/
@Service
public class MqttDeviceTemplateService {
    private final DeviceTemplateServiceProvider deviceTemplateServiceProvider;
    public static final String INTEGRATION_ID = "mqtt-device";

    public MqttDeviceTemplateService(DeviceTemplateServiceProvider deviceTemplateServiceProvider) {
        this.deviceTemplateServiceProvider = deviceTemplateServiceProvider;
    }

    @EventSubscribe(payloadKeyExpression = INTEGRATION_ID + ".integration." + MqttDeviceIntegrationEntities.ADD_DEVICE_TEMPLATE_IDENTIFIER + ".*", eventType = ExchangeEvent.EventType.CALL_SERVICE)
    public void onAddDeviceTemplate(Event<MqttDeviceIntegrationEntities.AddDeviceTemplate> event) {
        MqttDeviceIntegrationEntities.AddDeviceTemplate addDeviceTemplate = event.getPayload();
        String deviceTemplateName = addDeviceTemplate.getAddDeviceTemplateName();
        String deviceTemplateContent = addDeviceTemplate.getAddDeviceTemplateContent();
        String deviceTemplateDescription = addDeviceTemplate.getAddDeviceTemplateDescription();
        String topic = event.getPayload().getTopic();
        DeviceTemplate deviceTemplate = new DeviceTemplateBuilder(INTEGRATION_ID)
                .name(deviceTemplateName)
                .content(deviceTemplateContent)
                .description(deviceTemplateDescription)
                .identifier(deviceTemplateName)
                .additional(Map.of("topic", topic))
                .build();

        deviceTemplateServiceProvider.save(deviceTemplate);
    }

    @EventSubscribe(payloadKeyExpression = INTEGRATION_ID + ".integration." + MqttDeviceIntegrationEntities.UPDATE_DEVICE_TEMPLATE_IDENTIFIER + ".*", eventType = ExchangeEvent.EventType.CALL_SERVICE)
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
        deviceTemplate.setContent(deviceTemplateContent);
        deviceTemplate.setDescription(deviceTemplateDescription);
        deviceTemplate.setAdditional(Map.of("topic", topic));

        deviceTemplateServiceProvider.save(deviceTemplate);
    }

    @EventSubscribe(payloadKeyExpression = INTEGRATION_ID + ".integration." + MqttDeviceIntegrationEntities.DELETE_DEVICE_TEMPLATE_IDENTIFIER, eventType = ExchangeEvent.EventType.CALL_SERVICE)
    public void onDeleteDeviceTemplate(Event<MqttDeviceIntegrationEntities.DeleteDeviceTemplate> event) {
        DeviceTemplate deviceTemplate = event.getPayload().getDeletedDeviceTemplate();
        deviceTemplateServiceProvider.deleteById(deviceTemplate.getId());
    }
}
