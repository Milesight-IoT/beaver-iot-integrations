package com.milesight.beaveriot.integrations.milesightgateway.mqtt.model;

import lombok.Data;

import java.util.Map;

/**
 * MqttRequestError class.
 *
 * @author simon
 * @date 2025/2/14
 */
@Data
public class MqttRequestError {
    Integer code;

    String error;

    public Map<String, Object> toMap() {
        return Map.of(
                "code", code,
                "error", error
        );
    }
}
