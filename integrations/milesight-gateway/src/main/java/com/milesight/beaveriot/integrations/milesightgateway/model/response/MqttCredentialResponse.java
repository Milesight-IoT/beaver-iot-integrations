package com.milesight.beaveriot.integrations.milesightgateway.model.response;

import lombok.Data;

/**
 * GatewayCredentialResponse class.
 *
 * @author simon
 * @date 2025/3/12
 */
@Data
public class MqttCredentialResponse {
    private String credentialId;

    private String username;

    private String password;

    private String clientId;

    private String uplinkDataTopic;

    private String downlinkDataTopic;

    private String requestDataTopic;

    private String responseDataTopic;
}
