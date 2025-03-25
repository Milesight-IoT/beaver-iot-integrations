package com.milesight.beaveriot.integrations.milesightgateway.mqtt.model;

import lombok.Data;

import java.util.Map;
import java.util.UUID;

/**
 * MqttRequest class.
 *
 * @author simon
 * @date 2025/2/14
 */
@Data
public class MqttRequest {
    private String type = "ns-api";

    private String id;

    private String method;

    private String url;

    private Map<String, Object> body;

    public MqttRequest() {
        this.id = UUID.randomUUID().toString();
    }

    public static MqttRequest createFrom(MqttRequest fromReq) {
        MqttRequest req = new MqttRequest();
        req.setType(fromReq.getType());
        req.setMethod(fromReq.getMethod());
        req.setUrl(fromReq.getUrl());
        req.setBody(fromReq.getBody());
        return req;
    }
}
