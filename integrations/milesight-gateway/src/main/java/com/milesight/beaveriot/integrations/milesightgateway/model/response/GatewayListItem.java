package com.milesight.beaveriot.integrations.milesightgateway.model.response;

import lombok.Data;

/**
 * GatewayListResponse class.
 *
 * @author simon
 * @date 2025/3/12
 */
@Data
public class GatewayListItem {
    private String deviceId;

    private String deviceKey;

    private String name;

    private String status;

    private String credentialId;

    private Integer deviceCount;

    private String applicationId;

    private String eui;
}
