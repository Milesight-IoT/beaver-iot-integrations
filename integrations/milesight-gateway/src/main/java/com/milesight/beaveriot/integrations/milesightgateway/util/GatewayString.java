package com.milesight.beaveriot.integrations.milesightgateway.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.security.SecureRandom;
import java.util.Map;

/**
 * MilesightJson class.
 *
 * @author simon
 * @date 2025/2/25
 */
public class GatewayString {
    private static final ObjectMapper JSON = JsonMapper.builder().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).build();

    private static final String HEX_PATTERN = "^[0-9a-fA-F]{12,16}$";

    public static ObjectMapper jsonInstance() {
        return JSON;
    }

    public static String standardizeEUI(String eui) {
        if (!eui.matches(HEX_PATTERN)) {
            throw new IllegalArgumentException("Not a valid eui: " + eui);
        }

        return eui.toUpperCase();
    }

    public static Map<String, Object> convertToMap(Object obj) {
        return JSON.convertValue(obj, new TypeReference<>() {});
    }

    public static String getGatewayIdentifier(String eui) {
        return Constants.GATEWAY_IDENTIFIER_PREFIX + standardizeEUI(eui);
    }

    public static boolean isGatewayIdentifier(String identifier) {
        return identifier.startsWith(Constants.GATEWAY_IDENTIFIER_PREFIX);
    }

    public static String getDeviceKey(String deviceEui) {
        return Constants.INTEGRATION_ID + ".device." + deviceEui;
    }

    public static String getDeviceEntityKey(String deviceEui, String entityIdentifier) {
        return getDeviceKey(deviceEui) + "." + entityIdentifier;
    }

    public static String getDeviceEuiByKey(String deviceKey) {
        String[] keyParts = deviceKey.split("\\.");
        String eui = keyParts[keyParts.length - 1];
        if (isGatewayIdentifier(eui)) {
            eui = eui.substring(Constants.GATEWAY_IDENTIFIER_PREFIX.length());
        }

        return eui;
    }

    private static final String CLIENT_ID_RANDOM_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = SECURE_RANDOM.nextInt(CLIENT_ID_RANDOM_CHARS.length());
            sb.append(CLIENT_ID_RANDOM_CHARS.charAt(index));
        }
        return sb.toString();
    }

    private GatewayString() {}
}
