package com.milesight.beaveriot.integrations.mqttdevice.entity;

import com.milesight.beaveriot.context.integration.context.AddDeviceAware;
import com.milesight.beaveriot.context.integration.context.DeleteDeviceAware;
import com.milesight.beaveriot.context.integration.entity.annotation.Entities;
import com.milesight.beaveriot.context.integration.entity.annotation.Entity;
import com.milesight.beaveriot.context.integration.entity.annotation.IntegrationEntities;
import com.milesight.beaveriot.context.integration.enums.AccessMod;
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

    @Entity(type = EntityType.PROPERTY, name = "Mqtt device properties", accessMod = AccessMod.R)
    private MqttDeviceProperties mqttDeviceProperties;

    @Entity(type = EntityType.PROPERTY, identifier = TOPIC_MAP_IDENTIFIER, accessMod = AccessMod.R, visible = false)
    private String topicMap;

    @Entity(type = EntityType.SERVICE, identifier = ADD_DEVICE_IDENTIFIER, visible = false)
    private AddDevice addDevice;

    @Entity(type = EntityType.SERVICE, identifier = DELETE_DEVICE_IDENTIFIER, visible = false)
    private DeleteDevice deleteDevice;

    @Data
    @EqualsAndHashCode(callSuper = true)
    @Entities
    public static class MqttDeviceProperties extends ExchangePayload {
        @Entity(type = EntityType.PROPERTY, name = "Mqtt device broker properties initialized flag", visible = false)
        private Boolean isInitialized;

        @Entity(type = EntityType.PROPERTY, name = "Mqtt device broker server")
        private String brokerServer;

        @Entity(type = EntityType.PROPERTY, name = "Mqtt device broker port")
        private Integer brokerPort;

        @Entity(type = EntityType.PROPERTY, name = "Mqtt device broker username")
        private String brokerUsername;

        @Entity(type = EntityType.PROPERTY, name = "Mqtt device broker password")
        private String brokerPassword;

        @Entity(type = EntityType.PROPERTY, name = "Mqtt device broker topic prefix")
        private String brokerTopicPrefix;
    }

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
}
