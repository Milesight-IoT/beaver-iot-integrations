package com.milesight.beaveriot.integrations.milesightgateway.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.milesight.beaveriot.base.annotations.shedlock.DistributedLock;
import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.DeviceBuilder;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.integration.model.event.ExchangeEvent;
import com.milesight.beaveriot.eventbus.annotations.EventSubscribe;
import com.milesight.beaveriot.eventbus.api.Event;
import com.milesight.beaveriot.integrations.milesightgateway.codec.CodecExecutor;
import com.milesight.beaveriot.integrations.milesightgateway.codec.EntityValueConverter;
import com.milesight.beaveriot.integrations.milesightgateway.entity.MsGwIntegrationEntities;
import com.milesight.beaveriot.integrations.milesightgateway.codec.DeviceHelper;
import com.milesight.beaveriot.integrations.milesightgateway.model.*;
import com.milesight.beaveriot.integrations.milesightgateway.model.api.AddDeviceRequest;
import com.milesight.beaveriot.integrations.milesightgateway.model.api.DeviceListProfileItem;
import com.milesight.beaveriot.integrations.milesightgateway.model.api.DeviceListResponse;
import com.milesight.beaveriot.integrations.milesightgateway.mqtt.model.MqttResponse;
import com.milesight.beaveriot.integrations.milesightgateway.util.Constants;
import com.milesight.beaveriot.integrations.milesightgateway.util.GatewayRequester;
import com.milesight.beaveriot.integrations.milesightgateway.util.GatewayString;
import com.milesight.beaveriot.integrations.milesightgateway.util.LockConstants;
import jakarta.persistence.EntityManager;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * DeviceService class.
 *
 * @author simon
 * @date 2025/2/25
 */
@Component("milesightGatewayDeviceService")
@Slf4j
public class DeviceService {
    @Autowired
    DeviceServiceProvider deviceServiceProvider;

    @Autowired
    DeviceCodecService deviceCodecService;

    @Autowired
    MsGwEntityService msGwEntityService;

    @Autowired
    GatewayRequester gatewayRequester;

    @Autowired
    EntityValueServiceProvider entityValueServiceProvider;

    @Autowired
    EntityManager entityManager;

    private final ObjectMapper json = GatewayString.jsonInstance();

    public List<Device> getDevices(List<String> euiList) {
        return deviceServiceProvider.findByIdentifiers(euiList, Constants.INTEGRATION_ID);
    }

    @EventSubscribe(payloadKeyExpression = Constants.INTEGRATION_ID + ".integration.add-device.*", eventType = ExchangeEvent.EventType.CALL_SERVICE)
    public void onAddDevice(Event<MsGwIntegrationEntities.AddDevice> event) {
        MsGwIntegrationEntities.AddDevice addDevice = event.getPayload();
        String deviceName = addDevice.getAddDeviceName();
        String deviceEUI = GatewayString.standardizeEUI(addDevice.getEui());
        String gatewayEUI = GatewayString.standardizeEUI(addDevice.getGatewayEUI());
        if (!getDevices(List.of(deviceEUI)).isEmpty()) {
            throw ServiceException.with(MilesightGatewayErrorCode.DUPLICATED_DEVICE_EUI).args(Map.of("eui", deviceEUI)).build();
        }

        GatewayDeviceData deviceData = new GatewayDeviceData();
        deviceData.setEui(deviceEUI);

        // get gateway data
        Device gateway = deviceServiceProvider.findByIdentifier(GatewayString.getGatewayIdentifier(gatewayEUI), Constants.INTEGRATION_ID);
        if (gateway == null) {
            throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED.getErrorCode(), "Unknown gateway EUI: " + gatewayEUI).build();
        }
        GatewayData gatewayData = json.convertValue(gateway.getAdditional(), GatewayData.class);

        deviceData.setGatewayEUI(gatewayEUI);

        MqttResponse<DeviceListResponse> deviceListResponseMqttResponse = gatewayRequester.requestDeviceList(gatewayEUI, 0, 1, null);

