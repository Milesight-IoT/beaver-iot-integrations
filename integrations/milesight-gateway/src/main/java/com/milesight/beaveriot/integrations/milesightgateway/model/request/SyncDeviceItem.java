package com.milesight.beaveriot.integrations.milesightgateway.model.request;

import lombok.Data;

/**
 * SyncDeviceItem class.
 *
 * @author simon
 * @date 2025/3/13
 */
@Data
public class SyncDeviceItem {
    private String eui;

    private String modelId;
}
