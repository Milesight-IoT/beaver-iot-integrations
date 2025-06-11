package com.milesight.beaveriot.integrations.mqttdevice.model.response;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * author: Luxb
 * create: 2025/6/11 10:50
 **/
@Data
public class DeviceTemplateOutputResponse {
    private List<Map<String, Object>> outputs;
}
