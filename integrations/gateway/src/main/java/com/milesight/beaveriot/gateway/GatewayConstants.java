package com.milesight.beaveriot.gateway;

public class GatewayConstants {
    private GatewayConstants() {}

    public static final String INTEGRATION_ID = "gateway";

    public static final String GATEWAY_GATEWAY_IP = INTEGRATION_ID + ".integration.gateway.gateway_IP";
    public static final String GATEWAY_USERNAME = INTEGRATION_ID + ".integration.gateway.username";
    public static final String GATEWAY_PASSWORD = INTEGRATION_ID + ".integration.gateway.password";

    public static final String GATEWAY_WEBHOOK_URL = INTEGRATION_ID + ".integration.gateway.webhook_url";
}
