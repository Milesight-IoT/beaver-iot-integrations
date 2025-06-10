package com.milesight.beaveriot.integrations.mqttdevice.model.request;

import lombok.Data;

import java.util.List;

@Data
public class BatchDeleteDeviceTemplateRequest {
    private List<String> keyList;
}
