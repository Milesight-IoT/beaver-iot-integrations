package com.milesight.beaveriot.integrations.ollama.entity;

import com.milesight.beaveriot.base.utils.StringUtils;
import com.milesight.beaveriot.context.integration.entity.annotation.Attribute;
import com.milesight.beaveriot.context.integration.entity.annotation.Entities;
import com.milesight.beaveriot.context.integration.entity.annotation.Entity;
import com.milesight.beaveriot.context.integration.entity.annotation.IntegrationEntities;
import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.integrations.ollama.constant.OllamaIntegrationConstants;
import lombok.*;
import lombok.experimental.FieldNameConstants;

@FieldNameConstants
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IntegrationEntities
public class OllamaConnectionPropertiesEntities extends ExchangePayload {

    public static String getKey(String propertyKey) {
        return OllamaIntegrationConstants.INTEGRATION_IDENTIFIER + ".integration." + StringUtils.toSnakeCase(propertyKey);
    }

    @Entity(type = EntityType.PROPERTY, name = "Ollama Properties")
    private OllamaProperties ollamaProperties;


    @Entity(type = EntityType.PROPERTY, name = "Ollama api status", accessMod = AccessMod.R)
    private Boolean apiStatus;

    @Entity(type = EntityType.PROPERTY, name = "Ollama models", accessMod = AccessMod.R)
    private String models;


    @FieldNameConstants
    @EqualsAndHashCode(callSuper = true)
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Entities
    public static class OllamaProperties extends ExchangePayload {

        @Entity(type = EntityType.PROPERTY, name = "Ollama base url", accessMod = AccessMod.RW, attributes = {@Attribute(minLength = 1)})
        private String baseUrl;

    }

}
