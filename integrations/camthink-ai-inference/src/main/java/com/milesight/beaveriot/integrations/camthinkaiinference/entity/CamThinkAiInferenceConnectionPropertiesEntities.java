package com.milesight.beaveriot.integrations.camthinkaiinference.entity;

import com.milesight.beaveriot.base.utils.StringUtils;
import com.milesight.beaveriot.context.integration.entity.annotation.Attribute;
import com.milesight.beaveriot.context.integration.entity.annotation.Entities;
import com.milesight.beaveriot.context.integration.entity.annotation.Entity;
import com.milesight.beaveriot.context.integration.entity.annotation.IntegrationEntities;
import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.integrations.camthinkaiinference.constant.Constants;
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
public class CamThinkAiInferenceConnectionPropertiesEntities extends ExchangePayload {
    @Entity(type = EntityType.PROPERTY, name = "CamThink Ai inference properties", visible = false)
    private CamThinkAiInferenceProperties camthinkAiInferenceProperties;

    @Entity(type = EntityType.PROPERTY, name = "CamThink Ai inference api status", accessMod = AccessMod.R, visible = false)
    private Boolean apiStatus;

    public static String getKey(String propertyKey) {
        return Constants.INTEGRATION_ID + ".integration." + StringUtils.toSnakeCase(propertyKey);
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @Entities
    public static class CamThinkAiInferenceProperties extends ExchangePayload {
        @Entity(type = EntityType.PROPERTY, name = "CamThink Ai inference base url", accessMod = AccessMod.RW, attributes = {@Attribute(minLength = 1)})
        private String baseUrl;

        @Entity(type = EntityType.PROPERTY, name = "CamThink Ai inference token", accessMod = AccessMod.RW, attributes = {@Attribute(minLength = 1)})
        private String token;
    }
}