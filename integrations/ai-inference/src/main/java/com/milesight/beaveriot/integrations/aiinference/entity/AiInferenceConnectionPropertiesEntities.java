package com.milesight.beaveriot.integrations.aiinference.entity;

import com.milesight.beaveriot.base.utils.StringUtils;
import com.milesight.beaveriot.context.integration.entity.annotation.Attribute;
import com.milesight.beaveriot.context.integration.entity.annotation.Entities;
import com.milesight.beaveriot.context.integration.entity.annotation.Entity;
import com.milesight.beaveriot.context.integration.entity.annotation.IntegrationEntities;
import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.integrations.aiinference.constant.Constants;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;

/**
 * author: Luxb
 * create: 2025/5/14 14:31
 **/
@FieldNameConstants
@Data
@EqualsAndHashCode(callSuper = true)
@IntegrationEntities
public class AiInferenceConnectionPropertiesEntities extends ExchangePayload {
    @Entity(type = EntityType.PROPERTY, name = "Ai inference properties")
    private AiInferenceProperties aiInferenceProperties;

    @Entity(type = EntityType.PROPERTY, name = "Ai inference api status", accessMod = AccessMod.R)
    private Boolean apiStatus;

    public static String getKey(String propertyKey) {
        return Constants.INTEGRATION_ID + ".integration." + StringUtils.toSnakeCase(propertyKey);
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @Entities
    public static class AiInferenceProperties extends ExchangePayload {
        @Entity(type = EntityType.PROPERTY, name = "Ai inference base url", accessMod = AccessMod.RW, attributes = {@Attribute(minLength = 1)})
        private String baseUrl;

        @Entity(type = EntityType.PROPERTY, name = "Ai inference token", accessMod = AccessMod.RW, attributes = {@Attribute(minLength = 1)})
        private String token;
    }
}