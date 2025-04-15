package com.milesight.beaveriot.integrations.milesightgateway.mqtt;

import com.milesight.beaveriot.integrations.milesightgateway.util.Constants;

/**
 * MsGwMqttUtil class.
 *
 * @author simon
 * @date 2025/3/21
 */
public class MsGwMqttUtil {
    private MsGwMqttUtil() {}

    public static String parseGatewayIdFromTopic(String topic) {
        String[] levels = topic.split("/");
        final int gatewayIdPos = 1;
        if (levels.length <= gatewayIdPos) {
            return null;
        }

        return levels[gatewayIdPos];
    }

    public static String getMqttTopic(String gatewayEui, String scope) {
        return Constants.INTEGRATION_ID + "/" + gatewayEui + "/" + scope;
    }
}
