package com.milesight.beaveriot.weather.entity;

import com.milesight.beaveriot.context.integration.context.AddDeviceAware;
import com.milesight.beaveriot.context.integration.context.DeleteDeviceAware;
import com.milesight.beaveriot.context.integration.entity.annotation.Attribute;
import com.milesight.beaveriot.context.integration.entity.annotation.Entities;
import com.milesight.beaveriot.context.integration.entity.annotation.Entity;
import com.milesight.beaveriot.context.integration.entity.annotation.IntegrationEntities;
import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.weather.enums.CityEnum;
import com.milesight.beaveriot.weather.enums.LangEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@IntegrationEntities
public class WeatherIntegrationEntities extends ExchangePayload {
    @Entity(type = EntityType.PROPERTY, name = "Detect Status", identifier = "detect_status", attributes = @Attribute(enumClass = DetectStatus.class), accessMod = AccessMod.R)
    // highlight-next-line
    private Integer detectStatus;
    @Entity(type = EntityType.PROPERTY, name = "Api Info", identifier = "api_info", accessMod = AccessMod.RW)
    private ApiInfo apiInfo;
    @Entity(type = EntityType.SERVICE, name = "Get Weather Info", identifier = "benchmark")
    // highlight-next-line
    private String benchmark;

    @Entity(type = EntityType.SERVICE, name = "Add Weather Device", identifier = "add_device")
    // highlight-next-line
    private AddDevice addDevice;

    @Entity(type = EntityType.SERVICE, name = "Delete Weather Device", identifier = "delete_device")
    // highlight-next-line
    private DeleteDevice deleteDevice;


    @Data
    @EqualsAndHashCode(callSuper = true)
    @Entities
    public static class AddDevice extends ExchangePayload implements AddDeviceAware {
        @Entity(attributes = @Attribute(enumClass = CityEnum.class))
        private String city;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @Entities
    public static class DeleteDevice extends ExchangePayload implements DeleteDeviceAware {
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @Entities
    public static class ApiInfo extends ExchangePayload {
        @Entity(type = EntityType.PROPERTY, name = "和风天气接口地址", identifier = "api_url", accessMod = AccessMod.RW)
        private String apiUrl;

        @Entity(type = EntityType.PROPERTY, name = "和风天气秘钥", identifier = "api_key", accessMod = AccessMod.RW)
        private String apiKey;

        @Entity(type = EntityType.PROPERTY, name = "天气语言", identifier = "api_lang", attributes = @Attribute(enumClass = LangEnum.class), accessMod = AccessMod.RW)
        private String apiLang;
    }

    public enum DetectStatus {
        STANDBY, DETECTING;
    }
}
