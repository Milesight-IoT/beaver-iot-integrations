package com.milesight.beaveriot.integrations.mqttdevice.model.response;

import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.context.integration.model.config.EntityConfig;
import com.milesight.beaveriot.context.model.DeviceTemplateModel;
import com.milesight.beaveriot.context.model.response.DeviceTemplateResponseData;
import com.milesight.beaveriot.integrations.mqttdevice.support.DataCenter;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * author: Luxb
 * create: 2025/6/16 8:49
 **/
@EqualsAndHashCode(callSuper = true)
@Setter
@Getter
public class DeviceTemplateDetailResponse extends DeviceTemplateInfoResponse {
    private DeviceTemplateModel.Definition.Input inputSchema;
    private DeviceTemplateModel.Definition.Output outputSchema;
    private List<Entity> entitySchema;

    protected DeviceTemplateDetailResponse(DeviceTemplateResponseData deviceTemplateResponseData, String topic,
                                           DeviceTemplateModel.Definition.Input inputSchema,
                                           DeviceTemplateModel.Definition.Output outputSchema,
                                           List<EntityConfig> entityConfigs) {
        super(deviceTemplateResponseData, topic);
        this.inputSchema = inputSchema;
        this.outputSchema = outputSchema;
        if (entityConfigs != null) {
            List<Entity> entities = entityConfigs.stream().map(entityConfig -> {
                Entity entity = entityConfig.toEntity();
                entity.setIntegrationId(DataCenter.INTEGRATION_ID);
                entity.setDeviceKey(DataCenter.DEFAULT_DEVICE_KEY);
                return entity;
            }).toList();
            entities.forEach(entity -> entity.initializeProperties(DataCenter.INTEGRATION_ID, DataCenter.DEFAULT_DEVICE_KEY));
            this.entitySchema = entities;
        }
    }

    public static DeviceTemplateDetailResponse build(DeviceTemplateResponseData deviceTemplateResponseData, String topic,
                                                     DeviceTemplateModel.Definition.Input inputSchema,
                                                     DeviceTemplateModel.Definition.Output outputSchema,
                                                     List<EntityConfig> entityConfigs) {
        return new DeviceTemplateDetailResponse(deviceTemplateResponseData, topic, inputSchema, outputSchema, entityConfigs);
    }
}
