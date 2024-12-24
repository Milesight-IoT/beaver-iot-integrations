package com.milesight.beaveriot.gateway;

import cn.hutool.core.map.MapUtil;
import com.milesight.beaveriot.base.utils.StringUtils;
import com.milesight.beaveriot.context.integration.entity.annotation.Attribute;
import com.milesight.beaveriot.context.integration.entity.annotation.Entities;
import com.milesight.beaveriot.context.integration.entity.annotation.Entity;
import com.milesight.beaveriot.context.integration.entity.annotation.IntegrationEntities;
import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import lombok.*;
import lombok.experimental.FieldNameConstants;

@Data
@EqualsAndHashCode(callSuper = true)
@IntegrationEntities
@FieldNameConstants
public class GatewayIntegrationEntities extends ExchangePayload {

    public static String getKey(String propertyKey) {
        return GatewayConstants.INTEGRATION_ID + ".integration." + StringUtils.toSnakeCase(propertyKey);
    }

    @Entity(type = EntityType.SERVICE, name = "Device Connection Benchmark", identifier = "benchmark")
    private String benchmark;

    @Entity
    private Gateway gateway;

    @Entity
    private ScheduledDataFetch scheduledDataFetch;

    @Entity(type = EntityType.SERVICE, identifier = "add_device")
    private AddDevice addDevice;

    @Entity(type = EntityType.SERVICE, identifier = "delete_device")
    private String deleteDevice;

    @Data
    @EqualsAndHashCode(callSuper = true)
    @Entities
    @FieldNameConstants
    public static class AddDevice extends ExchangePayload {
        @Entity(attributes = {@Attribute(minLength = 1)})
        private String devEUI;
        /*@Entity
        private String name;*/
        @Entity(accessMod = AccessMod.R, attributes = {@Attribute(enumClass = DefaultApplicationName.class)})
        private String applicationName;
        /*@Entity
        private String description;
        @Entity
        private String profileID;
        @Entity
        private Integer fCntUp;
        @Entity
        private Integer fCntDown;
        @Entity
        private Boolean skipFCntCheck;
        @Entity
        private String appKey;*/
        @Entity(attributes = {@Attribute(minLength = 1)})
        private String devAddr;
        /*@Entity
        private String appSKey;
        @Entity
        private String nwkSKey;
        @Entity
        private String mbMode;
        @Entity
        private String mbFramePort;
        @Entity
        private String mbTCPPort;
        @Entity
        private String fPort;*/
        @Entity(accessMod = AccessMod.R)
        private String payloadCodecID;
    }

    @FieldNameConstants
    @EqualsAndHashCode(callSuper = true)
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Entities
    public static class Gateway extends ExchangePayload {

        @Entity(attributes = {@Attribute(minLength = 1)})
        private String gatewayIP;

        @Entity(attributes = {@Attribute(minLength = 1)})
        private String username;

        @Entity(attributes = {@Attribute(minLength = 1)})
        private String password;

        @Entity
        private String webhookUrl;

    }

    @FieldNameConstants
    @EqualsAndHashCode(callSuper = true)
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Entities
    public static class ScheduledDataFetch extends ExchangePayload {

        @Entity
        private Boolean enabled;

        @Entity(attributes = {@Attribute(min = 30, max = 86400)})
        private Integer period;

    }

    @Getter
    @RequiredArgsConstructor
    public enum DefaultApplicationName {
        JYX_TEST("JYX_TEST");

        private String value;

        DefaultApplicationName(String value) {
            this.value = value;
        }
    }
}
