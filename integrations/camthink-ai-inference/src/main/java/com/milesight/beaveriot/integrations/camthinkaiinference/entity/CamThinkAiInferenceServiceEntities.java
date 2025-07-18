package com.milesight.beaveriot.integrations.camthinkaiinference.entity;

import com.milesight.beaveriot.context.integration.entity.annotation.Entities;
import com.milesight.beaveriot.context.integration.entity.annotation.Entity;
import com.milesight.beaveriot.context.integration.entity.annotation.IntegrationEntities;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * author: Luxb
 * create: 2025/6/5 8:41
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@IntegrationEntities
public class CamThinkAiInferenceServiceEntities extends ExchangePayload {
    @Entity(type = EntityType.SERVICE, name = "Refresh models")
    private RefreshModels refreshModels;

    @Entity(type = EntityType.SERVICE, name = "Draw result image")
    private DrawResultImage drawResultImage;

    @Entities
    public static class RefreshModels extends ExchangePayload {
    }

    public static class ModelInput extends ExchangePayload {
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    @Entities
    public static class DrawResultImage extends ExchangePayload {
        @Entity(type = EntityType.SERVICE, name = "Image base64")
        private String imageBase64;

        @Entity(type = EntityType.SERVICE, name = "Camthink infer response json")
        private String camthinkInferResponseJson;
    }
}