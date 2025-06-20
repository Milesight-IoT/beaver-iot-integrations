package com.milesight.beaveriot.integrations.aiinference.model.response;

import lombok.Data;

import java.util.List;

/**
 * author: Luxb
 * create: 2025/6/18 16:00
 **/
@Data
public class DeviceResponse {
    private List<DeviceData> content;

    private DeviceResponse(List<DeviceData> devices) {
        this.content = devices;
    }

    public static DeviceResponse build(List<DeviceData> devices) {
        return new DeviceResponse(devices);
    }
}
