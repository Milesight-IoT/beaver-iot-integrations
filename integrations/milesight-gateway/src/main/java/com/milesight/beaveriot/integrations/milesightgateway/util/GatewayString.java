package com.milesight.beaveriot.integrations.milesightgateway.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.security.SecureRandom;
import java.util.Map;
import java.util.zip.CRC32;

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

    public static String getGatewayKey(String gatewayEui) {
        return Constants.INTEGRATION_ID + "." + "device" + "." + getGatewayIdentifier(gatewayEui);
    }

    public static String parseGatewayIdentifier(String key) {
        return key.split("\\.")[2];
    }

    public static String getDeviceKey(String deviceEui) {
        return Constants.INTEGRATION_ID + ".device." + deviceEui;
    }

    public static String getDeviceEntityKey(String deviceEui, String entityIdentifier) {
        return getDeviceKey(deviceEui) + "." + entityIdentifier;
    }

    public static String getDeviceIdentifierByKey(String deviceKey) {
        String[] keyParts = deviceKey.split("\\.");
        return keyParts[keyParts.length - 1];
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

    public static String hashTo6Digits(String input) {
        CRC32 crc = new CRC32();
        crc.update(input.getBytes());
        long hashValue = crc.getValue();
        int hash = (int) (hashValue % 1000000);
        return String.format("%06d", hash);
    }

    public static String generateGatewayClientId(String gatewayEui) {
        String strToHash = Constants.GATEWAY_MQTT_CLIENT_ID_PREFIX + gatewayEui + ":" + GatewayString.generateRandomString(Constants.CLIENT_ID_RANDOM_LENGTH);
        return strToHash + ":" + hashTo6Digits(strToHash);
    }

    public static boolean validateGatewayClientId(String clientId, String gatewayEui) {
        String[] parts = clientId.split(":");
        if (parts.length != 4) {
            return false;
        }

        if (!(parts[0] + ":").equals(Constants.GATEWAY_MQTT_CLIENT_ID_PREFIX)) {
            return false;
        }

        if (!parts[1].equals(gatewayEui)) {
            return false;
        }

        if (parts[2].length() != Constants.CLIENT_ID_RANDOM_LENGTH) {
            return false;
        }

        return hashTo6Digits(parts[0] + ":" + parts[1] + ":" + parts[2]).equals(parts[3]);
    }

    public static String parseGatewayEuiFromClientId(String clientId) {
        String[] parts = clientId.split(":");
        return parts.length < 2 ? null : parts[1];
    }

    private GatewayString() {}
}
