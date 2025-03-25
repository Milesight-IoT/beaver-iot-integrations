package com.milesight.beaveriot.integrations.milesightgateway.model.request;

import lombok.Data;

/**
 * FetchGatewayCredentialRequest class.
 *
 * @author simon
 * @date 2025/3/21
 */
@Data
public class FetchGatewayCredentialRequest {
    private String eui;

    private String credentialId;
}
