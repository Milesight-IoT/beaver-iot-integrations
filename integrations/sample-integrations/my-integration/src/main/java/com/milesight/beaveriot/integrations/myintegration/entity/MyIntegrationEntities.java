package com.milesight.beaveriot.integrations.myintegration.entity;

import com.milesight.beaveriot.context.integration.context.AddDeviceAware;
import com.milesight.beaveriot.context.integration.context.DeleteDeviceAware;
import com.milesight.beaveriot.context.integration.entity.annotation.Attribute;
import com.milesight.beaveriot.context.integration.entity.annotation.Entities;
import com.milesight.beaveriot.context.integration.entity.annotation.Entity;
import com.milesight.beaveriot.context.integration.entity.annotation.IntegrationEntities;
import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@IntegrationEntities
public class MyIntegrationEntities extends ExchangePayload {
    @Entity(type = EntityType.SERVICE, name = "Device Connection Benchmark", identifier = "benchmark")
    private Benchmark benchmark;

    @Entity(type = EntityType.PROPERTY, name = "Detect Status", identifier = "detect_status", attributes = @Attribute(enumClass = DetectStatus.class), accessMod = AccessMod.R)
    private Long detectStatus;

    @Entity(type = EntityType.EVENT, name = "Detect Report", identifier = "detect_report")
    private DetectReport detectReport;

    @Entity(type = EntityType.SERVICE, identifier = "add_device", visible = false)
    private AddDevice addDevice;

    @Entity(type = EntityType.SERVICE, identifier = "delete_device", visible = false)
    private DeleteDevice deleteDevice;


    @Data
    @EqualsAndHashCode(callSuper = true)
    @Entities
    public static class DetectReport extends ExchangePayload {
        // Entity type inherits from parent entity (DetectReport)
        @Entity
        private Long consumedTime;

        @Entity
        private Long onlineCount;

        @Entity
        private Long offlineCount;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @Entities
    public static class AddDevice extends ExchangePayload implements AddDeviceAware {
        @Entity
        private String ip;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @Entities
    public static class DeleteDevice extends ExchangePayload implements DeleteDeviceAware {
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @Entities
    public static class Benchmark extends ExchangePayload {
    }

    public enum DetectStatus {
        STANDBY, DETECTING;
    }
}
