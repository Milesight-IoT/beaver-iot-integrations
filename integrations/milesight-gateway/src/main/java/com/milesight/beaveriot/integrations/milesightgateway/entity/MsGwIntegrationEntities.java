package com.milesight.beaveriot.integrations.milesightgateway.entity;

import com.milesight.beaveriot.base.enums.EnumCode;
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

    public static final String GATEWAY_DEVICE_RELATION_IDENTIFIER = "gateway-device-relation";

    public static final String GATEWAY_DEVICE_RELATION_KEY = Constants.INTEGRATION_ID + ".integration." + GATEWAY_DEVICE_RELATION_IDENTIFIER;

    @Entity(type = EntityType.SERVICE, name = "Add Device", identifier = ADD_DEVICE_IDENTIFIER, visible = false)
    private AddDevice addDevice;

    @Entity(type = EntityType.SERVICE, name = "Delete Device", identifier = "delete-device", visible = false)
    private DeleteDevice deleteDevice;

    @Entity(type = EntityType.EVENT, name = "Gateway Status Event", identifier = "gateway-status-event")
    private GatewayStatusEvent gatewayStatusEvent;

    @Entity(type = EntityType.PROPERTY, name = "Gateway Device Relation", identifier = GATEWAY_DEVICE_RELATION_IDENTIFIER, accessMod = AccessMod.R, visible = false)
    private String gatewayDeviceRelation;

    @Data
    @EqualsAndHashCode(callSuper = true)
    @Entities
    public static class AddDevice extends ExchangePayload implements AddDeviceAware {
        @Entity(name = "DevEUI", attributes = @Attribute(lengthRange = "16", format = "HEX"))
        private String eui;

        @Entity(name = "Model", identifier = ADD_DEVICE_GATEWAY_DEVICE_MODEL_IDENTIFIER, attributes = @Attribute(enumClass = EmptyEnum.class))
        private String deviceModel;

        @Entity(name = "Gateway", identifier = ADD_DEVICE_GATEWAY_EUI_IDENTIFIER, attributes = @Attribute(enumClass = EmptyEnum.class))
        private String gatewayEUI;

        @Entity(name = "fPort", identifier = "fport", attributes = @Attribute(max = 223, min = 1, defaultValue = "1"))
        private Long fPort;

        @Entity(name = "Frame-counter Validation", identifier = "frame-counter-validation")
        private Boolean frameCounterValidation;

        @Entity(name = "Application Key", identifier = "app-key", attributes = @Attribute(optional = true, lengthRange = "32", format = "HEX"))
        private String appKey;

        @Entity(name = "Offline Timeout", identifier = "offline-timeout", attributes = @Attribute(
                min = Constants.OFFLINE_TIMEOUT_ENTITY_MIN_VALUE,
                defaultValue = Constants.DEFAULT_DEVICE_OFFLINE_TIMEOUT_STR,
                unit = Constants.OFFLINE_TIMEOUT_ENTITY_UNIT
        ))
        private Long offlineTimeout;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @Entities
    public static class DeleteDevice extends ExchangePayload implements DeleteDeviceAware {}

    @Data
    @EqualsAndHashCode(callSuper = true)
    @Entities
    public static class GatewayStatusEvent extends ExchangePayload {
        public enum GatewayStatus {
            ONLINE, OFFLINE;
        }

        @Entity(name = "Device EUI")
        private String eui;

        @Entity(name = "Gateway Name", identifier = "gateway-name")
        private String gatewayName;

        @Entity(name = "Gateway Status", attributes = @Attribute(enumClass = GatewayStatus.class))
        private String status;

        @Entity(name = "Status Timestamp")
        private Long statusTimestamp;
    }

    public enum EmptyEnum {
    }
}
