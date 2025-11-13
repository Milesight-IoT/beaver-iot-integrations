package com.milesight.beaveriot.integrations.milesightgateway.model.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DeviceListProfileItem class.
 *
 * @author simon
 * @date 2025/2/24
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeviceListProfileItem {
    private String profileID;

    private String profileName;
}
