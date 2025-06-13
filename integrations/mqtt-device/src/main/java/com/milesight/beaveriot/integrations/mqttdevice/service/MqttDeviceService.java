package com.milesight.beaveriot.integrations.mqttdevice.service;

import com.google.common.collect.Maps;
import com.milesight.beaveriot.base.annotations.shedlock.DistributedLock;
import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.api.DeviceTemplateServiceProvider;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.constants.IntegrationConstants;
import com.milesight.beaveriot.context.integration.model.*;
import com.milesight.beaveriot.context.integration.model.event.ExchangeEvent;
import com.milesight.beaveriot.eventbus.annotations.EventSubscribe;
import com.milesight.beaveriot.eventbus.api.Event;
import com.milesight.beaveriot.integrations.mqttdevice.constants.LockConstants;
import com.milesight.beaveriot.integrations.mqttdevice.entity.MqttDeviceIntegrationEntities;
import com.milesight.beaveriot.integrations.mqttdevice.support.DataCenter;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

/**
 * author: Luxb
 * create: 2025/5/14 14:25
 **/
@Service
public class MqttDeviceService {
    private final DeviceServiceProvider deviceServiceProvider;
    private final DeviceTemplateServiceProvider deviceTemplateServiceProvider;
    private final EntityServiceProvider entityServiceProvider;

    public MqttDeviceService(DeviceServiceProvider deviceServiceProvider, DeviceTemplateServiceProvider deviceTemplateServiceProvider, EntityServiceProvider entityServiceProvider) {
        this.deviceServiceProvider = deviceServiceProvider;
        this.deviceTemplateServiceProvider = deviceTemplateServiceProvider;
        this.entityServiceProvider = entityServiceProvider;
    }

    @EventSubscribe(payloadKeyExpression = DataCenter.INTEGRATION_ID + ".integration." + MqttDeviceIntegrationEntities.ADD_DEVICE_IDENTIFIER + ".*", eventType = ExchangeEvent.EventType.CALL_SERVICE)
    public void onAddDevice(Event<MqttDeviceIntegrationEntities.AddDevice> event) {
        MqttDeviceIntegrationEntities.AddDevice addDevice = event.getPayload();
        String deviceName = addDevice.getAddDeviceName();
        String deviceTemplateKey = addDevice.getTemplate();
        String deviceId = addDevice.getDeviceId();
        Device device = new DeviceBuilder(DataCenter.INTEGRATION_ID)
                .name(deviceName)
                .template(deviceTemplateKey)
                .identifier(deviceId)
                .additional(Map.of("deviceId", deviceId))
                .build();

        deviceServiceProvider.save(device);
    }

    @EventSubscribe(payloadKeyExpression = DataCenter.INTEGRATION_ID + ".integration." + MqttDeviceIntegrationEntities.DELETE_DEVICE_IDENTIFIER, eventType = ExchangeEvent.EventType.CALL_SERVICE)
    public void onDeleteDevice(Event<MqttDeviceIntegrationEntities.DeleteDevice> event) {
        Device device = event.getPayload().getDeletedDevice();
        deviceServiceProvider.deleteById(device.getId());
    }

    private Entity getAddDeviceTemplateEntity() {
        Entity deviceTemplateEntity = entityServiceProvider.findByKey(MqttDeviceIntegrationEntities.ADD_DEVICE_TEMPLATE_KEY);
        Map<String, Object> attributes = deviceTemplateEntity.getAttributes();
        if (attributes == null) {
            attributes = new HashMap<>();
            deviceTemplateEntity.setAttributes(attributes);
            attributes.put(AttributeBuilder.ATTRIBUTE_ENUM, new HashMap<>());
        }

        return deviceTemplateEntity;
    }

    @DistributedLock(name = LockConstants.SYNC_DEVICE_TEMPLATE_LOCK)
    public void syncAddDeviceTemplates() {
        List<DeviceTemplate> deviceTemplates = deviceTemplateServiceProvider.findAll(IntegrationConstants.SYSTEM_INTEGRATION_ID);
        Map<String, String> templates;
        if (CollectionUtils.isEmpty(deviceTemplates)) {
            templates = Maps.newLinkedHashMap();
        } else {
            templates = deviceTemplates.stream()
                    .collect(Collectors.toMap(
                            DeviceTemplate::getKey,
                            DeviceTemplate::getName,
                            (existing, replacement) -> existing,
                            LinkedHashMap::new
                    ));
        }
        Entity addDeviceTemplateEntity = getAddDeviceTemplateEntity();
        addDeviceTemplateEntity.getAttributes().put(AttributeBuilder.ATTRIBUTE_ENUM, templates);
        entityServiceProvider.save(addDeviceTemplateEntity);
    }
}
