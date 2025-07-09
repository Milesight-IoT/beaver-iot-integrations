package com.milesight.beaveriot.integrations.mqttdevice.model.request;

import lombok.Data;

@Data
public class CreateDeviceTemplateRequest {
    private String name;

    private String content;

    private String description;

    private String topic;
}
