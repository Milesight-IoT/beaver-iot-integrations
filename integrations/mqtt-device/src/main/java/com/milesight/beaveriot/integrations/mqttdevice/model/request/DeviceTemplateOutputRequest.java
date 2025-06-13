package com.milesight.beaveriot.integrations.mqttdevice.model.request;

import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import lombok.Data;

/**
 * author: Luxb
 * create: 2025/6/11 10:50
 **/
@Data
public class DeviceTemplateOutputRequest {
    private ExchangePayload exchange;
}