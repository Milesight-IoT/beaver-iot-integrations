package com.milesight.beaveriot.integrations.milesightgateway.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.milesight.beaveriot.base.annotations.shedlock.DistributedLock;
import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.api.DeviceTemplateParserProvider;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.context.integration.model.EntityBuilder;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.integration.model.event.ExchangeEvent;
import com.milesight.beaveriot.context.model.response.DeviceTemplateOutputResult;
import com.milesight.beaveriot.eventbus.annotations.EventSubscribe;
import com.milesight.beaveriot.eventbus.api.Event;
import com.milesight.beaveriot.integrations.milesightgateway.entity.MsGwIntegrationEntities;
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
import java.util.concurrent.atomic.AtomicReference;

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
    MsGwEntityService msGwEntityService;

    @Autowired
    GatewayRequester gatewayRequester;

    @Autowired
    EntityManager entityManager;

    @Autowired
    DeviceTemplateParserProvider deviceTemplateParserProvider;

    @Autowired
    EntityValueServiceProvider entityValueServiceProvider;

    private final ObjectMapper json = GatewayString.jsonInstance();

    public List<Device> getAllDevices() {
        return deviceServiceProvider.findAll(Constants.INTEGRATION_ID);
    }

    public List<Device> getDevices(List<String> euiList) {
        return deviceServiceProvider.findByIdentifiers(euiList, Constants.INTEGRATION_ID);
    }

    public Entity generateOfflineTimeoutEntity(String deviceKey) {
        return new EntityBuilder(Constants.INTEGRATION_ID, deviceKey)
                .identifier(Constants.OFFLINE_TIMEOUT_ENTITY_IDENTIFIER)
                .property(Constants.OFFLINE_TIMEOUT_ENTITY_NAME, AccessMod.RW)
                .valueType(EntityValueType.LONG)
                .attributes(Map.of(
                        "min", Constants.OFFLINE_TIMEOUT_ENTITY_MIN_VALUE,
                        "max", Constants.OFFLINE_TIMEOUT_ENTITY_MAX_VALUE,
                        "unit", Constants.OFFLINE_TIMEOUT_ENTITY_UNIT
                ))
                .build();
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
        DeviceModelIdentifier modelIdentifier = DeviceModelIdentifier.of(deviceModelId);
        deviceData.setDeviceModel(deviceModelId);
        deviceData.setFPort(addDevice.getFPort());
        deviceData.setFrameCounterValidation(addDevice.getFrameCounterValidation());
        deviceData.setAppKey(addDevice.getAppKey());

        final DeviceService self = self();
        AtomicReference<String> timeoutEntityKey = new AtomicReference<>();

        deviceTemplateParserProvider.createDevice(
                Constants.INTEGRATION_ID,
                modelIdentifier.getVendorId(),
                modelIdentifier.getModelId(),
                GatewayString.standardizeEUI(deviceData.getEui()),
                deviceName,
                (device, metadata) -> {
                    List<Entity> entities = new ArrayList<>(device.getEntities());
                    Entity timeoutEntity = generateOfflineTimeoutEntity(device.getKey());
                    entities.add(timeoutEntity);
                    timeoutEntityKey.set(timeoutEntity.getKey());
                    device.setEntities(entities);
                    device.setAdditional(json.convertValue(deviceData, new TypeReference<>() {}));

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
                    if (metadata == null || metadata.get(Constants.LORA_CLASS_METADATA_KEY) == null) {
                        throw ServiceException.with(MilesightGatewayErrorCode.TEMPLATE_MISSING_LORA_PROFILE).args(Map.of(
                                "deviceModelId", deviceModelId
                        )).build();
                    }

                    String profileName = (String) metadata.get(Constants.LORA_CLASS_METADATA_KEY);

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

                    self.manageGatewayDevices(gatewayEUI, deviceEUI, GatewayDeviceOperation.ADD);
                    try {
                        gatewayRequester.requestAddDevice(gatewayEUI, addDeviceRequest);
                    } catch (Exception e) {
                        self.manageGatewayDevices(gatewayEUI, deviceEUI, GatewayDeviceOperation.DELETE);
                        throw e;
                    }

                    return true;
                });

        entityValueServiceProvider.saveLatestValues(ExchangePayload.create(Map.of(
                timeoutEntityKey.get(), addDevice.getOfflineTimeout()
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
            int fPort = payload.getFPort().intValue();

            DeviceTemplateOutputResult outputResult = deviceTemplateParserProvider.output(payload.getDeviceKey(), ExchangePayload.create(payload.getPayload()), Map.of("fPort", fPort));
            byte[] byteData = (byte[]) outputResult.getOutput();

            String encodedData = Base64.getEncoder().encodeToString(byteData);
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

    public Long getDeviceOfflineTimeout(Device device) {
        Optional<Entity> offlineTimeoutEntity = device.getEntities().stream().filter(entity -> entity.getIdentifier().equals(Constants.OFFLINE_TIMEOUT_ENTITY_IDENTIFIER)).findFirst();
        if (offlineTimeoutEntity.isEmpty()) {
            return Constants.DEFAULT_DEVICE_OFFLINE_TIMEOUT;
        }

        Long offlineTimeout = (Long) entityValueServiceProvider.findValueByKey(offlineTimeoutEntity.get().getKey());
        if (offlineTimeout == null) {
            return Constants.DEFAULT_DEVICE_OFFLINE_TIMEOUT;
        }

        return offlineTimeout;
    }

    private DeviceService self() {
        return (DeviceService) AopContext.currentProxy();
    }
}
