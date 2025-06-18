package com.milesight.beaveriot.integrations.mqttdevice.service;

import com.google.common.collect.Maps;
import com.milesight.beaveriot.base.annotations.shedlock.DistributedLock;
import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.api.DeviceTemplateParserProvider;
import com.milesight.beaveriot.context.api.DeviceTemplateServiceProvider;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.constants.IntegrationConstants;
import com.milesight.beaveriot.context.integration.model.*;
import com.milesight.beaveriot.context.integration.model.event.ExchangeEvent;
import com.milesight.beaveriot.eventbus.annotations.EventSubscribe;
import com.milesight.beaveriot.eventbus.api.Event;
import com.milesight.beaveriot.integrations.mqttdevice.constants.LockConstants;
import com.milesight.beaveriot.integrations.mqttdevice.entity.MqttDeviceIntegrationEntities;
import com.milesight.beaveriot.integrations.mqttdevice.entity.MqttDeviceServiceEntities;
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
    private final DeviceTemplateParserProvider deviceTemplateParserProvider;
    private final EntityServiceProvider entityServiceProvider;

    public MqttDeviceService(DeviceServiceProvider deviceServiceProvider, DeviceTemplateServiceProvider deviceTemplateServiceProvider, DeviceTemplateParserProvider deviceTemplateParserProvider, EntityServiceProvider entityServiceProvider) {
        this.deviceServiceProvider = deviceServiceProvider;
        this.deviceTemplateServiceProvider = deviceTemplateServiceProvider;
        this.deviceTemplateParserProvider = deviceTemplateParserProvider;
        this.entityServiceProvider = entityServiceProvider;
    }

    @EventSubscribe(payloadKeyExpression = DataCenter.INTEGRATION_ID + ".integration." + MqttDeviceIntegrationEntities.ADD_DEVICE_IDENTIFIER + ".*", eventType = ExchangeEvent.EventType.CALL_SERVICE)
    public void onAddDevice(Event<MqttDeviceIntegrationEntities.AddDevice> event) {
        MqttDeviceIntegrationEntities.AddDevice addDevice = event.getPayload();
        String deviceName = addDevice.getAddDeviceName();
        String deviceTemplateKey = addDevice.getTemplate();
        String deviceId = addDevice.getDeviceId();

        DeviceTemplate deviceTemplate = deviceTemplateServiceProvider.findByKey(deviceTemplateKey);
        Device device = deviceTemplateParserProvider.createDevice(DataCenter.INTEGRATION_ID, deviceTemplate.getId(), deviceId, deviceName);
        deviceServiceProvider.save(device);
    }

    @EventSubscribe(payloadKeyExpression = DataCenter.INTEGRATION_ID + ".integration." + MqttDeviceIntegrationEntities.DELETE_DEVICE_IDENTIFIER, eventType = ExchangeEvent.EventType.CALL_SERVICE)
    public void onDeleteDevice(Event<MqttDeviceIntegrationEntities.DeleteDevice> event) {
        Device device = event.getPayload().getDeletedDevice();
        deviceServiceProvider.deleteById(device.getId());
    }

    private Entity getAddDeviceTemplateEntity() {
        Entity deviceTemplateEntity = entityServiceProvider.findByKey(MqttDeviceIntegrationEntities.ADD_DEVICE_TEMPLATE_KEY);
        initAttributes(deviceTemplateEntity);

        return deviceTemplateEntity;
    }

    private Entity getDataInputTemplateEntity() {
        Entity deviceTemplateEntity = entityServiceProvider.findByKey(MqttDeviceServiceEntities.DATA_INPUT_TEMPLATE_KEY);
        initAttributes(deviceTemplateEntity);

        return deviceTemplateEntity;
    }

    private void initAttributes(Entity entity) {
        Map<String, Object> attributes = entity.getAttributes();
        if (attributes == null) {
            attributes = new HashMap<>();
            entity.setAttributes(attributes);
            attributes.put(AttributeBuilder.ATTRIBUTE_ENUM, new HashMap<>());
        }
    }

    @DistributedLock(name = LockConstants.SYNC_DEVICE_TEMPLATE_LOCK)
    public void syncTemplates() {
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

        Entity dataInputTemplateEntity = getDataInputTemplateEntity();
        dataInputTemplateEntity.getAttributes().put(AttributeBuilder.ATTRIBUTE_ENUM, templates);
        entityServiceProvider.save(dataInputTemplateEntity);
    }
}
