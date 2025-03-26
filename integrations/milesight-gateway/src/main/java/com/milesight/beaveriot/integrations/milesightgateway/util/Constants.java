package com.milesight.beaveriot.integrations.milesightgateway.util;

import java.util.List;

/**
 * MilesightGatewayConstant class.
 *
 * @author simon
 * @date 2025/2/12
 */
public class Constants {
    public static final String INTEGRATION_ID = "milesight-gateway";

    public static final List<String> AVAILABLE_PROFILES = List.of("ClassA-OTAA", "ClassB-OTAA", "ClassC-OTAA", "ClassCB-OTAA");

    public static final String DEFAULT_APP_KEY = "5572404c696e6b4c6f52613230313823";

    public static final String GATEWAY_IDENTIFIER_PREFIX = "GW-";

    public static final String GATEWAY_MQTT_CLIENT_ID_PREFIX = "msgw:";

    public static final String GATEWAY_MQTT_UPLINK_SCOPE = "uplink";

    public static final String GATEWAY_MQTT_DOWNLINK_SCOPE = "downlink";

    public static final String GATEWAY_MQTT_REQUEST_SCOPE = "request";

    public static final String GATEWAY_MQTT_RESPONSE_SCOPE = "response";

    public static final Integer CLIENT_ID_RANDOM_LENGTH = 6;

    private Constants() {}
}
