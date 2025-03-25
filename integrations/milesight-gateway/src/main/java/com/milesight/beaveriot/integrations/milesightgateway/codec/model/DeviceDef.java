package com.milesight.beaveriot.integrations.milesightgateway.codec.model;

import lombok.Data;

import java.util.List;

/**
 * DeviceDef class.
 *
 * @author simon
 * @date 2025/2/17
 */
@Data
public class DeviceDef {
    // Definition version
    private String version;
    // Example bytes
    private String bytes;
    // Device objects
    private List<DeviceDefObject> object;
}
