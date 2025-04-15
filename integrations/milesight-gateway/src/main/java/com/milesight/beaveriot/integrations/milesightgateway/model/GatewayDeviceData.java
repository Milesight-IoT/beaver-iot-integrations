package com.milesight.beaveriot.integrations.milesightgateway.model;

import lombok.Data;

/**
 * GatewayDeviceData class.
 *
 * Store in device additional data.
 *
 * @author simon
 * @date 2025/2/27
 */
@Data
public class GatewayDeviceData {
    String eui;

    String deviceModel;

    String gatewayEUI;

    Long fPort;

    Boolean frameCounterValidation;

    String appKey;
}
