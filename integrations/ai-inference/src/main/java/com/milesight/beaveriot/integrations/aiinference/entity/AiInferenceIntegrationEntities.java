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
public class AiInferenceIntegrationEntities extends ExchangePayload {
    @Entity(type = EntityType.PROPERTY, name = "Ai inference device image entity map")
    private String deviceImageEntityMap;
}