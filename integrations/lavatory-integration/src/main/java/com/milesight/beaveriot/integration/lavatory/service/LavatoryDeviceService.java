package com.milesight.beaveriot.integration.lavatory.service;

import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.constants.IntegrationConstants;
import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.context.integration.model.*;
import com.milesight.beaveriot.context.integration.model.event.ExchangeEvent;
import com.milesight.beaveriot.eventbus.annotations.EventSubscribe;
import com.milesight.beaveriot.eventbus.api.Event;
import com.milesight.beaveriot.integration.lavatory.constant.LavatoryIntegrationConstants;
import com.milesight.beaveriot.integration.lavatory.entity.LavatoryServiceEntities;
import com.milesight.beaveriot.integration.lavatory.util.LavatoryTslUtils;
import com.milesight.cloud.sdk.client.model.DeviceSaveOrUpdateRequest;
import com.milesight.cloud.sdk.client.model.ThingSpec;
import com.milesight.cloud.sdk.client.model.TslPropertyDataUpdateRequest;
import com.milesight.cloud.sdk.client.model.TslServiceCallRequest;
import com.milesight.msc.sdk.error.MscApiException;
import com.milesight.msc.sdk.error.MscSdkException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;


@Slf4j
@Service
public class LavatoryDeviceService {

    @Lazy
    @Autowired
    private ILavatoryClientProvider lavatoryClientProvider;

    @Autowired
    private DeviceServiceProvider deviceServiceProvider;

//    public void list(List<LavatoryRequest> requests) {
//        List<Device> devices = deviceServiceProvider.findAll(LavatoryIntegrationConstants.INTEGRATION_IDENTIFIER);
//        if (devices.isEmpty()) {
//            return;
//        }
//
//    }
//
//    private Map<LavatoryRequest, List<Device>> filtrationDevice(List<LavatoryRequest> requests, List<Device> devices) {
//        Map<Integer, List<Device>> floorMap = filtrationDevice(devices, requests.stream().map(LavatoryRequest::getFloor).collect(Collectors.toSet()), LavatoryIntegrationConstants.DeviceAdditionalDataName.FLOOR);
//        Map<Integer, List<Device>> sexMap = filtrationDevice(devices, requests.stream().map(LavatoryRequest::getSex).collect(Collectors.toSet()), LavatoryIntegrationConstants.DeviceAdditionalDataName.SEX);
//        Map<LavatoryRequest, List<Device>> lavatoryMap = new HashMap<>();
//        for (LavatoryRequest request : requests) {
//            List<Device> floorDevices = floorMap.get(request.getFloor());
//            List<Device> sexDevices = sexMap.get(request.getSex());
//            if (null != request.getFloor() && null != request.getSex()) {
//                lavatoryMap.put(request, floorDevices.stream().filter(d -> sexDevices.contains(d)).collect(Collectors.toList()));
//            } else if (null != request.getFloor()) {
//                lavatoryMap.put(request, floorDevices);
//            } else if (null != request.getSex()) {
//                lavatoryMap.put(request, sexDevices);
//            }
//        }
//        return lavatoryMap;
//    }
//
//    private Map<Integer, List<Device>> filtrationDevice(List<Device> devices, Set<Integer> retains, String deviceAdditionalDataName) {
//        Map<Integer, List<Device>> out = new HashMap<>();
//        for (Device device : devices) {
//            Map<String, Object> additional = device.getAdditional();
//            if (null == additional || additional.isEmpty()) {
//                continue;
//            }
//            Object o = additional.get(deviceAdditionalDataName);
//            if (null == o || !(o instanceof Integer)) {
//                continue;
//            }
//            Integer value = Integer.parseInt(o.toString());
//            if (retains.contains(value)) {
//                List<Device> computeIfAbsent = out.computeIfAbsent(value, k -> new ArrayList<>());
//                computeIfAbsent.add(device);
//                out.put(value, computeIfAbsent);
//            }
//        }
//        return out;
//    }

