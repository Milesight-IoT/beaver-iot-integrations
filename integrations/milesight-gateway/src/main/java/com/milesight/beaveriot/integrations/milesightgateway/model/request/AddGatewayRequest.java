package com.milesight.beaveriot.integrations.milesightgateway.model.request;

import lombok.Data;

import java.util.Map;

/**
 * AddGatewayRequest class.
 *
 * @author simon
 * @date 2025/2/24
 */
@Data
public class AddGatewayRequest {
    private String name;

    private String eui;

    private String applicationId;

    private String credentialId;

    private String clientId;

    private String version;
}
