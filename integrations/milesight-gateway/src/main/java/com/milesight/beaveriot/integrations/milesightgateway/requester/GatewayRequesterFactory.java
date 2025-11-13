package com.milesight.beaveriot.integrations.milesightgateway.requester;

import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.integrations.milesightgateway.model.GatewayData;
import com.milesight.beaveriot.integrations.milesightgateway.model.MilesightGatewayErrorCode;
import com.milesight.beaveriot.integrations.milesightgateway.model.api.DeviceListResponse;
import com.milesight.beaveriot.integrations.milesightgateway.mqtt.MsGwMqttClient;
import com.milesight.beaveriot.integrations.milesightgateway.mqtt.model.MqttResponse;
import com.milesight.beaveriot.integrations.milesightgateway.requester.model.GatewayRequesterGuessResult;
import com.milesight.beaveriot.integrations.milesightgateway.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * GatewayRequesterFactory class.
 *
 * @author simon
 * @date 2025/10/30
 */
@Component("milesightGatewayRequesterFactory")
@Slf4j
public class GatewayRequesterFactory {
    @Autowired
    MsGwMqttClient msGwMqttClient;

    public GatewayRequester create(String gatewayEui, String version, String applicationId) {
        if (!StringUtils.hasText(version)) {
            return new GatewayRequesterV1(msGwMqttClient, gatewayEui, applicationId, true);
        } else if (version.equals(Constants.GATEWAY_VERSION_V1)) {
            return new GatewayRequesterV1(msGwMqttClient, gatewayEui, applicationId, false);
        } else if (version.equals(Constants.GATEWAY_VERSION_V2)) {
            return new GatewayRequesterV2(msGwMqttClient, gatewayEui, applicationId);
        }

        throw new IllegalArgumentException("Not supported gateway version: " + version);
    }

    public GatewayRequester create(String gatewayEui, String version) {
        return create(gatewayEui, version, null);
    }

    public GatewayRequester create(GatewayData gatewayData) {
        return create(gatewayData.getEui(), gatewayData.getVersion(), gatewayData.getApplicationId());
    }

    private GatewayRequesterGuessResult doGuess(String gatewayEui, String version) {
        GatewayRequester requester = create(gatewayEui, version);
        MqttResponse<DeviceListResponse> baseResponse = requester.requestBase();
        GatewayRequesterGuessResult result = new GatewayRequesterGuessResult();
        result.setGatewayRequester(requester);
        result.setBaseResponse(baseResponse);
        return result;
    }

    public GatewayRequesterGuessResult guess(String gatewayEui) {
        try {
            return this.doGuess(gatewayEui, Constants.GATEWAY_VERSION_V1);
        } catch (ServiceException e) {
            if (!e.getErrorCode().equals(MilesightGatewayErrorCode.GATEWAY_RESPOND_ERROR.getErrorCode())) {
                throw e;
            }
        }

        return this.doGuess(gatewayEui, Constants.GATEWAY_VERSION_V2);
    }
}
