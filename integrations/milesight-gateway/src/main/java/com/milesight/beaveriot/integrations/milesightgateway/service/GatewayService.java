package com.milesight.beaveriot.integrations.milesightgateway.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.milesight.beaveriot.base.annotations.shedlock.DistributedLock;
import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.context.api.CredentialsServiceProvider;
import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.api.MqttPubSubServiceProvider;
import com.milesight.beaveriot.context.integration.enums.CredentialsType;
import com.milesight.beaveriot.context.integration.model.*;
import com.milesight.beaveriot.context.integration.model.event.DeviceEvent;
import com.milesight.beaveriot.context.integration.model.event.ExchangeEvent;
import com.milesight.beaveriot.context.integration.wrapper.AnnotatedTemplateEntityWrapper;
import com.milesight.beaveriot.eventbus.annotations.EventSubscribe;
import com.milesight.beaveriot.eventbus.api.Event;
import com.milesight.beaveriot.integrations.milesightgateway.entity.GatewayEntities;
import com.milesight.beaveriot.integrations.milesightgateway.entity.MsGwIntegrationEntities;
import com.milesight.beaveriot.integrations.milesightgateway.model.DeviceConnectStatus;
import com.milesight.beaveriot.integrations.milesightgateway.model.GatewayDeviceData;
import com.milesight.beaveriot.integrations.milesightgateway.model.GatewayDeviceOperation;
import com.milesight.beaveriot.integrations.milesightgateway.model.api.DeviceListItemFields;
import com.milesight.beaveriot.integrations.milesightgateway.model.request.FetchGatewayCredentialRequest;
import com.milesight.beaveriot.integrations.milesightgateway.model.response.MqttCredentialResponse;
import com.milesight.beaveriot.integrations.milesightgateway.mqtt.MsGwMqttUtil;
import com.milesight.beaveriot.integrations.milesightgateway.mqtt.model.MqttResponse;
import com.milesight.beaveriot.integrations.milesightgateway.util.GatewayRequester;
import com.milesight.beaveriot.integrations.milesightgateway.model.api.DeviceListProfileItem;
import com.milesight.beaveriot.integrations.milesightgateway.model.api.DeviceListResponse;
import com.milesight.beaveriot.integrations.milesightgateway.model.GatewayData;
import com.milesight.beaveriot.integrations.milesightgateway.model.request.AddGatewayRequest;
import com.milesight.beaveriot.integrations.milesightgateway.model.response.ConnectionValidateResponse;
import com.milesight.beaveriot.integrations.milesightgateway.util.Constants;
import com.milesight.beaveriot.integrations.milesightgateway.util.GatewayString;
import com.milesight.beaveriot.integrations.milesightgateway.util.LockConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.milesight.beaveriot.integrations.milesightgateway.util.Constants.*;

/**
 * MilesightGatewayService class.
 *
 * @author simon
 * @date 2025/2/14
 */
@Component("milesightGatewayService")
@Slf4j
public class GatewayService {
    @Autowired
    GatewayRequester gatewayRequester;

    @Autowired
    EntityServiceProvider entityServiceProvider;

    @Autowired
    DeviceService deviceService;

    @Autowired
    MsGwEntityService msGwEntityService;

    @Autowired
    DeviceServiceProvider deviceServiceProvider;

    @Autowired
    MqttPubSubServiceProvider mqttServiceProvider;

    @Autowired
    CredentialsServiceProvider credentialsServiceProvider;

    @Autowired
    TaskExecutor taskExecutor;

    private final ObjectMapper json = GatewayString.jsonInstance();

    public MqttCredentialResponse fetchCredential(FetchGatewayCredentialRequest request) {
        String gatewayEui = GatewayString.standardizeEUI(request.getEui());
        Device gateway = getGatewayByEui(gatewayEui);
        String credentialId = gateway == null ? request.getCredentialId() : getGatewayCredentialId(gateway);
        String clientId = resolveGatewayClientId(gateway, gatewayEui);
        // set credential
        Credentials credentials = null;
        if (credentialId == null) {
            credentials = credentialsServiceProvider.getOrCreateCredentials(CredentialsType.MQTT);
        } else {
            credentials = credentialsServiceProvider.getCredentials(Long.valueOf(credentialId)).orElse(null);
        }

        if (credentials == null) {
            throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED.getErrorCode(), "Cannot find credentials: " + credentialId).build();
        }

        MqttCredentialResponse response = new MqttCredentialResponse();

        response.setCredentialId(credentials.getId().toString());
        response.setUsername(credentials.getAccessKey());
        response.setPassword(credentials.getAccessSecret());
        response.setClientId(clientId);

