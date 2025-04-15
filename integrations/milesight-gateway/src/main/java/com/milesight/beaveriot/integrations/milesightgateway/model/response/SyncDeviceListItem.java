package com.milesight.beaveriot.integrations.milesightgateway.model.response;

import lombok.Data;

/**
 * GatewaySyncDeviceItem class.
 *
 * @author simon
 * @date 2025/3/13
 */
@Data
public class SyncDeviceListItem {
    private String eui;

    private String name;

    private String guessModelId;
}
