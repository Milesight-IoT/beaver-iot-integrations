package com.milesight.beaveriot.integrations.mqttdevice.entity;

import com.milesight.beaveriot.context.integration.context.AddDeviceAware;
import com.milesight.beaveriot.context.integration.context.DeleteDeviceAware;
import com.milesight.beaveriot.context.integration.entity.annotation.Attribute;
import com.milesight.beaveriot.context.integration.entity.annotation.Entities;
import com.milesight.beaveriot.context.integration.entity.annotation.Entity;
import com.milesight.beaveriot.context.integration.entity.annotation.IntegrationEntities;
import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.support.IdentifierValidator;
import com.milesight.beaveriot.integrations.mqttdevice.support.DataCenter;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * author: Luxb
 * create: 2025/5/14 14:31
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@IntegrationEntities
public class MqttDeviceIntegrationEntities extends ExchangePayload {
    public static final String TOPIC_MAP_IDENTIFIER = "topic-map";
    public static final String ADD_DEVICE_IDENTIFIER = "add-device";
    public static final String DELETE_DEVICE_IDENTIFIER = "delete-device";
    public static final String ADD_DEVICE_DEVICE_ID_IDENTIFIER = "device_id";
    public static final String ADD_DEVICE_TEMPLATE_IDENTIFIER = "template";
    public static final String ADD_DEVICE_TEMPLATE_KEY = DataCenter.INTEGRATION_ID + ".integration." + ADD_DEVICE_IDENTIFIER + "." + ADD_DEVICE_TEMPLATE_IDENTIFIER;

    @Entity(type = EntityType.PROPERTY, identifier = TOPIC_MAP_IDENTIFIER, accessMod = AccessMod.R, visible = false)
    private String topicMap;

    @Entity(type = EntityType.SERVICE, identifier = ADD_DEVICE_IDENTIFIER, visible = false)
    private AddDevice addDevice;

    @Entity(type = EntityType.SERVICE, identifier = DELETE_DEVICE_IDENTIFIER, visible = false)
    private DeleteDevice deleteDevice;

    @Data
    @EqualsAndHashCode(callSuper = true)
    @Entities
    public static class AddDevice extends ExchangePayload implements AddDeviceAware {
        @Entity(name = "deviceId", identifier = ADD_DEVICE_DEVICE_ID_IDENTIFIER, attributes = @Attribute(format = "REGEX:" + IdentifierValidator.regex, maxLength = 64))
        private String deviceId;

        @Entity(name = "template", identifier = ADD_DEVICE_TEMPLATE_IDENTIFIER, attributes = @Attribute(enumClass = EmptyEnum.class))
        private String template;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @Entities
    public static class DeleteDevice extends ExchangePayload implements DeleteDeviceAware {
    }

    public enum EmptyEnum {
    }
}
