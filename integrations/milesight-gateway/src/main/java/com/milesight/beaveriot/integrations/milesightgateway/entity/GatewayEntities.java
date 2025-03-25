package com.milesight.beaveriot.integrations.milesightgateway.entity;

import com.milesight.beaveriot.context.integration.entity.annotation.Attribute;
import com.milesight.beaveriot.context.integration.entity.annotation.DeviceTemplateEntities;
import com.milesight.beaveriot.context.integration.entity.annotation.Entity;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.integrations.milesightgateway.model.DeviceConnectStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * GatewayEntities class.
 *
 * @author simon
 * @date 2025/3/7
 */
@Data
@EqualsAndHashCode(callSuper = true)
@DeviceTemplateEntities(name = "Milesight Gateway")
public class GatewayEntities extends ExchangePayload {
    public static final String STATUS_KEY = "status";

    @Entity(name = "Status", identifier = STATUS_KEY, attributes = @Attribute(enumClass = DeviceConnectStatus.class))
    private String status;
}
