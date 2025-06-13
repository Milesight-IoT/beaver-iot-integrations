package com.milesight.beaveriot.integrations.mqttdevice.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * author: Luxb
 * create: 2025/6/9 18:08
 **/
@Data
public class DeviceTemplateTestResponse {
    private List<EntityData> entities = new ArrayList<>();

    public void addEntityData(String entityName, Object value) {
        entities.add(new EntityData(entityName, value));
    }

    @AllArgsConstructor
    @Data
    public static class EntityData {
        private String entityName;
        private Object value;
    }
}
