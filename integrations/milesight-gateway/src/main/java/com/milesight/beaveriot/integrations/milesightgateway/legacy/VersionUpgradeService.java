package com.milesight.beaveriot.integrations.milesightgateway.legacy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.context.api.*;
import com.milesight.beaveriot.context.integration.model.*;
import com.milesight.beaveriot.context.integration.model.config.EntityConfig;
import com.milesight.beaveriot.context.model.DeviceTemplateModel;
import com.milesight.beaveriot.integrations.milesightgateway.model.DeviceModelIdentifier;
import com.milesight.beaveriot.integrations.milesightgateway.model.GatewayDeviceData;
import com.milesight.beaveriot.integrations.milesightgateway.util.Constants;
import com.milesight.beaveriot.integrations.milesightgateway.util.GatewayString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * VersionUpgradeService class.
 * <p><em>Intend to be removed at V2.0.</em></p>
 *
 * @author simon
 * @date 2025/9/19
 */
@Service("milesightGatewayIntegrationVersionUpgradeService")
@Slf4j
public class VersionUpgradeService {
    @Autowired
    EntityServiceProvider entityServiceProvider;

    @Autowired
    EntityValueServiceProvider entityValueServiceProvider;

    @Autowired
    DeviceServiceProvider deviceServiceProvider;

    @Autowired
    EntityTemplateServiceProvider entityTemplateServiceProvider;

    @Autowired
    DeviceTemplateParserProvider deviceTemplateParserProvider;

    private final ObjectMapper json = GatewayString.jsonInstance();

    public void upgrade() {
        if (isNewVersion()) {
            return;
        }

        log.info("Old version milesight gateway integration detected. Upgrading...");

        upgradeDevices();
        cleanIntegrationEntities();
    }

    private boolean isNewVersion() {
        return entityServiceProvider.findByKey(LegacyConstants.DEVICE_MODEL_DATA_KEY) == null;
    }

    private void upgradeDevices() {
        List<Device> allDevices = deviceServiceProvider.findAll(Constants.INTEGRATION_ID);
        allDevices.forEach(device -> {
            if (GatewayString.isGatewayIdentifier(device.getIdentifier())) {
                upgradeGateway(device);
            } else {
                upgradeNode(device);
            }
        });
    }

    private void upgradeGateway(Device gateway) {
        Optional<Entity> statusEntity = gateway.getEntities().stream().filter(entity -> entity.getIdentifier().equals(LegacyConstants.OLD_GATEWAY_STATUS_ENTITY_IDENTIFIER)).findFirst();
        EntityTemplate statusEntityTemplate = entityTemplateServiceProvider.findByKey(LegacyConstants.NEW_GATEWAY_STATUS_ENTITY_IDENTIFIER);
        statusEntity.ifPresent(entity -> {
            String statusValue = (String) entityValueServiceProvider.findValueByKey(entity.getKey());
            entityServiceProvider.deleteByKey(entity.getKey());
            Entity newStatusEntity = statusEntityTemplate.toEntity(Constants.INTEGRATION_ID, gateway.getKey());
            entityServiceProvider.save(newStatusEntity);
            if (statusValue != null && statusValue.equals(Constants.STATUS_ONLINE)) {
                entityValueServiceProvider.saveLatestValues(ExchangePayload.create(Map.of(newStatusEntity.getKey(), Constants.STATUS_ONLINE)));
            } else {
                entityValueServiceProvider.saveLatestValues(ExchangePayload.create(Map.of(newStatusEntity.getKey(), Constants.STATUS_OFFLINE)));
            }
        });
    }

    private void upgradeNode(Device node) {
        Map<String, Object> additionalData = node.getAdditional();
        String modelId = null;
        if (additionalData != null) {
            GatewayDeviceData deviceData = json.convertValue(additionalData, new TypeReference<>() {});
            modelId = deviceData.getDeviceModel(); // am103@milesight-iot
        }

        if (modelId == null) {
            log.warn("Failed to upgrade " + node.getIdentifier() + ", no model id found.");
            return;
        }

        DeviceModelIdentifier modelIdentifier = DeviceModelIdentifier.of(modelId);
        DeviceTemplate deviceTemplate = null;
        try {
            deviceTemplate = deviceTemplateParserProvider.getLatestDeviceTemplate(modelIdentifier.getVendorId(), modelIdentifier.getModelId());
        } catch (ServiceException e) {
            log.warn("Failed to upgrade " + node.getIdentifier() + ", no template found for " + modelId);
            return;
        }

        DeviceTemplateModel deviceTemplateModel = deviceTemplateParserProvider.parse(deviceTemplate.getContent());
        List<EntityConfig> initialEntityConfigList = deviceTemplateModel.getInitialEntities();

        // get new entities
        Set<String> upgradedEntityIdentifiers = new HashSet<>();
        List<Entity> upgradedEntities = initialEntityConfigList.stream().map(entityConfig -> {
            Entity entity = entityConfig.toEntity();
            entity.setIntegrationId(Constants.INTEGRATION_ID);
            entity.setDeviceKey(node.getKey());
            upgradedEntityIdentifiers.add(entity.getIdentifier());
            return entity;
        }).toList();

        // delete old entities
        node.getEntities().forEach(entity -> {
            if (!upgradedEntityIdentifiers.contains(entity.getIdentifier())) {
                entityServiceProvider.deleteByKey(entity.getKey());
            }
        });

        // update new entities
        node.setEntities(upgradedEntities);
        node.setTemplate(deviceTemplate.getKey());
        deviceServiceProvider.save(node);
    }

    private void cleanIntegrationEntities() {
        entityServiceProvider.deleteByKey(LegacyConstants.DEVICE_MODEL_DATA_KEY);
        entityServiceProvider.deleteByKey(LegacyConstants.MODEL_REPO_URL_KEY);
        entityServiceProvider.deleteByKey(LegacyConstants.SYNC_DEVICE_CODEC_KEY);
    }
}
