package com.milesight.beaveriot.vms.entity;

import com.milesight.beaveriot.context.integration.context.AddDeviceAware;
import com.milesight.beaveriot.context.integration.context.DeleteDeviceAware;
import com.milesight.beaveriot.context.integration.entity.annotation.Attribute;
import com.milesight.beaveriot.context.integration.entity.annotation.Entities;
import com.milesight.beaveriot.context.integration.entity.annotation.Entity;
import com.milesight.beaveriot.context.integration.entity.annotation.IntegrationEntities;
import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.vms.constants.VmsConstants;
import com.milesight.beaveriot.vms.enums.ObjectDetectionModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@IntegrationEntities
public class VmsIntegrationEntities extends ExchangePayload {
    @Entity(type = EntityType.PROPERTY, name = "Detect Status", identifier = VmsConstants.Entity.DETECT_STATUS, attributes = @Attribute(enumClass = DetectStatus.class), accessMod = AccessMod.R)
    private Integer detectStatus;

    @Entity(type = EntityType.PROPERTY, name = "VMS Info", identifier = VmsConstants.Entity.VMS_INFO, accessMod = AccessMod.RW)
    private VmsInfo vmsInfo;

    @Entity(type = EntityType.SERVICE, name = "Sync VMS Devices", identifier = VmsConstants.Entity.BENCHMARK)
    private String benchmark;

    @Entity(type = EntityType.SERVICE, name = "Add VMS Device", identifier = "add_device")
    private AddDevice addDevice;

    @Entity(type = EntityType.SERVICE, name = "Delete Weather Device", identifier = "delete_device")
    // highlight-next-line
    private DeleteDevice deleteDevice;

    @Data
    @EqualsAndHashCode(callSuper = true)
    @Entities
    public static class VmsInfo extends ExchangePayload {
        @Entity(type = EntityType.PROPERTY, name = "VMS地址", identifier = VmsConstants.Entity.VMS_URL, accessMod = AccessMod.RW)
        private String vmsUrl;

        @Entity(type = EntityType.PROPERTY, name = "VMS用户名", identifier = VmsConstants.Entity.USERNAME, accessMod = AccessMod.RW)
        private String username;

        @Entity(type = EntityType.PROPERTY, name = "VMS密码", identifier = VmsConstants.Entity.PASSWORD, accessMod = AccessMod.RW)
        private String password;

        @Entity(type = EntityType.PROPERTY, name = "AI平台分析地址", identifier = VmsConstants.Entity.AI_URL, accessMod = AccessMod.RW)
        private String aiUrl;

        @Entity(type = EntityType.PROPERTY, name = "AI平台授权秘钥", identifier = VmsConstants.Entity.AI_ACCESS_TOKEN, accessMod = AccessMod.RW)
        private String aiAccessToken;

        @Entity(type = EntityType.PROPERTY, name = "AI平台检测模型", identifier = VmsConstants.Entity.OBJECT_DETECTION_MODEL, attributes = @Attribute(enumClass = ObjectDetectionModel.class), accessMod = AccessMod.RW)
        private String objectDetectionModel;

        @Entity(type = EntityType.PROPERTY, name = "AI平台检测标签", identifier = VmsConstants.Entity.IMAGE_LABELS, accessMod = AccessMod.RW)
        private String imageLabels;

        @Entity(type = EntityType.PROPERTY, name = "AI平台置信度", identifier = VmsConstants.Entity.IMAGE_SCORE, accessMod = AccessMod.RW)
        private String imageScore;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @Entities
    public static class AddDevice extends ExchangePayload implements AddDeviceAware {
        @Entity(type = EntityType.PROPERTY, name = "设备ID", identifier = VmsConstants.Entity.DEVICE_ID, accessMod = AccessMod.RW)
        private String deviceId;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @Entities
    public static class DeleteDevice extends ExchangePayload implements DeleteDeviceAware {
    }

    public enum DetectStatus {
        STANDBY, DETECTING;
    }
}
