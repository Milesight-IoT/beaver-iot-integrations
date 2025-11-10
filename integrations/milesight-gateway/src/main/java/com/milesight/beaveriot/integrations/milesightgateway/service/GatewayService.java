package com.milesight.beaveriot.integrations.milesightgateway.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.milesight.beaveriot.base.annotations.shedlock.DistributedLock;
import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.context.api.*;
import com.milesight.beaveriot.context.integration.enums.CredentialsType;
import com.milesight.beaveriot.context.integration.model.*;
import com.milesight.beaveriot.context.integration.model.event.DeviceEvent;
import com.milesight.beaveriot.context.integration.model.event.ExchangeEvent;
import com.milesight.beaveriot.eventbus.annotations.EventSubscribe;
import com.milesight.beaveriot.eventbus.api.Event;
import com.milesight.beaveriot.integrations.milesightgateway.entity.MsGwIntegrationEntities;
import com.milesight.beaveriot.integrations.milesightgateway.model.*;
import com.milesight.beaveriot.integrations.milesightgateway.model.request.FetchGatewayCredentialRequest;
import com.milesight.beaveriot.integrations.milesightgateway.model.response.GatewayDeviceListItem;
import com.milesight.beaveriot.integrations.milesightgateway.model.response.MqttCredentialResponse;
import com.milesight.beaveriot.integrations.milesightgateway.mqtt.MsGwMqttUtil;
import com.milesight.beaveriot.integrations.milesightgateway.requester.GatewayRequester;
import com.milesight.beaveriot.integrations.milesightgateway.requester.GatewayRequesterFactory;
import com.milesight.beaveriot.integrations.milesightgateway.requester.model.GatewayRequesterGuessResult;
import com.milesight.beaveriot.integrations.milesightgateway.model.api.DeviceListResponse;
import com.milesight.beaveriot.integrations.milesightgateway.model.request.AddGatewayRequest;
import com.milesight.beaveriot.integrations.milesightgateway.model.response.ConnectionValidateResponse;
import com.milesight.beaveriot.integrations.milesightgateway.util.Constants;
import com.milesight.beaveriot.integrations.milesightgateway.util.GatewayString;
import com.milesight.beaveriot.integrations.milesightgateway.util.LockConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
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
    GatewayRequesterFactory gatewayRequesterFactory;

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

    @Autowired
    DeviceStatusServiceProvider deviceStatusServiceProvider;

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

        String downlinkTopic;
        if (gateway == null || StringUtils.hasText(GatewayData.fromMap(gateway.getAdditional()).getVersion())) {
            downlinkTopic = MsGwMqttUtil.getDownlinkTopic(gatewayEui, MsGwMqttUtil.DEVICE_EUI_PLACEHOLDER);
        } else {
            // compatible
            downlinkTopic = MsGwMqttUtil.getDownlinkTopic(gatewayEui, null);
        }

        // set topics
        response.setUplinkDataTopic(mqttServiceProvider.getFullTopicName(credentials.getAccessKey(), MsGwMqttUtil.getUplinkTopic(gatewayEui)));
        response.setDownlinkDataTopic(mqttServiceProvider.getFullTopicName(credentials.getAccessKey(), downlinkTopic));
        response.setRequestDataTopic(mqttServiceProvider.getFullTopicName(credentials.getAccessKey(), MsGwMqttUtil.getRequestTopic(gatewayEui)));
        response.setResponseDataTopic(mqttServiceProvider.getFullTopicName(credentials.getAccessKey(), MsGwMqttUtil.getResponseTopic(gatewayEui)));

        return response;
    }

    public void validateGatewayInfo(String eui) {
        String gatewayEui = GatewayString.standardizeEUI(eui);
        if (getGatewayByEui(gatewayEui) != null) {
            throw ServiceException.with(MilesightGatewayErrorCode.DUPLICATED_GATEWAY_EUI).args(Map.of("eui", eui)).build();
        }
    }

    public ConnectionValidateResponse validateGatewayConnection(String inputEui, String credentialId) {
        String eui = GatewayString.standardizeEUI(inputEui);
        validateGatewayInfo(eui);
        ConnectionValidateResponse result = new ConnectionValidateResponse();
        GatewayRequesterGuessResult guessResult = gatewayRequesterFactory.guess(eui);
        DeviceListResponse responseData = guessResult.getBaseResponse().getSuccessBody();
        if (ObjectUtils.isEmpty(responseData.getAppResult())) {
            throw ServiceException.with(MilesightGatewayErrorCode.GATEWAY_NO_APPLICATION).build();
        }

        if (ObjectUtils.isEmpty(responseData.getProfileResult())) {
            throw ServiceException.with(MilesightGatewayErrorCode.GATEWAY_NO_DEVICE_PROFILE).build();
        }

        result.setAppResult(responseData.getAppResult());
        result.setProfileResult(responseData.getProfileResult());
        result.setVersion(guessResult.getGatewayRequester().getVersion());

        Optional<Credentials> credentials = credentialsServiceProvider.getCredentials(Long.valueOf(credentialId));
        if (credentials.isEmpty() || !credentials.get().getAccessKey().equals(guessResult.getBaseResponse().getCtx().getUsername())) {
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

    @DistributedLock(name = LockConstants.UPDATE_GATEWAY_DEVICE_ENUM_LOCK, waitForLock = "5s")
    public void putAddDeviceGatewayEui(List<Device> gateways) {
        Entity gatewayEuiEntity = getAddDeviceGatewayEntity();
        LinkedHashMap<String, String> attrEnum = json.convertValue(gatewayEuiEntity.getAttributes().get(AttributeBuilder.ATTRIBUTE_ENUM), new TypeReference<>() {});
        gateways.forEach(gateway -> attrEnum.put(getGatewayEui(gateway), gateway.getName()));
        gatewayEuiEntity.getAttributes().put(AttributeBuilder.ATTRIBUTE_DEFAULT_VALUE, attrEnum.entrySet().iterator().next().getKey());

        gatewayEuiEntity.getAttributes().put(AttributeBuilder.ATTRIBUTE_ENUM, attrEnum);
        entityServiceProvider.save(gatewayEuiEntity);
    }

    @DistributedLock(name = LockConstants.UPDATE_GATEWAY_DEVICE_ENUM_LOCK, waitForLock = "5s")
    public void removeAddDeviceGatewayEui(List<String> gatewayEuiList) {
        Entity gatewayEuiEntity = getAddDeviceGatewayEntity();
        LinkedHashMap<String, String> attrEnum = json.convertValue(gatewayEuiEntity.getAttributes().get(AttributeBuilder.ATTRIBUTE_ENUM), new TypeReference<>() {});
        gatewayEuiList.forEach(eui -> attrEnum.remove(GatewayString.standardizeEUI(eui)));
        gatewayEuiEntity.getAttributes().put(AttributeBuilder.ATTRIBUTE_ENUM, attrEnum);
        if (!attrEnum.isEmpty()) {
            gatewayEuiEntity.getAttributes().put(AttributeBuilder.ATTRIBUTE_DEFAULT_VALUE, attrEnum.entrySet().iterator().next().getKey());
        } else {
            gatewayEuiEntity.getAttributes().remove(AttributeBuilder.ATTRIBUTE_DEFAULT_VALUE);
        }
        entityServiceProvider.save(gatewayEuiEntity);
    }

    @DistributedLock(name = LockConstants.UPDATE_GATEWAY_DEVICE_RELATION_LOCK, waitForLock = "10s")
    public GatewayData addGateway(AddGatewayRequest request) {
        GatewayData newGatewayData = new GatewayData();

        // validate connection again
        newGatewayData.setEui(GatewayString.standardizeEUI(request.getEui()));
        newGatewayData.setCredentialId(request.getCredentialId());

        // check application
        ConnectionValidateResponse validateResult = validateGatewayConnection(newGatewayData.getEui(), request.getCredentialId());
        if (validateResult.getAppResult().stream().noneMatch(appItem -> appItem.getApplicationID().equals(request.getApplicationId()))) {
            throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED.getErrorCode(), "Unknown application: " + request.getApplicationId()).build();
        }

        newGatewayData.setApplicationId(request.getApplicationId());
        newGatewayData.setClientId(request.getClientId());
        newGatewayData.setVersion(validateResult.getVersion());

        if (request.getClientId() == null) {
            throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED.getErrorCode(), "Client Id not provided").build();
        } else if (!GatewayString.validateGatewayClientId(request.getClientId(), newGatewayData.getEui())) {
            throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED.getErrorCode(), "Invalid Client Id: " + request.getClientId()).build();
        }

        // check duplicate
        Map<String, List<String>> gatewayRelation = msGwEntityService.getGatewayRelation();

        if (gatewayRelation.keySet().stream().anyMatch(gatewayEui -> gatewayEui.equals(newGatewayData.getEui()))) {
            throw ServiceException.with(MilesightGatewayErrorCode.DUPLICATED_GATEWAY_EUI).args(Map.of("eui", newGatewayData.getEui())).build();
        }

        // build and add gateway device
        Device gateway = new DeviceBuilder(INTEGRATION_ID)
                .name(request.getName())
                .identifier(GatewayString.getGatewayIdentifier(newGatewayData.getEui()))
                .additional(json.convertValue(newGatewayData, new TypeReference<>() {}))
                .build();
        deviceServiceProvider.save(gateway);
        deviceStatusServiceProvider.online(gateway);

        // add to relation

        gatewayRelation.put(newGatewayData.getEui(), new ArrayList<>());
        msGwEntityService.saveGatewayRelation(gatewayRelation);

        // add to add device gateway list
        self().putAddDeviceGatewayEui(List.of(gateway));

        // check duplicate eui
        return newGatewayData;
    }

    @DistributedLock(name = LockConstants.UPDATE_GATEWAY_DEVICE_RELATION_LOCK, waitForLock = "10s")
    @Transactional(rollbackFor = Throwable.class)
    public void batchDeleteGateway(List<String> gatewayEuiList) {
        Map<String, List<String>> gatewayDeviceListMap = msGwEntityService.getGatewayRelation();

        // find gateway that have devices then delete gateways and devices
        List<String> deviceEuiList = new ArrayList<>();
        for (String inputEUI : gatewayEuiList) {
            String gatewayEui = GatewayString.standardizeEUI(inputEUI);
            List<String> gatewayDeviceEuiList = gatewayDeviceListMap.remove(gatewayEui);
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

        Map<String, Device> gatewayEuiMap = getGatewayByEuiList(gatewayEuiList).stream().collect(Collectors.toMap(this::getGatewayEui, Function.identity()));

        List<CompletableFuture<Void>> futures = gatewayDeviceToDelete.entrySet().stream().map(entry -> CompletableFuture.runAsync(() -> {
            String gatewayEui = entry.getKey();

            GatewayRequester gatewayRequester = gatewayRequesterFactory.create(GatewayData.fromMap(gatewayEuiMap.get(gatewayEui).getAdditional()));
            try {
                // delete devices
                gatewayRequester.requestDeleteDeviceAsync(entry.getValue());
            } catch (Exception e) {
                log.error("Delete device at gateway error: {} {}", gatewayEui, e.getMessage());
            }
        }, taskExecutor)).toList();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream().map(CompletableFuture::join))
                .join();

        // delete gateway
        for (Device gateway : gatewayEuiMap.values()) {
            deviceServiceProvider.deleteById(gateway.getId());
        }

        // save relation
        msGwEntityService.saveGatewayRelation(gatewayDeviceListMap);

        // delete gateway from add device gateway eui list
        self().removeAddDeviceGatewayEui(gatewayEuiList);
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
            self().batchDeleteGateway(List.of(getGatewayEui(device)));
        } else {
            GatewayDeviceData deviceData = deviceService.getDeviceData(device);
            gatewayRequesterFactory
                    .create(GatewayData.fromMap(getGatewayByEui(deviceData.getGatewayEUI()).getAdditional()))
                    .requestDeleteDeviceAsync(List.of(deviceData.getEui()));
            deviceService.manageGatewayDevices(deviceData.getGatewayEUI(), deviceData.getEui(), GatewayDeviceOperation.DELETE);
            deviceServiceProvider.deleteById(device.getId());
        }
    }

    public void syncGatewayListToAddDeviceGatewayEuiList() {
        List<Device> gatwayList = getAllGateways();
        Entity addDeviceGatewayEuiEntity = getAddDeviceGatewayEntity();
        LinkedHashMap<String, String> euiToNameMap = gatwayList.stream().collect(Collectors.toMap(
                this::getGatewayEui,
                Device::getName,
                (existing, replacement) -> existing,
                LinkedHashMap::new
        ));
        addDeviceGatewayEuiEntity.getAttributes().put(AttributeBuilder.ATTRIBUTE_ENUM, euiToNameMap);
        if (!gatwayList.isEmpty()) {
            addDeviceGatewayEuiEntity.getAttributes().put(AttributeBuilder.ATTRIBUTE_DEFAULT_VALUE, euiToNameMap.entrySet().iterator().next().getKey());
        }
        entityServiceProvider.save(addDeviceGatewayEuiEntity);
    }

    @EventSubscribe(payloadKeyExpression = Constants.INTEGRATION_ID + ".device.*", eventType = DeviceEvent.EventType.UPDATED)
    public void onUpdateDevice(DeviceEvent event) {
        Device device = event.getPayload();
        if (GatewayString.isGatewayIdentifier(device.getIdentifier())) {
            // sync gateway name to add device gateway eui list
            self().putAddDeviceGatewayEui(List.of(device));
        }
    }

    public Map<String, Object> doUpdateGatewayDevice(GatewayRequester gatewayRequester, String deviceEui, Map<String, Object> toUpdate) {
        if (toUpdate == null || toUpdate.isEmpty()) {
            return Map.of();
        }

        Optional<Map<String, Object>> deviceItem = gatewayRequester.requestDeviceItemByEui(deviceEui);
        if (deviceItem.isEmpty()) {
            log.warn("Device " + deviceEui + " not found in gateway " + gatewayRequester.getGatewayEui());
            return Map.of();
        }

        AtomicBoolean hasUpdate = new AtomicBoolean(false);
        toUpdate.forEach((String key, Object value) -> {
            if (Objects.equals(deviceItem.get().get(key), value)) {
                return;
            }

            deviceItem.get().put(key, value);
            hasUpdate.set(true);
        });

        if (!hasUpdate.get()) {
            log.info("Device " + deviceEui + " not changed. And would not be updated.");
            return deviceItem.get();
        }

        gatewayRequester.requestUpdateDeviceItem(deviceEui, deviceItem.get());
        return deviceItem.get();
    }

    private GatewayService self() {
        return (GatewayService) AopContext.currentProxy();
    }

    public List<GatewayDeviceListItem> getGatewayDevices(String eui) {
        List<String> deviceEuiList = msGwEntityService.getGatewayRelation().get(GatewayString.standardizeEUI(eui));
        if (deviceEuiList == null) {
            return List.of();
        }

        Set<String> foundEui = new HashSet<>();
        List<GatewayDeviceListItem> result = deviceService.getDevices(deviceEuiList).stream().map(device -> {
            GatewayDeviceListItem item = new GatewayDeviceListItem();
            item.setId(device.getId().toString());
            item.setKey(device.getKey());
            item.setEui(device.getIdentifier());
            item.setName(device.getName());
            item.setCreatedAt(device.getCreatedAt());
            foundEui.add(device.getIdentifier());
            return item;
        }).sorted(Comparator.comparingLong(GatewayDeviceListItem::getCreatedAt).reversed()).toList();

        // resolve dirty data
        deviceEuiList.forEach(deviceEui -> {
            if (!foundEui.contains(deviceEui)) {
                deviceService.manageGatewayDevices(eui, deviceEui, GatewayDeviceOperation.DELETE);
            }
        });
        return result;
    }
}
