package com.milesight.beaveriot.integrations.mqttdevice.support;

import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * author: Luxb
 * create: 2025/5/15 16:52
 **/
public class DataCenter {
    public static final String INTEGRATION_ID = "mqtt-device";
    public static final String TOPIC_KEY = "topic";
    public static final String DEVICE_ID_PLACEHOLDER = "${device_id}";
    @Getter
    private static final Map<Long, String> templateIdTopicMap = new ConcurrentHashMap<>();
}