    @SneakyThrows
    @EventSubscribe(payloadKeyExpression = "lavatory.device.*", eventType = ExchangeEvent.EventType.DOWN)
    public void onDeviceExchangeEvent(ExchangeEvent event) {
        val exchangePayload = event.getPayload();
        val devices = exchangePayload.getExchangeEntities()
                .values()
                .stream()
                .map(Entity::getDeviceKey)
                .distinct()
                .map(deviceServiceProvider::findByKey)
                .filter(Objects::nonNull)
                .toList();
        if (devices.size() != 1) {
            log.warn("Invalid device number: {}", devices.size());
            return;
        }
        val device = devices.get(0);

        handlePropertiesPayload(device, exchangePayload);
        handleServicePayload(device, exchangePayload);
    }

    private void handleServicePayload(Device device, ExchangePayload exchangePayload) {
        val objectMapper = lavatoryClientProvider.getMscClient().getObjectMapper();
        val servicePayload = exchangePayload.getPayloadsByEntityType(EntityType.SERVICE);
        if (servicePayload.isEmpty()) {
            return;
        }
        val deviceId = (String) device.getAdditional().get(LavatoryIntegrationConstants.DeviceAdditionalDataName.DEVICE_ID);
        val serviceGroups = LavatoryTslUtils.convertExchangePayloadMapToGroupedJsonNode(
                objectMapper, device.getKey(), servicePayload);
        serviceGroups.entrySet().removeIf(entry -> LavatoryIntegrationConstants.InternalPropertyIdentifier.Pattern.match(entry.getKey()));
        if (serviceGroups.isEmpty()) {
            return;
        }
        serviceGroups.forEach((serviceId, serviceProperties) ->
                lavatoryClientProvider.getMscClient().device().callService(deviceId, TslServiceCallRequest.builder()
                        .serviceId(serviceId)
                        .inputs(serviceProperties)
                        .build()));
    }

    @SneakyThrows
    private void handlePropertiesPayload(Device device, ExchangePayload exchangePayload) {
        val objectMapper = lavatoryClientProvider.getMscClient().getObjectMapper();
        val propertiesPayload = exchangePayload.getPayloadsByEntityType(EntityType.PROPERTY);
        if (propertiesPayload.isEmpty()) {
            return;
        }
        val properties = LavatoryTslUtils.convertExchangePayloadMapToGroupedJsonNode(
                objectMapper, device.getKey(), propertiesPayload);
        properties.entrySet().removeIf(entry -> LavatoryIntegrationConstants.InternalPropertyIdentifier.Pattern.match(entry.getKey()));
        if (properties.isEmpty()) {
            return;
        }
        val deviceId = (String) device.getAdditional().get(LavatoryIntegrationConstants.DeviceAdditionalDataName.DEVICE_ID);
        lavatoryClientProvider.getMscClient().device().updateProperties(deviceId, TslPropertyDataUpdateRequest.builder()
                        .properties(properties)
                        .build())
                .execute();
    }

    @SneakyThrows
    @EventSubscribe(payloadKeyExpression = "lavatory.integration.add_device.*", eventType = ExchangeEvent.EventType.DOWN)
    public void onAddDevice(Event<LavatoryServiceEntities.AddDevice> event) {
        val payload = event.getPayload();
        val deviceName = payload.getContext("device_name", "Device Name");
        if (lavatoryClientProvider == null || lavatoryClientProvider.getMscClient() == null) {
            log.warn("MscClient not initiated.");
            return;
        }
        val identifier = payload.getSn();
        val mscClient = lavatoryClientProvider.getMscClient();
        val addDeviceResponse = mscClient.device().attach(DeviceSaveOrUpdateRequest.builder()
                        .name(deviceName)
                        .snDevEUI(identifier)
                        .autoProvision(false)
                        .build())
                .execute()
                .body();
        if (addDeviceResponse == null || addDeviceResponse.getData() == null
                || addDeviceResponse.getData().getDeviceId() == null) {
            log.warn("Add device failed: '{}' '{}'", deviceName, identifier);
            return;
        }

        val deviceId = addDeviceResponse.getData().getDeviceId();
        log.info("Device '{}' added to MSC with id '{}'", deviceName, deviceId);

        final String deviceIdStr = String.valueOf(deviceId);
        val thingSpec = getThingSpec(deviceIdStr);

        addLocalDevice(identifier, deviceName, payload.getFloor(), deviceIdStr, thingSpec);
    }

