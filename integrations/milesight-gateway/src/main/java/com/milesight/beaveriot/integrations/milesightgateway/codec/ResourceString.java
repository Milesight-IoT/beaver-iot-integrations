package com.milesight.beaveriot.integrations.milesightgateway.codec;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * ResourceString class.
 *
 * @author simon
 * @date 2025/2/27
 */
public class ResourceString {
    private static final ObjectMapper JSON = JsonMapper.builder().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).build();

    static {
        JSON.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    private static final char[] forbiddenChars = {'[', ']', '#'};

    public static boolean containsForbiddenChars(String id) {
        for (char c : forbiddenChars) {
            if (id.indexOf(c) >= 0) return true;
        }

        return false;
    }

    private ResourceString() {}

    public static ObjectMapper jsonInstance() {
        return JSON;
    }
}