        // get device model
        String deviceModelId = addDevice.getDeviceModel();
        DeviceCodecData codecData = deviceCodecService.batchGetDeviceCodecData(List.of(deviceModelId)).getOrDefault(deviceModelId, null);
        if (codecData == null) {
            throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED.getErrorCode(), "Cannot find codec for: " + deviceModelId).build();
        }

        deviceData.setDeviceModel(deviceModelId);
        deviceData.setFPort(addDevice.getFPort());
        deviceData.setFrameCounterValidation(addDevice.getFrameCounterValidation());
        deviceData.setAppKey(addDevice.getAppKey());

        Device device = new DeviceBuilder(Constants.INTEGRATION_ID)
                .name(deviceName)
                .identifier(deviceEUI)
                .additional(json.convertValue(deviceData, new TypeReference<>() {}))
                .build();
        DeviceHelper.UpdateResourceResult updateResourceResult = DeviceHelper.updateResourceInfo(device, codecData.getDef());

        // request gateway
        AddDeviceRequest addDeviceRequest = new AddDeviceRequest();
        addDeviceRequest.setName(deviceName);
        addDeviceRequest.setDevEUI(deviceEUI);
        addDeviceRequest.setFPort(addDevice.getFPort());
        addDeviceRequest.setDescription("From Beaver IoT");
        if (StringUtils.hasText(addDevice.getAppKey())) {
            addDeviceRequest.setAppKey(addDevice.getAppKey());
        } else {
            addDeviceRequest.setAppKey(Constants.DEFAULT_APP_KEY);
        }

        addDeviceRequest.setSkipFCntCheck(!addDevice.getFrameCounterValidation());
        addDeviceRequest.setApplicationID(gatewayData.getApplicationId());
        String profileName = codecData.getResourceInfo().getDeviceProfile().get(0);

        Optional<DeviceListProfileItem> profileItem = deviceListResponseMqttResponse
                .getSuccessBody()
                .getProfileResult()
                .stream()
                .filter(deviceListProfileItem -> deviceListProfileItem.getProfileName().equals(profileName))
                .findFirst();
        if (profileItem.isEmpty()) {
            throw ServiceException.with(MilesightGatewayErrorCode.NO_VALID_PROFILE_FOR_DEVICE).args(Map.of(
                    "gatewayEui", gatewayEUI,
                    "profileName", profileName
            )).build();
        }
        addDeviceRequest.setProfileID(profileItem.get().getProfileID());

        self().manageGatewayDevices(gatewayEUI, deviceEUI, GatewayDeviceOperation.ADD);
        try {
            gatewayRequester.requestAddDevice(gatewayEUI, addDeviceRequest);
            deviceServiceProvider.save(device);
        } catch (Exception e) {
            self().manageGatewayDevices(gatewayEUI, deviceEUI, GatewayDeviceOperation.DELETE);
            throw e;
        }

        // save script
        entityValueServiceProvider.saveLatestValues(ExchangePayload.create(Map.of(
                updateResourceResult.getDecoderEntity().getKey(), codecData.getDecoderStr(),
                updateResourceResult.getEncoderEntity().getKey(), codecData.getEncoderStr()
        )));
    }

    public GatewayDeviceData getDeviceData(Device device) {
        return json.convertValue(device.getAdditional(), GatewayDeviceData.class);
    }

    @DistributedLock(name = LockConstants.UPDATE_GATEWAY_DEVICE_ENUM_LOCK, waitForLock = "5s")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void manageGatewayDevices(String gatewayEUI, String deviceEUI, GatewayDeviceOperation op) {
        entityManager.flush();
        entityManager.clear();
        Map<String, List<String>> gatewayDeviceRelation = msGwEntityService.getGatewayRelation();
        List<String> deviceList = gatewayDeviceRelation.get(gatewayEUI);
        if (op == GatewayDeviceOperation.ADD) {
            if (deviceList.contains(deviceEUI)) {
                throw ServiceException.with(MilesightGatewayErrorCode.DUPLICATED_DEVICE_EUI).args(Map.of("eui", deviceEUI)).build();
            }

            deviceList.add(0, deviceEUI);
        } else if (op == GatewayDeviceOperation.DELETE) {
            if (deviceList == null) {
                return;
            }

            deviceList.remove(deviceEUI);
        } else {
            throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), "Unsupported gateway device relation op: " + op.name()).build();
        }

        msGwEntityService.saveGatewayRelation(gatewayDeviceRelation);
    }

    @Data
    private static class DevicePayload {
        private String gatewayEui;

        private String deviceKey;

        private Long fPort;

        private String credentialId;

        private String mqttUsername;

        private Map<String, Object> payload = new HashMap<>();
    }

    @EventSubscribe(payloadKeyExpression = Constants.INTEGRATION_ID + ".device.*", eventType = {
            ExchangeEvent.EventType.CALL_SERVICE, ExchangeEvent.EventType.UPDATE_PROPERTY})
    public void onDeviceEntityExchange(ExchangeEvent event) {
        Map<String, DevicePayload> devicePayloadMap = getDevicePayloadMap(event);

        getDevices(devicePayloadMap.keySet().stream().toList()).forEach(device -> {
            GatewayDeviceData deviceData = getDeviceData(device);
            devicePayloadMap.get(deviceData.getEui()).setFPort(deviceData.getFPort());
        });

        // use default credential for now, so we don't fetch gateways for username or credential id.

        // downlink one by one
        devicePayloadMap.forEach((deviceEui, payload) -> {
            JsonNode jsonData = EntityValueConverter.convertToJson(payload.getDeviceKey(), payload.getPayload());
            log.debug("Downlink json data: " + jsonData);

            String encoderScript = msGwEntityService.getDeviceEncoderScript(deviceEui);
            if (!StringUtils.hasText(encoderScript)) {
                log.warn("Encode Script not found: " + deviceEui);
                return;
            }

            int fPort = payload.getFPort().intValue();

            String encodedData = CodecExecutor.runEncode(encoderScript, fPort, jsonData);
            log.debug("Downlink encoded data: " + encodedData);
            if (!StringUtils.hasText(encodedData)) {
                return;
            }

            gatewayRequester.downlink(payload.getGatewayEui(), deviceEui, fPort, encodedData);

        });
    }

    private Map<String, DevicePayload> getDevicePayloadMap(ExchangeEvent event) {
        Map<String, Object> allPayloads = event.getPayload().getAllPayloads();
        Map<String, Entity> entityMap = event.getPayload().getExchangeEntities();

        Map<String, DevicePayload> devicePayloadMap = new HashMap<>();
        Map<String, String> deviceToGatewayMap = msGwEntityService.getDeviceGatewayRelation();
        // split by device
        allPayloads.forEach((String entityKey, Object entityValue) -> {
            Entity entity = entityMap.get(entityKey);
            String deviceKey = entity.getDeviceKey();
            String deviceEui = GatewayString.getDeviceIdentifierByKey(deviceKey);
            if (GatewayString.isGatewayIdentifier(deviceEui)) {
                return;
            }

            DevicePayload devicePayload = devicePayloadMap.computeIfAbsent(deviceEui, k -> new DevicePayload());
            devicePayload.setDeviceKey(deviceKey);

            devicePayload.setGatewayEui(deviceToGatewayMap.get(deviceEui));
            if (devicePayload.getGatewayEui() == null) {
                throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), "Cannot find gateway for device: " + deviceKey).build();
            }

            Object value = entityValue;
            if (entity.getValueType().equals(EntityValueType.BOOLEAN)) {
                value = entityValue.equals(Boolean.FALSE) ? 0 : 1;
            }

            devicePayload.getPayload().put(entityKey, value);
        });
        return devicePayloadMap;
    }

    private DeviceService self() {
        return (DeviceService) AopContext.currentProxy();
    }
}
