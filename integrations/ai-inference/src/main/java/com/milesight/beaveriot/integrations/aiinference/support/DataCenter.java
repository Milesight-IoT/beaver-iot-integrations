package com.milesight.beaveriot.integrations.aiinference.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.context.integration.wrapper.AnnotatedEntityWrapper;
import com.milesight.beaveriot.integrations.aiinference.entity.AiInferenceIntegrationEntities;

import java.util.List;
import java.util.Map;

/**
 * author: Luxb
 * create: 2025/6/19 8:43
 **/
public class DataCenter {
    public static Map<Long, String> loadDeviceImageEntityMap() {
        AnnotatedEntityWrapper<AiInferenceIntegrationEntities> wrapper = new AnnotatedEntityWrapper<>();
        String jsonString = (String) wrapper.getValue(AiInferenceIntegrationEntities::getDeviceImageEntityMap).orElse("{}");
        return JsonUtils.fromJSON(jsonString, new TypeReference<>() {});
    }

    public static void putDeviceImageEntity(Long deviceId, String imageEntityKey) {
        Map<Long, String> deviceImageEntityMap = loadDeviceImageEntityMap();
        deviceImageEntityMap.put(deviceId, imageEntityKey);
        saveDeviceImageEntityMap(deviceImageEntityMap);
    }

    public static void removeDevice(Long deviceId) {
        Map<Long, String> deviceImageEntityMap = loadDeviceImageEntityMap();
        deviceImageEntityMap.remove(deviceId);
        saveDeviceImageEntityMap(deviceImageEntityMap);
    }

    public static boolean isDeviceInDeviceImageEntityMap(Long deviceId) {
        Map<Long, String> deviceImageEntityMap = loadDeviceImageEntityMap();
        return deviceImageEntityMap.containsKey(deviceId);
    }

    public static String getImageEntityKeyByDeviceId(Long deviceId) {
        Map<Long, String> deviceImageEntityMap = loadDeviceImageEntityMap();
        return deviceImageEntityMap.get(deviceId);
    }

    public static List<Long> getDeviceIdListByImageEntityKey(String imageEntityKey) {
        Map<Long, String> deviceImageEntityMap = loadDeviceImageEntityMap();
        return deviceImageEntityMap.entrySet().stream()
                .filter(entry -> entry.getValue().equals(imageEntityKey))
                .map(Map.Entry::getKey)
                .toList();
    }

    private static void saveDeviceImageEntityMap(Map<Long, String> deviceImageEntityMap) {
        AnnotatedEntityWrapper<AiInferenceIntegrationEntities> wrapper = new AnnotatedEntityWrapper<>();
        wrapper.saveValue(AiInferenceIntegrationEntities::getDeviceImageEntityMap, JsonUtils.toJSON(deviceImageEntityMap));
    }
}
