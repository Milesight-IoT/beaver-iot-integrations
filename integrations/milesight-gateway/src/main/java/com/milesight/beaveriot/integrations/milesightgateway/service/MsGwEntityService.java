package com.milesight.beaveriot.integrations.milesightgateway.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.integration.wrapper.AnnotatedEntityWrapper;
import com.milesight.beaveriot.integrations.milesightgateway.codec.ResourceConstant;
import com.milesight.beaveriot.integrations.milesightgateway.entity.MsGwIntegrationEntities;
import com.milesight.beaveriot.integrations.milesightgateway.model.DeviceConnectStatus;
import com.milesight.beaveriot.integrations.milesightgateway.model.DeviceModelData;
import com.milesight.beaveriot.integrations.milesightgateway.util.GatewayString;
import com.milesight.beaveriot.integrations.milesightgateway.codec.ResourceString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * MsGwEntityService class.
 *
 * @author simon
 * @date 2025/3/5
 */
@Component("milesightGatewayEntityService")
@Slf4j
public class MsGwEntityService {
    ObjectMapper json = GatewayString.jsonInstance();

    @Autowired
    EntityValueServiceProvider entityValueServiceProvider;

    /**
     * gateway mapping to devices
     */
    public Map<String, List<String>> getGatewayRelation() {
        try {
            AnnotatedEntityWrapper<MsGwIntegrationEntities> gatewayEntitiesWrapper = new AnnotatedEntityWrapper<>();
            String gatewayListStr = (String) gatewayEntitiesWrapper.getValue(MsGwIntegrationEntities::getGatewayDeviceRelation).orElse("{}");
            return GatewayString.jsonInstance().readValue(gatewayListStr, new TypeReference<>() {});
        } catch (Exception e) {
            throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), "Broken gateway list data").build();
        }
    }

    /**
     * device mapping to gateway
     */
    public Map<String, String> getDeviceGatewayRelation() {
        return getGatewayRelation().entrySet().stream()
                .flatMap(entry -> entry.getValue().stream().map(device -> Map.entry(device, entry.getKey())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public String getDeviceModelRepoUrl() {
        Optional<Object> repoUrl = new AnnotatedEntityWrapper<MsGwIntegrationEntities>().getValue(MsGwIntegrationEntities::getModelRepoUrl);
        if (repoUrl.isEmpty() || !StringUtils.hasText((String) repoUrl.get())) {
            return null;
        }

        return (String) repoUrl.get();
    }

    public void saveGatewayRelation(Map<String, List<String>> gatewayRelation) {
        try {
            String relStr = json.writeValueAsString(gatewayRelation);
            new AnnotatedEntityWrapper<MsGwIntegrationEntities>().saveValue(MsGwIntegrationEntities::getGatewayDeviceRelation, relStr);
        } catch (Exception e) {
            throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), "Save relation error: " + e.getMessage()).build();
        }
    }

    public DeviceModelData getModelData() {
        AnnotatedEntityWrapper<MsGwIntegrationEntities> gatewayEntitiesWrapper = new AnnotatedEntityWrapper<>();
        String modelDataStr = (String) gatewayEntitiesWrapper.getValue(MsGwIntegrationEntities::getDeviceModelData).orElse("{}");
        try {
            return ResourceString.jsonInstance().readValue(modelDataStr, DeviceModelData.class);
        } catch (Exception e) {
            throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), "Broken model data: " + e.getMessage() + "\n" + modelDataStr).build();
        }
    }

    public Map<String, DeviceConnectStatus> getGatewayStatus(List<String> gatewayIdentifiers) {
        List<String> statusKeys = gatewayIdentifiers.stream().map(GatewayString::getGatewayStatusKey).toList();
        return entityValueServiceProvider.findValuesByKeys(statusKeys).entrySet().stream()
                .collect(Collectors.toMap(entry -> GatewayString.parseGatewayIdentifier(entry.getKey()), entry -> DeviceConnectStatus.valueOf((String) entry.getValue())));
    }

    public String getDeviceDecoderScript(String deviceEui) {
        return (String) entityValueServiceProvider.findValueByKey(GatewayString.getDeviceEntityKey(deviceEui, ResourceConstant.DECODER_ENTITY_IDENTIFIER));
    }

    public String getDeviceEncoderScript(String deviceEui) {
        return (String) entityValueServiceProvider.findValueByKey(GatewayString.getDeviceEntityKey(deviceEui, ResourceConstant.ENCODER_ENTITY_IDENTIFIER));
    }
}
