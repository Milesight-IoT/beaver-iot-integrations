package com.milesight.beaveriot.integrations.milesightgateway.entity;

import com.milesight.beaveriot.context.integration.context.AddDeviceAware;
import com.milesight.beaveriot.context.integration.context.DeleteDeviceAware;
import com.milesight.beaveriot.context.integration.entity.annotation.Attribute;
import com.milesight.beaveriot.context.integration.entity.annotation.Entities;
import com.milesight.beaveriot.context.integration.entity.annotation.Entity;
import com.milesight.beaveriot.context.integration.entity.annotation.IntegrationEntities;
import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.integrations.milesightgateway.util.Constants;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * MilesightGatewayEntities class.
 *
 * @author simon
 * @date 2025/2/12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@IntegrationEntities
public class MsGwIntegrationEntities extends ExchangePayload {
    public static final String ADD_DEVICE_IDENTIFIER = "add-device";

    public static final String ADD_DEVICE_GATEWAY_EUI_IDENTIFIER = "gateway-eui";

    public static final String ADD_DEVICE_GATEWAY_EUI_KEY = Constants.INTEGRATION_ID + ".integration." + ADD_DEVICE_IDENTIFIER + "." + ADD_DEVICE_GATEWAY_EUI_IDENTIFIER;

    public static final String ADD_DEVICE_GATEWAY_DEVICE_MODEL_IDENTIFIER = "device-model";

    public static final String ADD_DEVICE_GATEWAY_DEVICE_MODEL_KEY = Constants.INTEGRATION_ID + ".integration." + ADD_DEVICE_IDENTIFIER + "." + ADD_DEVICE_GATEWAY_DEVICE_MODEL_IDENTIFIER;

    public static final String SYNC_DEVICE_CODEC_IDENTIFIER = "sync-device-codec";

    public static final String SYNC_DEVICE_CODEC_KEY = Constants.INTEGRATION_ID + ".integration." + SYNC_DEVICE_CODEC_IDENTIFIER;

    @Entity(type = EntityType.SERVICE, name = "Synchronize Device Codec", identifier = SYNC_DEVICE_CODEC_IDENTIFIER)
    private EmptyPayload syncDeviceCodec;

    @Entity(type = EntityType.SERVICE, name = "Add Device", identifier = ADD_DEVICE_IDENTIFIER, visible = false)
    private AddDevice addDevice;

    @Entity(type = EntityType.SERVICE, name = "Delete Device", identifier = "delete-device", visible = false)
    private DeleteDevice deleteDevice;

    @Entity(type = EntityType.PROPERTY, name = "Gateway Device Relation", identifier = "gateway-device-relation", accessMod = AccessMod.R, visible = false)
    private String gatewayDeviceRelation;

    @Entity(type = EntityType.PROPERTY, name = "Device Model Data", identifier = "device-model-data", accessMod = AccessMod.R, visible = false)
    private String deviceModelData;

    @Entity(type = EntityType.PROPERTY, name = "Model Repository Url", identifier = "model-repo-url", accessMod = AccessMod.RW, attributes = @Attribute(optional = true))
    private String modelRepoUrl;

    @Data
    @EqualsAndHashCode(callSuper = true)
    @Entities
    public static class AddDevice extends ExchangePayload implements AddDeviceAware {
        @Entity(name = "Device EUI", attributes = @Attribute(maxLength = 16, minLength = 16))
        private String eui;

        @Entity(name = "Device Model", identifier = ADD_DEVICE_GATEWAY_DEVICE_MODEL_IDENTIFIER, attributes = @Attribute(enumClass = EmptyEnum.class))
        private String deviceModel;

        @Entity(name = "Gateway", identifier = ADD_DEVICE_GATEWAY_EUI_IDENTIFIER, attributes = @Attribute(enumClass = EmptyEnum.class))
        private String gatewayEUI;

        @Entity(identifier = "fport")
        private Long fPort;

        @Entity(name = "Frame-counter Validation", identifier = "frame-counter-validation")
        private Boolean frameCounterValidation;

        @Entity(name = "Application Key", identifier = "app-key", attributes = @Attribute(optional = true))
        private String appKey;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @Entities
    public static class EmptyPayload extends ExchangePayload {}

    @Data
    @EqualsAndHashCode(callSuper = true)
    @Entities
    public static class DeleteDevice extends ExchangePayload implements DeleteDeviceAware {}

    public enum EmptyEnum {
    }
}
