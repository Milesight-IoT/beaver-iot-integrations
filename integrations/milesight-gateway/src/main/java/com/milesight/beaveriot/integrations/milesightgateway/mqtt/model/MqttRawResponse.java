package com.milesight.beaveriot.integrations.milesightgateway.mqtt.model;

import lombok.Data;

import java.util.Map;

/**
 * MqttRawResponse class.
 *
 * @author simon
 * @date 2025/2/14
 */
@Data
public class MqttRawResponse {
    private String id;

    private String method;

    private String url;

    private MqttResponseContext ctx = new MqttResponseContext();

    private Map<String, Object> body;
}
