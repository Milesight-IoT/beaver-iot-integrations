package com.milesight.beaveriot.integrations.milesightgateway.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * DeviceModelIdentifier class.
 *
 * @author simon
 * @date 2025/9/22
 */
@Data
@AllArgsConstructor
public class DeviceModelIdentifier {
    private String vendorId;

    private String modelId;

    public static DeviceModelIdentifier of(String deviceModelId) {
        String[] parts = deviceModelId.split("@");
        assert parts.length == 2;

        return new DeviceModelIdentifier(parts[1], parts[0]);
    }

    @Override
    public String toString() {
        return getModelId() + "@" + getVendorId();
    }
}
