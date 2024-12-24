package com.milesight.beaveriot.integration.aws.entity;

import com.milesight.beaveriot.context.integration.entity.annotation.Attribute;
import com.milesight.beaveriot.context.integration.entity.annotation.Entities;
import com.milesight.beaveriot.context.integration.entity.annotation.Entity;
import com.milesight.beaveriot.context.integration.entity.annotation.IntegrationEntities;
import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.integration.aws.sdk.enums.LoraClass;
import com.milesight.beaveriot.integration.aws.sdk.enums.RfRegion;
import lombok.*;
import lombok.experimental.FieldNameConstants;

@EqualsAndHashCode(callSuper = false)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IntegrationEntities
public class AwsServiceEntities extends ExchangePayload {

    @Entity(type = EntityType.SERVICE)
    private AddDevice addDevice;

    @Entity(type = EntityType.SERVICE)
    private SyncDevice syncDevice;

    @Entity(type = EntityType.SERVICE)
    private DeleteDevice deleteDevice;

    @EqualsAndHashCode(callSuper = false)
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Entities
    @FieldNameConstants
    public static class AddDevice extends ExchangePayload {

        @Entity(attributes = {@Attribute(minLength = 12, maxLength = 16)})
        private String sn;

        @Entity(attributes = {@Attribute(minLength = 1)})
        private String devEui;

        @Entity(attributes = {@Attribute(minLength = 1)})
        private String devAppKey;

        @Entity(attributes = @Attribute(enumClass = RfRegion.class))
        private String rfRegion;

        @Entity(attributes = @Attribute(enumClass = LoraClass.class))
        private String loraClass;
    }



    @EqualsAndHashCode(callSuper = true)
    @Data
    @Builder
    @NoArgsConstructor
    @Entities
    public static class DeleteDevice extends ExchangePayload {

    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    @Builder
    @NoArgsConstructor
    @Entities
    public static class SyncDevice extends ExchangePayload {

    }

}
