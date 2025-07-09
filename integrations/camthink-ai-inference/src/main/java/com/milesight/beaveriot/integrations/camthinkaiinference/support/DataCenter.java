package com.milesight.beaveriot.integrations.camthinkaiinference.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.context.integration.wrapper.AnnotatedEntityWrapper;
import com.milesight.beaveriot.integrations.camthinkaiinference.entity.CamThinkAiInferenceIntegrationEntities;

import java.util.Map;

/**
 * author: Luxb
 * create: 2025/6/19 8:43
 **/
public class DataCenter {
    public static Map<String, Long> loadDeviceImageEntityMap() {
        AnnotatedEntityWrapper<CamThinkAiInferenceIntegrationEntities> wrapper = new AnnotatedEntityWrapper<>();
        String jsonString = (String) wrapper.getValue(CamThinkAiInferenceIntegrationEntities::getDeviceImageEntityMap).orElse("{}");
        return JsonUtils.fromJSON(jsonString, new TypeReference<>() {});
    }

    public static void putDeviceImageEntity(String imageEntityKey, Long deviceId) {
        Map<String, Long> deviceImageEntityMap = loadDeviceImageEntityMap();
        deviceImageEntityMap.put(imageEntityKey, deviceId);
        saveDeviceImageEntityMap(deviceImageEntityMap);
    }

    public static Long getDeviceIdByImageEntityKey(String imageEntityKey) {
        Map<String, Long> deviceImageEntityMap = loadDeviceImageEntityMap();
        return deviceImageEntityMap.get(imageEntityKey);
    }
    public static void removeDeviceFromImageEntityMap(Long deviceId) {
        Map<String, Long> deviceImageEntityMap = loadDeviceImageEntityMap();
        deviceImageEntityMap.entrySet().removeIf(entry -> entry.getValue().equals(deviceId));
        saveDeviceImageEntityMap(deviceImageEntityMap);
    }

    public static boolean isDeviceInDeviceImageEntityMap(Long deviceId) {
        Map<String, Long> deviceImageEntityMap = loadDeviceImageEntityMap();
        return deviceImageEntityMap.containsValue(deviceId);
    }

    public static String getImageEntityKeyByDeviceId(Long deviceId) {
        Map<String, Long> deviceImageEntityMap = loadDeviceImageEntityMap();
        return deviceImageEntityMap.entrySet().stream()
                .filter(entry -> entry.getValue().equals(deviceId))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    private static void saveDeviceImageEntityMap(Map<String, Long> deviceImageEntityMap) {
        AnnotatedEntityWrapper<CamThinkAiInferenceIntegrationEntities> wrapper = new AnnotatedEntityWrapper<>();
        wrapper.saveValue(CamThinkAiInferenceIntegrationEntities::getDeviceImageEntityMap, JsonUtils.toJSON(deviceImageEntityMap));
    }
}
