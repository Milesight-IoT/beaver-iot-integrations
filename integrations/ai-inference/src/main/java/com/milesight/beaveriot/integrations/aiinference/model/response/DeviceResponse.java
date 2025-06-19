package com.milesight.beaveriot.integrations.aiinference.model.response;

import com.milesight.beaveriot.context.integration.model.Device;
import lombok.Data;

import java.util.List;

/**
 * author: Luxb
 * create: 2025/6/18 16:00
 **/
@Data
public class DeviceResponse {
    private List<Device> content;

    private DeviceResponse(List<Device> devices) {
        this.content = devices;
    }

    public static DeviceResponse build(List<Device> devices) {
        return new DeviceResponse(devices);
    }
}
