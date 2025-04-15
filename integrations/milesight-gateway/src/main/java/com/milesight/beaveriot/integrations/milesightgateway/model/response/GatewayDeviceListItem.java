package com.milesight.beaveriot.integrations.milesightgateway.model.response;

import lombok.Data;

/**
 * GatewayDeviceListItem class.
 *
 * @author simon
 * @date 2025/3/12
 */
@Data
public class GatewayDeviceListItem {
    private String id;

    private String key;

    private String name;

    private String eui;

    private Long createdAt;
}
