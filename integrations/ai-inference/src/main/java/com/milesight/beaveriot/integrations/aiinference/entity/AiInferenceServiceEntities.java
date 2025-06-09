package com.milesight.beaveriot.integrations.aiinference.entity;

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
public class AiInferenceServiceEntities extends ExchangePayload {
    @Entity(type = EntityType.SERVICE, name = "Refresh models")
    private RefreshModels refreshModels;

    @Entities
    public static class RefreshModels extends ExchangePayload {
    }

    public static class ModelInput extends ExchangePayload {
    }
}