        // set topics
        response.setUplinkDataTopic(mqttServiceProvider.getFullTopicName(credentials.getAccessKey(), MsGwMqttUtil.getMqttTopic(gatewayEui, GATEWAY_MQTT_UPLINK_SCOPE)));
        response.setDownlinkDataTopic(mqttServiceProvider.getFullTopicName(credentials.getAccessKey(), MsGwMqttUtil.getMqttTopic(gatewayEui, GATEWAY_MQTT_DOWNLINK_SCOPE)));
        response.setRequestDataTopic(mqttServiceProvider.getFullTopicName(credentials.getAccessKey(), MsGwMqttUtil.getMqttTopic(gatewayEui, GATEWAY_MQTT_REQUEST_SCOPE)));
        response.setResponseDataTopic(mqttServiceProvider.getFullTopicName(credentials.getAccessKey(), MsGwMqttUtil.getMqttTopic(gatewayEui, GATEWAY_MQTT_RESPONSE_SCOPE)));

        return response;
    }

    public void validateGatewayInfo(String eui) {
        String gatewayEui = GatewayString.standardizeEUI(eui);
        if (getGatewayByEui(gatewayEui) != null) {
            throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED.getErrorCode(), "Gateway device has existed: " + eui).build();
        }
    }

    public ConnectionValidateResponse validateGatewayConnection(String eui, String credentialId) {
        validateGatewayInfo(eui);
        ConnectionValidateResponse result = new ConnectionValidateResponse();
        MqttResponse<DeviceListResponse> response = gatewayRequester.requestDeviceList(eui, 0, 1, null);
        DeviceListResponse responseData = response.getSuccessBody();
        if (ObjectUtils.isEmpty(responseData.getAppResult())) {
            throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED.getErrorCode(), "Empty applications.").build();
        }

        if (ObjectUtils.isEmpty(responseData.getProfileResult())) {
            throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED.getErrorCode(), "Empty profiles.").build();
        }

        result.setAppResult(responseData.getAppResult());
        result.setProfileResult(responseData.getProfileResult());

        Optional<Credentials> credentials = credentialsServiceProvider.getCredentials(Long.valueOf(credentialId));
        if (credentials.isEmpty() || !credentials.get().getAccessKey().equals(response.getCtx().getUsername())) {
            throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED.getErrorCode(), "Invalid credential: " + credentialId).build();
        }

        return result;
    }

    public Device getGatewayByEui(String eui) {
        return deviceServiceProvider.findByIdentifier(GatewayString.getGatewayIdentifier(eui), INTEGRATION_ID);
    }

    public List<Device> getGatewayByEuiList(List<String> euiList) {
        List<String> identifiers = euiList.stream().map(GatewayString::getGatewayIdentifier).toList();
        return deviceServiceProvider.findByIdentifiers(identifiers, INTEGRATION_ID);
    }

    public List<Device> getAllGateways() {
        List<String> gatewayEuiList = msGwEntityService.getGatewayRelation().keySet().stream().toList();
        return this.getGatewayByEuiList(gatewayEuiList);
    }

    private Entity getAddDeviceGatewayEntity() {
        Entity gatewayEuiEntity = entityServiceProvider.findByKey(MsGwIntegrationEntities.ADD_DEVICE_GATEWAY_EUI_KEY);
        Map<String, Object> attributes = gatewayEuiEntity.getAttributes();
        if (attributes == null) {
            attributes = new HashMap<>();
            gatewayEuiEntity.setAttributes(attributes);
            attributes.put(AttributeBuilder.ATTRIBUTE_ENUM, new HashMap<>());
        }

        return gatewayEuiEntity;
    }

    @DistributedLock(name = LockConstants.UPDATE_GATEWAY_DEVICE_ENUM_LOCK)
    private void putAddDeviceGatewayEui(List<Device> gateways) {
        Entity gatewayEuiEntity = getAddDeviceGatewayEntity();
        Map<String, String> attrEnum = json.convertValue(gatewayEuiEntity.getAttributes().get(AttributeBuilder.ATTRIBUTE_ENUM), new TypeReference<>() {});
        gateways.forEach(gateway -> attrEnum.put(getGatewayEui(gateway), gateway.getName()));

        gatewayEuiEntity.getAttributes().put(AttributeBuilder.ATTRIBUTE_ENUM, attrEnum);
        entityServiceProvider.save(gatewayEuiEntity);
    }

    @DistributedLock(name = LockConstants.UPDATE_GATEWAY_DEVICE_ENUM_LOCK)
    private void removeAddDeviceGatewayEui(List<String> gatewayEuiList) {
        Entity gatewayEuiEntity = getAddDeviceGatewayEntity();
        Map<String, String> attrEnum = json.convertValue(gatewayEuiEntity.getAttributes().get(AttributeBuilder.ATTRIBUTE_ENUM), new TypeReference<>() {});
        gatewayEuiList.forEach(eui -> attrEnum.remove(GatewayString.standardizeEUI(eui)));
        gatewayEuiEntity.getAttributes().put(AttributeBuilder.ATTRIBUTE_ENUM, attrEnum);
        entityServiceProvider.save(gatewayEuiEntity);
    }

    @DistributedLock(name = LockConstants.UPDATE_GATEWAY_DEVICE_RELATION_LOCK)
    public GatewayData addGateway(AddGatewayRequest request) {
        GatewayData newGatewayData = new GatewayData();

        // validate connection again
        newGatewayData.setEui(request.getEui());
        newGatewayData.setCredentialId(request.getCredentialId());

        // check application
        ConnectionValidateResponse validateResult = validateGatewayConnection(request.getEui(), request.getCredentialId());
        if (validateResult.getAppResult().stream().noneMatch(appItem -> appItem.getApplicationID().equals(request.getApplicationId()))) {
            throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED.getErrorCode(), "Unknown application: " + request.getApplicationId()).build();
        }
        newGatewayData.setApplicationId(request.getApplicationId());
        newGatewayData.setClientId(request.getClientId());

        if (request.getClientId() == null) {
            throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED.getErrorCode(), "Client Id not provided").build();
        } else if (!GatewayString.validateGatewayClientId(request.getClientId(), newGatewayData.getEui())) {
            throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED.getErrorCode(), "Invalid Client Id: " + request.getClientId()).build();
        }

        // check duplicate
        Map<String, List<String>> gatewayRelation = msGwEntityService.getGatewayRelation();

        if (gatewayRelation.keySet().stream().anyMatch(gatewayEui -> gatewayEui.equals(newGatewayData.getEui()))) {
            throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED.getErrorCode(), "Duplicated gateway EUI: " + newGatewayData.getEui()).build();
        }

        // build and add gateway device
        Device gateway = new DeviceBuilder(INTEGRATION_ID)
                .name(request.getName())
                .identifier(GatewayString.getGatewayIdentifier(newGatewayData.getEui()))
                .additional(json.convertValue(newGatewayData, new TypeReference<>() {}))
                .entities(new AnnotatedTemplateEntityBuilder(INTEGRATION_ID, newGatewayData.getEui()).build(GatewayEntities.class))
                .build();
        deviceServiceProvider.save(gateway);
        new AnnotatedTemplateEntityWrapper<GatewayEntities>(gateway.getIdentifier()).saveValue(GatewayEntities::getStatus, DeviceConnectStatus.ONLINE);

        // add to relation

        gatewayRelation.put(newGatewayData.getEui(), new ArrayList<>());
        msGwEntityService.saveGatewayRelation(gatewayRelation);

        // add to add device gateway list
        putAddDeviceGatewayEui(List.of(gateway));

        // check duplicate eui
        return newGatewayData;
    }

    @DistributedLock(name = LockConstants.UPDATE_GATEWAY_DEVICE_RELATION_LOCK)
    public void batchDeleteGateway(List<String> gatewayEuiList) {
        Map<String, List<String>> gatewayMap = msGwEntityService.getGatewayRelation();

        // find gateway that have devices then delete gateways and devices
        List<String> deviceEuiList = new ArrayList<>();
        for (String inputEUI : gatewayEuiList) {
            String gatewayEui = GatewayString.standardizeEUI(inputEUI);
            List<String> gatewayDeviceEuiList = gatewayMap.remove(gatewayEui);
            if (gatewayDeviceEuiList == null) {
                log.error("Gateway Relation not found: {}", gatewayEui);
                continue;
            }

            if (!gatewayDeviceEuiList.isEmpty()) {
                deviceEuiList.addAll(gatewayDeviceEuiList);
            }
        }

        Map<String, List<String>> gatewayDeviceToDelete = new HashMap<>();
        List<Device> deviceList = deviceService.getDevices(deviceEuiList);
        for (Device device : deviceList) {
            GatewayDeviceData deviceData = deviceService.getDeviceData(device);
            gatewayDeviceToDelete.computeIfAbsent(deviceData.getGatewayEUI(), k -> new ArrayList<>()).add(deviceData.getEui());
            deviceServiceProvider.deleteById(device.getId());
        }

        List<CompletableFuture<Void>> futures = gatewayDeviceToDelete.entrySet().stream().map(entry -> CompletableFuture.runAsync(() -> {
            String gatewayEui = entry.getKey();
            try {
                // check if the gateway is connected.
                gatewayRequester.requestDeviceList(gatewayEui, 0, 1, null);
                // delete devices
                gatewayRequester.requestDeleteDevice(gatewayEui, entry.getValue());
            } catch (Exception e) {
                log.error("Delete device at gateway error: {} {}", gatewayEui, e.getMessage());
            }
        }, taskExecutor)).toList();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream().map(CompletableFuture::join))
                .join();

        // delete gateway
        List<Device> gatewayList = getGatewayByEuiList(gatewayEuiList);
        for (Device gateway : gatewayList) {
            // TODO: optimize to batch delete
            deviceServiceProvider.deleteById(gateway.getId());
        }

        // save relation
        msGwEntityService.saveGatewayRelation(gatewayMap);

        // delete gateway from add device gateway eui list
        removeAddDeviceGatewayEui(gatewayEuiList);
    }

    public String getGatewayEui(Device gateway) {
        String eui = (String) gateway.getAdditional().get(GatewayData.Fields.eui);
        if (eui == null) {
            throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), "Not a gateway: " + gateway.getIdentifier()).build();
        }

        return eui;
    }

    public String getGatewayApplicationId(Device gateway) {
        return  (String) gateway.getAdditional().get(GatewayData.Fields.applicationId);
    }

    public String getGatewayCredentialId(Device gateway) {
        return  (String) gateway.getAdditional().get(GatewayData.Fields.credentialId);
    }

    private String resolveGatewayClientId(Device gateway, String gatewayEui) {
        if (gateway != null) {
            return getGatewayClientId(gateway);
        }

        return GatewayString.generateGatewayClientId(gatewayEui);
    }

    public String getGatewayClientId(Device gateway) {
        return  (String) gateway.getAdditional().get(GatewayData.Fields.clientId);
    }

    @EventSubscribe(payloadKeyExpression = Constants.INTEGRATION_ID + ".integration.delete-device", eventType = ExchangeEvent.EventType.CALL_SERVICE)
    public void onDeleteDevice(Event<MsGwIntegrationEntities.DeleteDevice> event) {
        MsGwIntegrationEntities.DeleteDevice deleteDevice = event.getPayload();
        Device device = deleteDevice.getDeletedDevice();
        if (GatewayString.isGatewayIdentifier(device.getIdentifier())) {
            batchDeleteGateway(List.of(getGatewayEui(device)));
        } else {
            GatewayDeviceData deviceData = deviceService.getDeviceData(device);
            gatewayRequester.requestDeleteDevice(deviceData.getGatewayEUI(), List.of(deviceData.getEui()));
            deviceService.manageGatewayDevices(deviceData.getGatewayEUI(), deviceData.getEui(), GatewayDeviceOperation.DELETE);
            deviceServiceProvider.deleteById(device.getId());
        }
    }

    public void syncGatewayListToAddDeviceGatewayEuiList() {
        List<Device> gatwayList = getAllGateways();
        Entity addDeviceGatewayEuiEntity = getAddDeviceGatewayEntity();
        Map<String, String> euiToNameMap = gatwayList.stream().collect(Collectors.toMap(
                this::getGatewayEui,
                Device::getName
        ));
        addDeviceGatewayEuiEntity.getAttributes().put(AttributeBuilder.ATTRIBUTE_ENUM, euiToNameMap);
        entityServiceProvider.save(addDeviceGatewayEuiEntity);
    }

    @EventSubscribe(payloadKeyExpression = Constants.INTEGRATION_ID + ".device.*", eventType = DeviceEvent.EventType.UPDATED)
    public void onUpdateDevice(DeviceEvent event) {
        Device device = event.getPayload();
        if (GatewayString.isGatewayIdentifier(device.getIdentifier())) {
            // sync gateway name to add device gateway eui list
            putAddDeviceGatewayEui(List.of(device));
        }
    }

    public Map<String, Object> doUpdateGatewayDevice(String gatewayEui, String deviceEui, String appId, Map<String, Object> toUpdate) {
        if (toUpdate == null || toUpdate.isEmpty()) {
            return Map.of();
        }

        Optional<Map<String, Object>> deviceItem = gatewayRequester.requestDeviceItemByEui(gatewayEui, deviceEui, appId);
        if (deviceItem.isEmpty()) {
            log.warn("Device " + deviceEui + " not found in gateway " + gatewayEui);
            return Map.of();
        }

        AtomicBoolean hasUpdate = new AtomicBoolean(false);
        toUpdate.forEach((String key, Object value) -> {
            if (deviceItem.get().get(key).equals(value)) {
                return;
            }

            deviceItem.get().put(key, value);
            hasUpdate.set(true);
        });

        if (!hasUpdate.get()) {
            log.info("Device " + deviceEui + " not changed. And would not be updated.");
            return deviceItem.get();
        }

        gatewayRequester.requestUpdateDeviceItem(gatewayEui, deviceEui, deviceItem.get());
        return deviceItem.get();
    }
}
