package com.milesight.beaveriot.integration.msc.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.constants.IntegrationConstants;
import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.DeviceBuilder;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.context.integration.model.EntityBuilder;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.integration.model.event.ExchangeEvent;
import com.milesight.beaveriot.eventbus.annotations.EventSubscribe;
import com.milesight.beaveriot.eventbus.api.Event;
import com.milesight.beaveriot.integration.msc.constant.MscErrorCode;
import com.milesight.beaveriot.integration.msc.constant.MscIntegrationConstants;
import com.milesight.beaveriot.integration.msc.entity.MscServiceEntities;
import com.milesight.beaveriot.integration.msc.util.MscTslUtils;
import com.milesight.cloud.sdk.client.model.DeviceInfoResponse;
import com.milesight.cloud.sdk.client.model.DeviceSaveOrUpdateRequest;
import com.milesight.cloud.sdk.client.model.GenericResponseBodyDeviceInfoResponse;
import com.milesight.cloud.sdk.client.model.ThingSpec;
import com.milesight.cloud.sdk.client.model.TslPropertyDataUpdateRequest;
import com.milesight.cloud.sdk.client.model.TslServiceCallRequest;
import com.milesight.msc.sdk.error.MscApiException;
import com.milesight.msc.sdk.error.MscSdkException;
import lombok.*;
import lombok.extern.slf4j.*;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Slf4j
@Service
public class MscDeviceService {

    @Lazy
    @Autowired
    private IMscClientProvider mscClientProvider;

    @Autowired
    private DeviceServiceProvider deviceServiceProvider;

    @SneakyThrows
    @EventSubscribe(payloadKeyExpression = "msc-integration.device.*", eventType = {
            ExchangeEvent.EventType.CALL_SERVICE, ExchangeEvent.EventType.UPDATE_PROPERTY})
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

