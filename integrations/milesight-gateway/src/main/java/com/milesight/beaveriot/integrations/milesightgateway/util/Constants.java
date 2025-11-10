package com.milesight.beaveriot.integrations.milesightgateway.util;

/**
 * MilesightGatewayConstant class.
 *
 * @author simon
 * @date 2025/2/12
 */
public class Constants {
    public static final String INTEGRATION_ID = "milesight-gateway";

    public static final String GATEWAY_IDENTIFIER_PREFIX = "GW-";

    public static final String GATEWAY_MQTT_CLIENT_ID_PREFIX = "msgw:";

    public static final Integer CLIENT_ID_RANDOM_LENGTH = 6;

    public static final String OFFLINE_TIMEOUT_ENTITY_IDENTIFIER = "ms-offline-timeout";

    public static final String OFFLINE_TIMEOUT_ENTITY_UNIT = "min";

    public static final String OFFLINE_TIMEOUT_ENTITY_NAME = "Offline Timeout";

    public static final int OFFLINE_TIMEOUT_ENTITY_MAX_VALUE = 2880;

    public static final int OFFLINE_TIMEOUT_ENTITY_MIN_VALUE = 1;

    public static final String DEFAULT_DEVICE_OFFLINE_TIMEOUT_STR = "1500";

    public static final long DEFAULT_DEVICE_OFFLINE_TIMEOUT = Long.parseLong(DEFAULT_DEVICE_OFFLINE_TIMEOUT_STR);

    public static final String LORA_CLASS_METADATA_KEY = "lora_device_profile_class";

    public static final String GATEWAY_VERSION_V1 = "v1";

    public static final String GATEWAY_VERSION_V2 = "v2";

    private Constants() {}
}
