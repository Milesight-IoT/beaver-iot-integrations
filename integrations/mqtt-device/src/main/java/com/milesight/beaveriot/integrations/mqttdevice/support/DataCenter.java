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
    public static final String DEVICE_ID_PLACEHOLDER = "${device_id}";
    private static final Map<String, String> templateKeyTopicMap = loadTopicMap();
    private static String userName;

    public static void putTopic(String deviceTemplateKey, String topic) {
        templateKeyTopicMap.put(deviceTemplateKey, topic);
        saveTopicMap();
    }

    public static String getTopic(String deviceTemplateKey) {
        return templateKeyTopicMap.get(deviceTemplateKey);
    }

    public static void removeTopic(String deviceTemplateKey) {
        templateKeyTopicMap.remove(deviceTemplateKey);
        saveTopicMap();
    }

    public static boolean isTopicExist(String topic) {
        return templateKeyTopicMap.containsValue(topic);
    }

    public static List<String> getDeviceTemplateKeys() {
        return new ArrayList<>(templateKeyTopicMap.keySet());
    }

    private static Map<String, String> loadTopicMap() {
        AnnotatedEntityWrapper<MqttDeviceIntegrationEntities> entitiesWrapper = new AnnotatedEntityWrapper<>();
        String topicMapStr = (String) entitiesWrapper.getValue(MqttDeviceIntegrationEntities::getTopicMap).orElse("{}");
        return JsonUtils.fromJSON(topicMapStr, new TypeReference<>() {});
    }

    private static void saveTopicMap() {
        String topicMapStr = JsonUtils.toJSON(templateKeyTopicMap);
        AnnotatedEntityWrapper<MqttDeviceIntegrationEntities> entitiesWrapper = new AnnotatedEntityWrapper<>();
        entitiesWrapper.saveValue(MqttDeviceIntegrationEntities::getTopicMap, topicMapStr);
    }

    public static void setUserName(String userName) {
        DataCenter.userName = userName;
    }

    public static String getUserName() {
        return DataCenter.userName;
    }
}
