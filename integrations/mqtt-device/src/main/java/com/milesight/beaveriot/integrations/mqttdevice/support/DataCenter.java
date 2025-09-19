package com.milesight.beaveriot.integrations.mqttdevice.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.context.integration.wrapper.AnnotatedEntityWrapper;
import com.milesight.beaveriot.integrations.mqttdevice.entity.MqttDeviceIntegrationEntities;
import com.milesight.beaveriot.integrations.mqttdevice.model.DeviceTemplateAdditionalData;

import java.util.Map;

/**
 * author: Luxb
 * create: 2025/5/15 16:52
 **/
public class DataCenter {
    public static final String INTEGRATION_ID = "mqtt-device";
    public static final String DEFAULT_DEVICE_KEY = "device_key";
    public static final String DEVICE_ID_PLACEHOLDER = "${device_id}";

    public static void putTopic(String topic, Long deviceTemplateId) {
        Map<String, Long> topicMap = loadTopicMap();
        topicMap.put(topic, deviceTemplateId);
        saveTopicMap(topicMap);
    }

    public static String getTopic(Long deviceTemplateId) {
        return loadTopicMap().entrySet().stream().filter(entry -> entry.getValue().equals(deviceTemplateId)).map(Map.Entry::getKey).findFirst().orElse(null);
    }

    public static Long getTemplateIdByTopic(String topic) {
        Map<String, Long> topicMap = loadTopicMap();
        for (String eachTopic : topicMap.keySet()) {
            String topicPattern = TopicSupporter.convert(eachTopic);
            if (TopicSupporter.matches(topicPattern, topic)) {
                return topicMap.get(eachTopic);
            }
        }
        return null;
    }

    public static void removeTopic(String topic) {
        Map<String, Long> topicMap = loadTopicMap();
        topicMap.remove(topic);
        saveTopicMap(topicMap);
    }

    public static void removeTopicByTemplateId(Long deviceTemplateId) {
        Map<String, Long> topicMap = loadTopicMap();
        topicMap.entrySet().removeIf(entry -> entry.getValue().equals(deviceTemplateId));
        saveTopicMap(topicMap);
    }

    public static boolean isTopicExist(String topic) {
        return loadTopicMap().containsKey(topic);
    }

    private static Map<String, Long> loadTopicMap() {
        AnnotatedEntityWrapper<MqttDeviceIntegrationEntities> entitiesWrapper = new AnnotatedEntityWrapper<>();
        String topicMapStr = (String) entitiesWrapper.getValue(MqttDeviceIntegrationEntities::getTopicMap).orElse("{}");
        return JsonUtils.fromJSON(topicMapStr, new TypeReference<>() {});
    }

    private static void saveTopicMap(Map<String, Long> topicMap) {
        String topicMapStr = JsonUtils.toJSON(topicMap);
        AnnotatedEntityWrapper<MqttDeviceIntegrationEntities> entitiesWrapper = new AnnotatedEntityWrapper<>();
        entitiesWrapper.saveValue(MqttDeviceIntegrationEntities::getTopicMap, topicMapStr).publishSync();
    }

    private static Map<Long, DeviceTemplateAdditionalData> loadDeviceTemplateAdditionalDataMap() {
        AnnotatedEntityWrapper<MqttDeviceIntegrationEntities> entitiesWrapper = new AnnotatedEntityWrapper<>();
        String deviceTemplateAdditionalDataMapStr = (String) entitiesWrapper.getValue(MqttDeviceIntegrationEntities::getDeviceTemplateAdditionalDataMap).orElse("{}");
        return JsonUtils.fromJSON(deviceTemplateAdditionalDataMapStr, new TypeReference<>() {});
    }

    public static DeviceTemplateAdditionalData getDeviceTemplateAdditionalData(Long deviceTemplateId) {
        Map<Long, DeviceTemplateAdditionalData> deviceTemplateAdditionalDataMap = loadDeviceTemplateAdditionalDataMap();
        return deviceTemplateAdditionalDataMap.get(deviceTemplateId);
    }

    public static void saveDeviceTemplateAdditionalData(Long deviceTemplateId, DeviceTemplateAdditionalData deviceTemplateAdditionalData) {
        Map<Long, DeviceTemplateAdditionalData> deviceTemplateAdditionalDataMap = loadDeviceTemplateAdditionalDataMap();
        deviceTemplateAdditionalDataMap.put(deviceTemplateId, deviceTemplateAdditionalData);
        saveDeviceTemplateAdditionalDataMap(deviceTemplateAdditionalDataMap);
    }

    public static void saveDeviceTemplateAdditionalDataMap(Map<Long, DeviceTemplateAdditionalData> deviceTemplateAdditionalDataMap) {
        String deviceTemplateAdditionalDataMapStr = JsonUtils.toJSON(deviceTemplateAdditionalDataMap);
        AnnotatedEntityWrapper<MqttDeviceIntegrationEntities> entitiesWrapper = new AnnotatedEntityWrapper<>();
        entitiesWrapper.saveValue(MqttDeviceIntegrationEntities::getDeviceTemplateAdditionalDataMap, deviceTemplateAdditionalDataMapStr).publishSync();
    }

    public static void removeDeviceTemplateAdditionalData(Long deviceTemplateId) {
        Map<Long, DeviceTemplateAdditionalData> deviceTemplateAdditionalDataMap = loadDeviceTemplateAdditionalDataMap();
        deviceTemplateAdditionalDataMap.remove(deviceTemplateId);
        saveDeviceTemplateAdditionalDataMap(deviceTemplateAdditionalDataMap);
    }
}
