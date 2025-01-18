package com.milesight.beaveriot.integrations.myintegration.entity;

import com.milesight.beaveriot.context.integration.entity.annotation.*;
import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@DeviceTemplateEntities(name = "Ping Device")
public class MyDeviceEntities extends ExchangePayload {
    @Entity(type = EntityType.PROPERTY, name = "Device Connection Status", accessMod = AccessMod.R, attributes = @Attribute(enumClass = DeviceStatus.class))
    private Long status;

    public enum DeviceStatus {
        ONLINE, OFFLINE;
    }
}