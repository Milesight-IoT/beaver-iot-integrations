package com.milesight.beaveriot.integrations.milesightgateway.mqtt;

import com.milesight.beaveriot.integrations.milesightgateway.util.Constants;
import jakarta.annotation.Nullable;
import org.springframework.util.StringUtils;

/**
 * MsGwMqttUtil class.
 *
 * @author simon
 * @date 2025/3/21
 */
public class MsGwMqttUtil {
    private MsGwMqttUtil() {}

    public static final String MQTT_TOPIC_PLACEHOLDER = "+";

    public static final String DEVICE_EUI_PLACEHOLDER = "$deveui";

    private static final String GATEWAY_MQTT_UPLINK_SCOPE = "uplink";

    private static final String GATEWAY_MQTT_DOWNLINK_SCOPE = "downlink";

    private static final String GATEWAY_MQTT_REQUEST_SCOPE = "request";

    private static final String GATEWAY_MQTT_RESPONSE_SCOPE = "response";

    public static String parseGatewayIdFromTopic(String topic) {
        String[] levels = topic.split("/");
        final int gatewayIdPos = 1;
        if (levels.length <= gatewayIdPos) {
            return null;
        }

        return levels[gatewayIdPos];
    }

    private static String getBaseMqttTopic(String gatewayEui, String scope) {
        return Constants.INTEGRATION_ID + "/" + gatewayEui + "/" + scope;
    }

    public static String getUplinkTopic(String gatewayEui) {
        return getBaseMqttTopic(gatewayEui, GATEWAY_MQTT_UPLINK_SCOPE);
    }

    public static String getDownlinkTopic(String gatewayEui, @Nullable String deviceEui) {
        String baseTopic = getBaseMqttTopic(gatewayEui, GATEWAY_MQTT_DOWNLINK_SCOPE);
        if (!StringUtils.hasText(deviceEui)) {
            return baseTopic;
        }

        return baseTopic + "/" + deviceEui;
    }

    public static String getRequestTopic(String gatewayEui) {
        return getBaseMqttTopic(gatewayEui, GATEWAY_MQTT_REQUEST_SCOPE);
    }

    public static String getResponseTopic(String gatewayEui) {
        return getBaseMqttTopic(gatewayEui, GATEWAY_MQTT_RESPONSE_SCOPE);
    }
}
