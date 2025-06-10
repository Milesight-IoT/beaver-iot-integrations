package com.milesight.beaveriot.integrations.mqttdevice.service;

import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.DeviceBuilder;
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
public class MqttDeviceService {
    private final DeviceServiceProvider deviceServiceProvider;

    public MqttDeviceService(DeviceServiceProvider deviceServiceProvider) {
        this.deviceServiceProvider = deviceServiceProvider;
    }

    @EventSubscribe(payloadKeyExpression = DataCenter.INTEGRATION_ID + ".integration." + MqttDeviceIntegrationEntities.ADD_DEVICE_IDENTIFIER + ".*", eventType = ExchangeEvent.EventType.CALL_SERVICE)
    public void onAddDevice(Event<MqttDeviceIntegrationEntities.AddDevice> event) {
        MqttDeviceIntegrationEntities.AddDevice addDevice = event.getPayload();
        String deviceName = addDevice.getAddDeviceName();
        String deviceTemplateKey = addDevice.getAddDeviceTemplateKey();
        String deviceId = event.getPayload().getDeviceId();
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
}
