package com.milesight.beaveriot.integrations.milesightgateway.model.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DeviceLIstAppItem class.
 *
 * @author simon
 * @date 2025/2/24
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeviceListAppItem {
    private String appName;

    private String applicationID;
}
