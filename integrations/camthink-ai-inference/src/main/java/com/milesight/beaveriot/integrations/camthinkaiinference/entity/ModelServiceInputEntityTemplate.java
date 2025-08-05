package com.milesight.beaveriot.integrations.camthinkaiinference.entity;

import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.context.integration.model.AttributeBuilder;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.context.integration.model.EntityBuilder;
import com.milesight.beaveriot.integrations.camthinkaiinference.constant.Constants;
import lombok.Builder;

import java.util.HashMap;
import java.util.Map;

/**
 * author: Luxb
 * create: 2025/6/5 15:01
 **/
@Builder
public class ModelServiceInputEntityTemplate {
    private String parentIdentifier;
    private String name;
    private String type;
    private String description;
    private boolean required;
    private String format;
    private String defaultValue;
    private Double minimum;
    private Double maximum;

    public Entity toEntity() {
        EntityValueType valueType = convertType();
        Map<String, Object> attributes = buildAttributes();

        return new EntityBuilder(Constants.INTEGRATION_ID)
                .identifier(name)
                .parentIdentifier(parentIdentifier)
                .service(name)
                .description(description)
                .valueType(valueType)
                .attributes(attributes)
                .build();
    }

    private EntityValueType convertType() {
        EntityValueType valueType;
        if ("float".equals(type)) {
            valueType = EntityValueType.DOUBLE;
        } else if ("integer".equals(type)) {
            valueType = EntityValueType.LONG;
        } else if ("boolean".equals(type)) {
            valueType = EntityValueType.BOOLEAN;
        } else {
            valueType = EntityValueType.STRING;
        }
        return valueType;
    }

    private String convertFormat() {
        if (format.contains("uri") && format.contains("base64")) {
            return Constants.ATTRIBUTE_FORMAT_IMAGE;
        } else if (format.contains("uri")) {
            return Constants.ATTRIBUTE_FORMAT_IMAGE_URL;
        } else if (format.contains("base64")) {
            return Constants.ATTRIBUTE_FORMAT_IMAGE_BASE64;
        } else {
            return format;
        }
    }

    private Map<String, Object> buildAttributes() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(AttributeBuilder.ATTRIBUTE_OPTIONAL, !required);
        if (format != null) {
            String convertFormat = convertFormat();
            attributes.put(AttributeBuilder.ATTRIBUTE_FORMAT, convertFormat);
        }
        if (defaultValue != null) {
            attributes.put(AttributeBuilder.ATTRIBUTE_DEFAULT_VALUE, defaultValue);
        }
        if (minimum != null) {
            attributes.put(AttributeBuilder.ATTRIBUTE_MIN, minimum);
        }
        if (maximum != null) {
            attributes.put(AttributeBuilder.ATTRIBUTE_MAX, maximum);
        }
        return attributes;
    }

    public static String getModelInputNameFromKey(String key) {
        return key.substring(key.lastIndexOf(".") + 1);
    }
}
