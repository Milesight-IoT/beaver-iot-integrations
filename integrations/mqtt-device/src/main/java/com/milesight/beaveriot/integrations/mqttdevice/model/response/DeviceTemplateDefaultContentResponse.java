package com.milesight.beaveriot.integrations.mqttdevice.model.response;

import lombok.Data;

/**
 * author: Luxb
 * create: 2025/5/16 17:35
 **/
@Data
public class DeviceTemplateDefaultContentResponse {
    private String content;
    public static DeviceTemplateDefaultContentResponse build(String content) {
        DeviceTemplateDefaultContentResponse deviceTemplateDefaultContentResponse = new DeviceTemplateDefaultContentResponse();
        deviceTemplateDefaultContentResponse.setContent(content);
        return deviceTemplateDefaultContentResponse;
    }
}
