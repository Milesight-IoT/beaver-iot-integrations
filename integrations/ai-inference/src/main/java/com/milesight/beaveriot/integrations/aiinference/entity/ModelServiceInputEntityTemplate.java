package com.milesight.beaveriot.integrations.aiinference.entity;

import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.context.integration.model.EntityBuilder;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.integrations.aiinference.constant.Constants;
import lombok.Builder;

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

    public Entity toEntity() {
        EntityValueType valueType = EntityValueType.STRING;
        String formatValue = "";
        if ("image_base64".equals(type)) {
            formatValue = "IMAGE:BASE64";
        } else if ("image_url".equals(type)) {
            formatValue = "IMAGE:URL";
        } else if ("float".equals(type)) {
            valueType = EntityValueType.DOUBLE;
        }
        return new EntityBuilder(Constants.INTEGRATION_ID)
                .identifier(name)
                .parentIdentifier(parentIdentifier)
                .service(name)
                .description(description)
                .valueType(valueType)
                .attributes(Map.of(
                        "optional", !required,
                        "format", formatValue
                ))
                .build();
    }

    public static String getModelInputNameFromKey(String key) {
        return key.substring(key.lastIndexOf(".") + 1);
    }
}
