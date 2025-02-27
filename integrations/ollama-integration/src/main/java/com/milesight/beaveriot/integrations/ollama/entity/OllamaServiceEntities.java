package com.milesight.beaveriot.integrations.ollama.entity;

import com.milesight.beaveriot.context.integration.entity.annotation.Attribute;
import com.milesight.beaveriot.context.integration.entity.annotation.Entities;
import com.milesight.beaveriot.context.integration.entity.annotation.Entity;
import com.milesight.beaveriot.context.integration.entity.annotation.IntegrationEntities;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.integrations.ollama.enums.OllamaModel;
import lombok.*;

/**
 * @Author yuanh
 * @Description
 * @Package com.milesight.beaveriot.integrations.ollama.entity
 * @Date 2025/2/7 14:03
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IntegrationEntities
public class OllamaServiceEntities extends ExchangePayload {

    @Entity(type = EntityType.SERVICE, name = "Test connection")
    private TestConnection testConnection;

    @Entity(type = EntityType.SERVICE, name = "Generate a completion")
    private GenerateCompletion generateCompletion;

    @EqualsAndHashCode(callSuper = true)
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Entities
    public static class GenerateCompletion extends ExchangePayload {
        @Entity(attributes = {@Attribute(minLength = 1, enumClass = OllamaModel.class)})
        private String model;
        @Entity(attributes = {@Attribute(minLength = 1, optional = true)})
        private String prompt;
        @Entity(attributes = {@Attribute(minLength = 1, optional = true)})
        private String suffix;
        @Entity(attributes = {@Attribute(minLength = 1, optional = true)})
        private String images;
        @Entity(attributes = {@Attribute(minLength = 1, optional = true)})
        private String format;
        @Entity(attributes = {@Attribute(minLength = 1, optional = true)})
        private String options;
        @Entity(attributes = {@Attribute(minLength = 1, optional = true)})
        private String system;
        @Entity(attributes = {@Attribute(minLength = 1, optional = true)})
        private String template;
        @Entity(attributes = {@Attribute(optional = true)})
        private Boolean row;
        @Entity(attributes = {@Attribute(optional = true)})
        private Integer keepAlive;
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    @Builder
    @Entities
    public static class TestConnection extends ExchangePayload {
    }
}
