package com.milesight.beaveriot.integrations.mqttdevice.model.response;

import com.milesight.beaveriot.context.model.response.DeviceTemplateResponseData;
import com.milesight.beaveriot.context.support.SpringContext;
import com.milesight.beaveriot.integrations.mqttdevice.service.MqttDeviceTemplateService;
import com.milesight.beaveriot.integrations.mqttdevice.support.DataCenter;
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
    private long deviceOfflineTimeout;

    protected DeviceTemplateInfoResponse(DeviceTemplateResponseData deviceTemplateResponseData) {
        BeanUtils.copyProperties(deviceTemplateResponseData, this);

        Long deviceTemplateId = Long.parseLong(deviceTemplateResponseData.getId());
        topic = DataCenter.getTopic(deviceTemplateId);
        MqttDeviceTemplateService mqttDeviceTemplateService = SpringContext.getBean(MqttDeviceTemplateService.class);
        deviceOfflineTimeout = mqttDeviceTemplateService.getDeviceOfflineTimeout(deviceTemplateId);
    }

    public static DeviceTemplateInfoResponse build(DeviceTemplateResponseData deviceTemplateResponseData) {
        return new DeviceTemplateInfoResponse(deviceTemplateResponseData);
    }
}
