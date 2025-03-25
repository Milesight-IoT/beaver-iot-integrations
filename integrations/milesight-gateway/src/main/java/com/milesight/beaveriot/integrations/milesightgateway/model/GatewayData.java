package com.milesight.beaveriot.integrations.milesightgateway.model;

import com.milesight.beaveriot.integrations.milesightgateway.util.GatewayString;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.util.Map;

/**
 * GatewayData class.
 *
 * Store as a list in gatewayList of integration entity.
 *
 * @author simon
 * @date 2025/2/24
 */
@Data
@FieldNameConstants
public class GatewayData {
    private String eui;

    private String applicationId;

    private String credentialId;

    private String clientId;

    private Map<String, String> profile;

    public void setEui(String eui) {
        this.eui = GatewayString.standardizeEUI(eui);
    }
}
