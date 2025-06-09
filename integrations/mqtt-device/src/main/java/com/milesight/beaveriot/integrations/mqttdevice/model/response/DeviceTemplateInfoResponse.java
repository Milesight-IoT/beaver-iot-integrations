package com.milesight.beaveriot.integrations.mqttdevice.model.response;

import com.milesight.beaveriot.context.model.response.DeviceTemplateResponseData;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.beans.BeanUtils;

/**
 * author: Luxb
 * create: 2025/6/9 18:08
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class DeviceTemplateInfoResponse extends DeviceTemplateResponseData {
    private String topic;

    public DeviceTemplateInfoResponse(DeviceTemplateResponseData deviceTemplateResponseData, String topic) {
        BeanUtils.copyProperties(deviceTemplateResponseData, this);
        this.topic = topic;
    }
}
