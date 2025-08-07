package com.milesight.beaveriot.integrations.camthinkaiinference.entity;

import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.context.integration.model.EntityBuilder;
import com.milesight.beaveriot.integrations.camthinkaiinference.constant.Constants;
import lombok.Builder;

import java.text.MessageFormat;

/**
 * author: Luxb
 * create: 2025/6/5 15:01
 **/
@Builder
public class ModelServiceEntityTemplate {
    private String modelId;
    private String name;
    private String description;
    private String engineType;

    public Entity toEntity() {
        return new EntityBuilder(Constants.INTEGRATION_ID)
                .identifier(MessageFormat.format(Constants.IDENTIFIER_MODEL_FORMAT, modelId))
                .service(name)
                .description(description)
                .valueType(EntityValueType.OBJECT)
                .build();
    }

    public String getKey(String key) {
        return getModelPrefixKey() + modelId + "." + key;
    }

    public static String getModelKey(String modelId) {
        return getModelPrefixKey() + modelId;
    }

    public static String getModelPrefixKey() {
        return Constants.INTEGRATION_ID + ".integration.model_";
    }

    public static String getModelIdFromKey(String key) {
        return key.substring(key.indexOf(getModelPrefixKey()) + getModelPrefixKey().length(),  key.lastIndexOf("."));
    }
}
