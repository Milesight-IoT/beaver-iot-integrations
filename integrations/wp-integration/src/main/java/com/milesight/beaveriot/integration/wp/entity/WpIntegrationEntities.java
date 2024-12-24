package com.milesight.beaveriot.integration.wp.entity;

import com.milesight.beaveriot.context.integration.context.AddDeviceAware;
import com.milesight.beaveriot.context.integration.context.DeleteDeviceAware;
import com.milesight.beaveriot.context.integration.entity.annotation.Attribute;
import com.milesight.beaveriot.context.integration.entity.annotation.Entities;
import com.milesight.beaveriot.context.integration.entity.annotation.Entity;
import com.milesight.beaveriot.context.integration.entity.annotation.IntegrationEntities;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import lombok.*;
import lombok.experimental.FieldNameConstants;

@Data
@EqualsAndHashCode(callSuper = true)
@IntegrationEntities
public class WpIntegrationEntities extends ExchangePayload {

    @Entity(type = EntityType.SERVICE, identifier = "add_device")
    private AddDevice addDevice;

    @Entity(type = EntityType.SERVICE)
    private SyncDevice syncDevice;

    @Entity(type = EntityType.SERVICE, identifier = "delete_device")
    private DeleteDevice deleteDevice;


    @Entity
    private Openapi openapi;

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

    @FieldNameConstants
    @EqualsAndHashCode(callSuper = true)
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Entities
    public static class Openapi extends ExchangePayload {

        @Entity(attributes = {@Attribute(minLength = 1)})
        private String username;

        @Entity(attributes = {@Attribute(minLength = 1)})
        private String password;

    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @Entities
    public static class AddDevice extends ExchangePayload implements AddDeviceAware {

        @Entity(attributes = {@Attribute(min = 1, max = 999)})
        private Integer memberCapacity;

    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @Entities
    public static class DeleteDevice extends ExchangePayload implements DeleteDeviceAware {
    }
  
    public enum DetectStatus {
        STANDBY, DETECTING;
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    @Builder
    @NoArgsConstructor
    @Entities
    public static class SyncDevice extends ExchangePayload {

    }
}
