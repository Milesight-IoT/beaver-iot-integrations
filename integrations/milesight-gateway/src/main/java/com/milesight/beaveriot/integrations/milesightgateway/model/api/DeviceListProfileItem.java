package com.milesight.beaveriot.integrations.milesightgateway.model.api;

import lombok.Data;

/**
 * DeviceListProfileItem class.
 *
 * @author simon
 * @date 2025/2/24
 */
@Data
public class DeviceListProfileItem {
    private String profileID;

    private String profileName;

    private Boolean supportsJoin;
}