    public Device addLocalDevice(String identifier, String deviceName, Integer floor, String deviceId, ThingSpec thingSpec) {
        val integrationId = LavatoryIntegrationConstants.INTEGRATION_IDENTIFIER;
        val deviceKey = IntegrationConstants.formatIntegrationDeviceKey(integrationId, identifier);
        val entities = LavatoryTslUtils.thingSpecificationToEntities(integrationId, deviceKey, thingSpec);
        addAdditionalEntities(integrationId, deviceKey, entities);

        Map<String, Object> additional = Map.of(
                LavatoryIntegrationConstants.DeviceAdditionalDataName.DEVICE_ID, deviceId,
                LavatoryIntegrationConstants.DeviceAdditionalDataName.FLOOR, floor
        );
        val device = new DeviceBuilder(integrationId)
                .name(deviceName)
                .identifier(identifier)
                .additional(additional)
                .entities(entities)
                .build();
        deviceServiceProvider.save(device);
        return device;
    }

    public Device updateLocalDevice(String identifier, String deviceId, ThingSpec thingSpec) {
        val integrationId = LavatoryIntegrationConstants.INTEGRATION_IDENTIFIER;
        val deviceKey = IntegrationConstants.formatIntegrationDeviceKey(integrationId, identifier);
        val entities = LavatoryTslUtils.thingSpecificationToEntities(integrationId, deviceKey, thingSpec);
        addAdditionalEntities(integrationId, deviceKey, entities);

        val device = deviceServiceProvider.findByIdentifier(identifier, integrationId);
        // update device attributes except name
//        device.setIdentifier(identifier);
        Map<String, Object> additional = device.getAdditional();
        additional.put(LavatoryIntegrationConstants.DeviceAdditionalDataName.DEVICE_ID, deviceId);
        device.setAdditional(additional);
        device.setEntities(entities);
        deviceServiceProvider.save(device);
        return device;
    }

    @Nullable
    public ThingSpec getThingSpec(String deviceId) throws IOException, MscSdkException {
        val mscClient = lavatoryClientProvider.getMscClient();
        ThingSpec thingSpec = null;
        val response = mscClient.device()
                .getThingSpecification(deviceId)
                .execute()
                .body();
        if (response != null && response.getData() != null) {
            thingSpec = response.getData();
        }
        return thingSpec;
    }

    private static void addAdditionalEntities(String integrationId, String deviceKey, List<Entity> entities) {
        entities.add(new EntityBuilder(integrationId, deviceKey)
                .identifier(LavatoryIntegrationConstants.InternalPropertyIdentifier.LAST_SYNC_TIME)
                .property(LavatoryIntegrationConstants.InternalPropertyIdentifier.LAST_SYNC_TIME, AccessMod.R)
                .valueType(EntityValueType.LONG)
                .attributes(Map.of("internal", true))
                .build());
    }

    @SneakyThrows
    @EventSubscribe(payloadKeyExpression = "lavatory.integration.delete_device", eventType = ExchangeEvent.EventType.DOWN)
    public void onDeleteDevice(Event<LavatoryServiceEntities.DeleteDevice> event) {
        if (lavatoryClientProvider == null || lavatoryClientProvider.getMscClient() == null) {
            log.warn("MscClient not initiated.");
            return;
        }
        val device = deviceServiceProvider.findByIdentifier(
                ((Device) event.getPayload().getContext("device")).getIdentifier(), LavatoryIntegrationConstants.INTEGRATION_IDENTIFIER);
        val additionalData = device.getAdditional();
        if (additionalData == null) {
            return;
        }
        val deviceId = additionalData.get(LavatoryIntegrationConstants.DeviceAdditionalDataName.DEVICE_ID);
        if (deviceId == null) {
            return;
        }
        try {
            lavatoryClientProvider.getMscClient().device().delete(deviceId.toString())
                    .execute();
        } catch (MscApiException e) {
            if (!"device_not_found".equals(e.getErrorResponse().getErrCode())) {
                throw e;
            } else {
                log.warn("Device '{}' ({}) not found in MSC", device.getIdentifier(), deviceId);
            }
        }
        deviceServiceProvider.deleteById(device.getId());
    }

}
