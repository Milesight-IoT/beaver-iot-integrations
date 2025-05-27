package com.milesight.beaveriot.integrations.mqttdevice.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.context.integration.wrapper.AnnotatedEntityWrapper;
import com.milesight.beaveriot.integrations.mqttdevice.entity.MqttDeviceIntegrationEntities;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * author: Luxb
 * create: 2025/5/15 16:52
 **/
public class DataCenter {
    public static final String INTEGRATION_ID = "mqtt-device";
    public static final String TOPIC_KEY = "topic";
    public static final String DEVICE_ID_PLACEHOLDER = "${device_id}";
    private static final Map<Long, String> templateIdTopicMap = loadTopicMap();

    public static void putTopic(Long deviceTemplateId, String topic) {
        templateIdTopicMap.put(deviceTemplateId, topic);
        saveTopicMap();
    }

    public static String getTopic(Long deviceTemplateId) {
        return templateIdTopicMap.get(deviceTemplateId);
    }

    public static void removeTopic(Long deviceTemplateId) {
        templateIdTopicMap.remove(deviceTemplateId);
        saveTopicMap();
    }

    public static boolean isTopicExist(String topic) {
        return templateIdTopicMap.containsValue(topic);
    }

    public static List<Long> getDeviceTemplateIds() {
        return new ArrayList<>(templateIdTopicMap.keySet());
    }

    private static Map<Long, String> loadTopicMap() {
        AnnotatedEntityWrapper<MqttDeviceIntegrationEntities> entitiesWrapper = new AnnotatedEntityWrapper<>();
        String topicMapStr = (String) entitiesWrapper.getValue(MqttDeviceIntegrationEntities::getTopicMap).orElse("{}");
        return JsonUtils.fromJSON(topicMapStr, new TypeReference<>() {});
    }

    private static void saveTopicMap() {
        String topicMapStr = JsonUtils.toJSON(templateIdTopicMap);
        AnnotatedEntityWrapper<MqttDeviceIntegrationEntities> entitiesWrapper = new AnnotatedEntityWrapper<>();
        entitiesWrapper.saveValue(MqttDeviceIntegrationEntities::getTopicMap, topicMapStr);
    }
}
