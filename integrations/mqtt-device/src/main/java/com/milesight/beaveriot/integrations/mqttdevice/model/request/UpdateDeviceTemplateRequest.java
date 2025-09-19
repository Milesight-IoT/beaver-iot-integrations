package com.milesight.beaveriot.integrations.mqttdevice.model.request;

import com.milesight.beaveriot.integrations.mqttdevice.constants.MqttDeviceConstants;
import lombok.Data;

@Data
public class UpdateDeviceTemplateRequest {
    private String name;
    private String content;
    private String description;
    private String topic;
    private long deviceOfflineTimeout = MqttDeviceConstants.DEFAULT_DEVICE_OFFLINE_TIMEOUT;
}
