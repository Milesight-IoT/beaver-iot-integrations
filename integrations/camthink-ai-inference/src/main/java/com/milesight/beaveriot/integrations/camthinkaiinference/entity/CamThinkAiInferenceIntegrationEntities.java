package com.milesight.beaveriot.integrations.camthinkaiinference.entity;

import com.milesight.beaveriot.context.integration.entity.annotation.Entity;
import com.milesight.beaveriot.context.integration.entity.annotation.IntegrationEntities;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
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
public class CamThinkAiInferenceIntegrationEntities extends ExchangePayload {
    @Entity(type = EntityType.PROPERTY, name = "CamThink Ai inference device image entity map")
    private String deviceImageEntityMap;
}