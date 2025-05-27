package com.milesight.beaveriot.integrations.mqttdevice.entity;

import com.milesight.beaveriot.context.integration.context.*;
import com.milesight.beaveriot.context.integration.entity.annotation.Entities;
import com.milesight.beaveriot.context.integration.entity.annotation.Entity;
import com.milesight.beaveriot.context.integration.entity.annotation.IntegrationEntities;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
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
    public static final String ADD_DEVICE_TEMPLATE_IDENTIFIER = "add-device-template";
    public static final String UPDATE_DEVICE_TEMPLATE_IDENTIFIER = "update-device-template";
    public static final String DELETE_DEVICE_TEMPLATE_IDENTIFIER = "delete-device-template";

    @Entity(type = EntityType.PROPERTY, identifier = TOPIC_MAP_IDENTIFIER, visible = false)
    private String topicMap;

    @Entity(type = EntityType.SERVICE, identifier = ADD_DEVICE_IDENTIFIER, visible = false)
    private AddDevice addDevice;

    @Entity(type = EntityType.SERVICE, identifier = DELETE_DEVICE_IDENTIFIER, visible = false)
    private DeleteDevice deleteDevice;

    @Entity(type = EntityType.SERVICE, identifier = ADD_DEVICE_TEMPLATE_IDENTIFIER, visible = false)
    private AddDeviceTemplate addDeviceTemplate;

    @Entity(type = EntityType.SERVICE, identifier = UPDATE_DEVICE_TEMPLATE_IDENTIFIER, visible = false)
    private UpdateDeviceTemplate updateDeviceTemplate;

    @Entity(type = EntityType.SERVICE, identifier = DELETE_DEVICE_TEMPLATE_IDENTIFIER, visible = false)
    private DeleteDeviceTemplate deleteDeviceTemplate;

    @Data
    @EqualsAndHashCode(callSuper = true)
    @Entities
    public static class AddDevice extends ExchangePayload implements AddDeviceAware {
        @Entity(name = "deviceId", identifier = "device_id")
        private String deviceId;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @Entities
    public static class DeleteDevice extends ExchangePayload implements DeleteDeviceAware {
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @Entities
    public static class AddDeviceTemplate extends ExchangePayload implements AddDeviceTemplateAware {
        @Entity(name = "topic", identifier = "topic")
        private String topic;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @Entities
    public static class UpdateDeviceTemplate extends ExchangePayload implements UpdateDeviceTemplateAware {
        @Entity(name = "topic", identifier = "topic")
        private String topic;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @Entities
    public static class DeleteDeviceTemplate extends ExchangePayload implements DeleteDeviceTemplateAware {
    }
}
