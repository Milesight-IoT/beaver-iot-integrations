package com.milesight.beaveriot.integrations.aiinference.support.image;

import java.util.HashMap;
import java.util.Map;

/**
 * author: Luxb
 * create: 2025/6/23 10:12
 **/
public class ColorManager {
    private final Map<Class<?>, Map<String, ColorPicker>> classColorMap;

    public ColorManager() {
        this.classColorMap = new HashMap<>();
    }

    public boolean isRegistered(Class<?> clazz) {
        return classColorMap.containsKey(clazz);
    }

    public void register(Class<?> clazz, Map<String, ColorPicker> filedColorMap) {
        if (filedColorMap != null) {
            classColorMap.put(clazz, filedColorMap);
        }
    }

    public ColorPicker getColorPicker(Class<?> clazz, String fieldName) {
        if (classColorMap.containsKey(clazz)) {
            Map<String, ColorPicker> filedColorMap = classColorMap.get(clazz);
            if (filedColorMap.containsKey(fieldName)) {
                return filedColorMap.get(fieldName);
            }
        }
        return null;
    }
}
