package com.milesight.beaveriot.common.entity;

import com.milesight.beaveriot.context.integration.model.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Author yuanh
 * @Description
 * @Package com.milesight.beaveriot.entity
 * @Date 2024/11/25 16:02
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IntegrationDeviceBuilder {
    private String integrationId;
    @Accessors(fluent = true)
    private String name;
    @Accessors(fluent = true)
    private String identifier;
    @Accessors(fluent = true)
    private Map<String, Object> additional;
    @Accessors(fluent = true)
    private List<Entity> entities;

    public IntegrationDeviceBuilder(String integrationId) {
        this.integrationId = integrationId;
    }

    public Device build(Class<?> clazz) {
        if (entities == null) {
            entities = new ArrayList<>();
            Object deviceEntities;
            try {
                deviceEntities = clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create instance of " + clazz.getName(), e);
            }

            List<Field> fields = getAllFields(clazz);

            for (Field field : fields) {
                if (field.isAnnotationPresent(EntityField.class)) {
                    EntityField entityField = field.getAnnotation(EntityField.class);
                    Entity entity = createEntity(entityField);
                    entities.add(entity);
                    setFieldValue(deviceEntities, field, entity);
                }
            }
        }
        return new DeviceBuilder(integrationId)
                .name(name)
                .identifier(identifier)
                .additional(additional)
                .entities(entities)
                .build();
    }

    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null) {
            for (Field field : clazz.getDeclaredFields()) {
                fields.add(field);
            }
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    private void setFieldValue(Object deviceEntities, Field field, Entity entity) {
        try {
            field.setAccessible(true);
            field.set(deviceEntities, entity);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private Entity createEntity(EntityField entityField) {
        EntityBuilder builder = new EntityBuilder(integrationId)
                .identifier(entityField.identifier())
                .valueType(entityField.valueType());

        if (!entityField.property().isBlank() && entityField.accessMod() != null) {
            builder.property(entityField.property(), entityField.accessMod());
        }

        if (!entityField.unit().isEmpty()) {
            builder.attributes(new AttributeBuilder().unit(entityField.unit()).build());
        }

        if (entityField.isService()) {
            builder.service(entityField.identifier());
        }

        return builder.build();
    }
}
