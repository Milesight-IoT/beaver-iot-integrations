package com.milesight.beaveriot.integrations.milesightgateway.model.request;

import lombok.Data;

import java.util.List;

/**
 * SyncGatewayDeviceRequest class.
 *
 * @author simon
 * @date 2025/3/13
 */
@Data
public class SyncGatewayDeviceRequest {
    private List<SyncDeviceItem> devices;
}
