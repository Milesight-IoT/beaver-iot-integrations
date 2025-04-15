package com.milesight.beaveriot.integrations.milesightgateway.model.request;

import lombok.Data;

/**
 * ConnectionValidateRequest class.
 *
 * @author simon
 * @date 2025/2/24
 */
@Data
public class ConnectionValidateRequest {
    private String eui;

    private String credentialId;
}