        publishPropertiesPayload(device, exchangePayload);
        publishServicePayload(device, exchangePayload);
    }

    private void publishServicePayload(Device device, ExchangePayload exchangePayload) {
        val objectMapper = mscClientProvider.getMscClient().getObjectMapper();
        val servicePayload = exchangePayload.getPayloadsByEntityType(EntityType.SERVICE);
        if (servicePayload.isEmpty()) {
            return;
        }
        val deviceId = (String) device.getAdditional().get(MscIntegrationConstants.DeviceAdditionalDataName.DEVICE_ID);
        val serviceGroups = MscTslUtils.convertExchangePayloadToGroupedJsonNode(
                objectMapper, device.getKey(), servicePayload);
        serviceGroups.entrySet().removeIf(entry -> MscIntegrationConstants.InternalPropertyIdentifier.Pattern.match(entry.getKey()));
        if (serviceGroups.isEmpty()) {
            return;
        }
        serviceGroups.forEach((serviceId, serviceProperties) -> callService(deviceId, serviceId, serviceProperties));
    }

    @SneakyThrows
    private void callService(String deviceId, String serviceId, JsonNode serviceProperties) {
        mscClientProvider.getMscClient()
                .device()
                .callService(deviceId, TslServiceCallRequest.builder()
                        .serviceId(serviceId)
                        .inputs(serviceProperties)
                        .build())
                .execute();
    }

    @SneakyThrows
    private void publishPropertiesPayload(Device device, ExchangePayload exchangePayload) {
        val objectMapper = mscClientProvider.getMscClient().getObjectMapper();
        val propertiesPayload = exchangePayload.getPayloadsByEntityType(EntityType.PROPERTY);
        if (propertiesPayload.isEmpty()) {
            return;
        }
        val properties = MscTslUtils.convertExchangePayloadToGroupedJsonNode(
                objectMapper, device.getKey(), propertiesPayload);
        properties.entrySet().removeIf(entry -> MscIntegrationConstants.InternalPropertyIdentifier.Pattern.match(entry.getKey()));
        if (properties.isEmpty()) {
            return;
        }
        val deviceId = (String) device.getAdditional().get(MscIntegrationConstants.DeviceAdditionalDataName.DEVICE_ID);
        mscClientProvider.getMscClient().device().updateProperties(deviceId, TslPropertyDataUpdateRequest.builder()
                        .properties(properties)
                        .build())
                .execute();
    }

    @SneakyThrows
    @EventSubscribe(payloadKeyExpression = "msc-integration.integration.add_device.*")
    public void onAddDevice(Event<MscServiceEntities.AddDevice> event) {
        if (mscClientProvider == null || mscClientProvider.getMscClient() == null) {
            throw ServiceException
                    .with(ErrorCode.SERVER_ERROR.getErrorCode(), "Integration has not been initialized yet.")
                    .build();
        }

        val deviceName = event.getPayload().getAddDeviceName();
        val identifier = event.getPayload().getSn().toUpperCase();
        val mscClient = mscClientProvider.getMscClient();

        try {
            val addDeviceResponse = mscClient.device()
                    .attach(DeviceSaveOrUpdateRequest.builder()
                            .name(deviceName)
                            .snDevEUI(identifier)
                            .autoProvision(false)
                            .build())
                    .execute()
                    .body();

            val deviceId = Optional.ofNullable(addDeviceResponse)
                    .map(GenericResponseBodyDeviceInfoResponse::getData)
                    .map(DeviceInfoResponse::getDeviceId)
                    .orElse(null);
            log.info("Device '{}' added to MSC with id '{}'", deviceName, deviceId);

            final String deviceIdStr = String.valueOf(deviceId);
            val thingSpec = getThingSpec(deviceIdStr);

            addLocalDevice(identifier, deviceName, deviceIdStr, thingSpec);
        } catch (MscSdkException e) {
            log.warn("Add device failed: '{}' '{}'", deviceName, identifier);
            throw MscErrorCode.wrap(e).build();
        }
    }

    public Device addLocalDevice(String identifier, String deviceName, String deviceId, ThingSpec thingSpec) {
        val integrationId = MscIntegrationConstants.INTEGRATION_IDENTIFIER;
        val deviceKey = IntegrationConstants.formatIntegrationDeviceKey(integrationId, identifier);
        val entities = MscTslUtils.thingSpecificationToEntities(integrationId, deviceKey, thingSpec);
        addAdditionalEntities(integrationId, deviceKey, entities);

        val device = new DeviceBuilder(integrationId)
                .name(deviceName)
                .identifier(identifier)
                .additional(Map.of(MscIntegrationConstants.DeviceAdditionalDataName.DEVICE_ID, deviceId))
                .entities(entities)
                .build();
        deviceServiceProvider.save(device);
        return device;
    }

    public Device updateLocalDevice(String identifier, String deviceId, ThingSpec thingSpec) {
        val integrationId = MscIntegrationConstants.INTEGRATION_IDENTIFIER;
        val deviceKey = IntegrationConstants.formatIntegrationDeviceKey(integrationId, identifier);
        val entities = MscTslUtils.thingSpecificationToEntities(integrationId, deviceKey, thingSpec);
        addAdditionalEntities(integrationId, deviceKey, entities);

        val device = deviceServiceProvider.findByIdentifier(identifier, integrationId);

        // keep entity name
        val keyToUpdatedEntity = entities.stream().collect(Collectors.toMap(Entity::getKey, Function.identity(), (a, b) -> a));
        device.getEntities().stream()
                .flatMap(existingEntity -> Optional.ofNullable(existingEntity.getChildren())
                        .map(Collection::stream)
                        .map(childrenStream -> Stream.concat(Stream.of(existingEntity), childrenStream))
                        .orElseGet(() -> Stream.of(existingEntity)))
                .forEach(existingEntity -> {
                    val updatedEntity = keyToUpdatedEntity.get(existingEntity.getKey());
                    if (updatedEntity != null) {
                        updatedEntity.setName(existingEntity.getName());
                    }
                });

        // update device attributes except name
        device.setAdditional(Map.of(MscIntegrationConstants.DeviceAdditionalDataName.DEVICE_ID, deviceId));
        device.setEntities(entities);
        deviceServiceProvider.save(device);
        return device;
    }

    @Nullable
    public ThingSpec getThingSpec(String deviceId) throws IOException, MscSdkException {
        val mscClient = mscClientProvider.getMscClient();
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
                .identifier(MscIntegrationConstants.InternalPropertyIdentifier.LAST_SYNC_TIME)
                .property(MscIntegrationConstants.InternalPropertyIdentifier.LAST_SYNC_TIME, AccessMod.R)
                .valueType(EntityValueType.LONG)
                .attributes(Map.of("internal", true))
                .visible(false)
                .build());
    }

    @SneakyThrows
    @EventSubscribe(payloadKeyExpression = "msc-integration.integration.delete_device")
    public void onDeleteDevice(Event<MscServiceEntities.DeleteDevice> event) {
        if (mscClientProvider == null || mscClientProvider.getMscClient() == null) {
            throw ServiceException
                    .with(ErrorCode.SERVER_ERROR.getErrorCode(), "Integration has not been initialized yet.")
                    .build();
        }
        val device = deviceServiceProvider.findByIdentifier(
                event.getPayload().getDeletedDevice().getIdentifier(), MscIntegrationConstants.INTEGRATION_IDENTIFIER);
        val additionalData = device.getAdditional();
        if (additionalData == null) {
            return;
        }
        val deviceId = additionalData.get(MscIntegrationConstants.DeviceAdditionalDataName.DEVICE_ID);
        if (deviceId == null) {
            return;
        }
        try {
            mscClientProvider.getMscClient().device().delete(deviceId.toString())
                    .execute();
        } catch (MscApiException e) {
            if (!"device_not_found".equals(e.getErrorResponse().getErrCode())) {
                throw MscErrorCode.wrap(e).build();
            } else {
                log.warn("Device '{}' ({}) not found in MSC", device.getIdentifier(), deviceId);
            }
        }
        deviceServiceProvider.deleteById(device.getId());
    }

}
