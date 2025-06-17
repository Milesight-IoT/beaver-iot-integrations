package com.milesight.beaveriot.integrations.mqttdevice.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.context.integration.wrapper.AnnotatedEntityWrapper;
import com.milesight.beaveriot.context.security.TenantContext;
import com.milesight.beaveriot.integrations.mqttdevice.entity.MqttDeviceIntegrationEntities;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * author: Luxb
 * create: 2025/5/15 16:52
 **/
public class DataCenter {
    public static final String INTEGRATION_ID = "mqtt-device";
    public static final String DEFAULT_DEVICE_KEY = "device_key";
    public static final String DEVICE_ID_PLACEHOLDER = "${device_id}";
    private static final Map<String, Map<String, Long>> topicMap = loadTopicMap();

    public static void putTopic(String topic, Long deviceTemplateId) {
        getTenantTopicMap().put(topic, deviceTemplateId);
        saveTopicMap();
    }

    public static String getTopic(Long deviceTemplateId) {
        return getTenantTopicMap().entrySet().stream().filter(entry -> entry.getValue().equals(deviceTemplateId)).map(Map.Entry::getKey).findFirst().orElse(null);
    }

    public static Long getTemplateIdByTopic(String topic) {
        for (String eachTopic : getTenantTopicMap().keySet()) {
            String topicPattern = TopicSupporter.convert(eachTopic);
            if (TopicSupporter.matches(topicPattern, topic)) {
                return getTenantTopicMap().get(eachTopic);
            }
        }
        return null;
    }

    public static void removeTopic(String topic) {
        getTenantTopicMap().remove(topic);
        saveTopicMap();
    }

    public static void removeTopicByTemplateId(Long deviceTemplateId) {
        getTenantTopicMap().entrySet().removeIf(entry -> entry.getValue().equals(deviceTemplateId));
        saveTopicMap();
    }

    public static boolean isTopicExist(String topic) {
        return getTenantTopicMap().containsKey(topic);
    }

    private static Map<String, Map<String, Long>> loadTopicMap() {
        AnnotatedEntityWrapper<MqttDeviceIntegrationEntities> entitiesWrapper = new AnnotatedEntityWrapper<>();
        String topicMapStr = (String) entitiesWrapper.getValue(MqttDeviceIntegrationEntities::getTopicMap).orElse("{}");
        return JsonUtils.fromJSON(topicMapStr, new TypeReference<>() {});
    }

    private static Map<String, Long> getTenantTopicMap() {
        String tenantId = TenantContext.getTenantId();
        return topicMap.computeIfAbsent(tenantId, k -> new ConcurrentHashMap<>());
    }

    private static void saveTopicMap() {
        String topicMapStr = JsonUtils.toJSON(topicMap);
        AnnotatedEntityWrapper<MqttDeviceIntegrationEntities> entitiesWrapper = new AnnotatedEntityWrapper<>();
        entitiesWrapper.saveValue(MqttDeviceIntegrationEntities::getTopicMap, topicMapStr).publishSync();
    }
}
