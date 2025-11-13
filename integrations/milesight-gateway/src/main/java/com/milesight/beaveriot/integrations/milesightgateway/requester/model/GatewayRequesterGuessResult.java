package com.milesight.beaveriot.integrations.milesightgateway.requester.model;

import com.milesight.beaveriot.integrations.milesightgateway.model.api.DeviceListResponse;
import com.milesight.beaveriot.integrations.milesightgateway.mqtt.model.MqttResponse;
import com.milesight.beaveriot.integrations.milesightgateway.requester.GatewayRequester;
import lombok.Data;

/**
 * GatewayRequesterGuessResult class.
 *
 * @author simon
 * @date 2025/10/30
 */
@Data
public class GatewayRequesterGuessResult {
    private GatewayRequester gatewayRequester;

    private MqttResponse<DeviceListResponse> baseResponse;
}
