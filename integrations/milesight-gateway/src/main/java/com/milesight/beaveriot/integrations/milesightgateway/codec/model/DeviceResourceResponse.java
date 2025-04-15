package com.milesight.beaveriot.integrations.milesightgateway.codec.model;

import lombok.Data;

import java.util.List;

/**
 * DeviceResourceResponse class.
 *
 * @author simon
 * @date 2025/2/27
 */
@Data
public class DeviceResourceResponse {
    String version;

    List<DeviceResourceInfo> devices;
}
