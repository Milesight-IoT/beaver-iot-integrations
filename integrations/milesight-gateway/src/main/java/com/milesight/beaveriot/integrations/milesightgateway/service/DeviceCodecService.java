package com.milesight.beaveriot.integrations.milesightgateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.milesight.beaveriot.base.annotations.shedlock.DistributedLock;
import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.integration.model.AttributeBuilder;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.context.integration.model.event.ExchangeEvent;
import com.milesight.beaveriot.eventbus.annotations.EventSubscribe;
import com.milesight.beaveriot.eventbus.api.Event;
import com.milesight.beaveriot.integrations.milesightgateway.entity.MsGwIntegrationEntities;
import com.milesight.beaveriot.integrations.milesightgateway.codec.model.*;
import com.milesight.beaveriot.integrations.milesightgateway.model.DeviceCodecData;
import com.milesight.beaveriot.integrations.milesightgateway.model.DeviceModelData;
import com.milesight.beaveriot.integrations.milesightgateway.codec.ResourceRequester;
import com.milesight.beaveriot.integrations.milesightgateway.codec.ResourceString;
import com.milesight.beaveriot.integrations.milesightgateway.util.LockConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * DeviceCodecService class.
 *
 * @author simon
 * @date 2025/2/25
 */
@Component("milesightGatewayDeviceCodecService")
@Slf4j
public class DeviceCodecService {
    @Autowired
    EntityServiceProvider entityServiceProvider;

    @Autowired
    MsGwEntityService msGwEntityService;

    ObjectMapper json = ResourceString.jsonInstance();

    @EventSubscribe(payloadKeyExpression = MsGwIntegrationEntities.SYNC_DEVICE_CODEC_KEY, eventType = ExchangeEvent.EventType.CALL_SERVICE)
    public void onSyncDeviceCodec(Event<MsGwIntegrationEntities> event) throws ExecutionException, InterruptedException {
        syncDeviceCodec();
    }

    @EventSubscribe(payloadKeyExpression = MsGwIntegrationEntities.MODEL_REPO_URL_KEY, eventType = ExchangeEvent.EventType.UPDATE_PROPERTY)
    public void onUpdateRepoUrl(Event<MsGwIntegrationEntities> event) throws ExecutionException, InterruptedException {
        self().syncDeviceCodec(event.getPayload().getModelRepoUrl());
    }

    public void syncDeviceCodec() throws ExecutionException, InterruptedException {
        self().syncDeviceCodec(msGwEntityService.getDeviceModelRepoUrl());
    }

    @DistributedLock(name = LockConstants.DEVICE_CODEC_INDEX_UPDATE_LOCK)
    public void syncDeviceCodec(String url) throws ExecutionException, InterruptedException {
        ResourceRequester resourceRequester = new ResourceRequester(url);
        VersionResponse versionInfo = resourceRequester.requestCodecVersion();
        DeviceModelData modelData = msGwEntityService.getDeviceModelData();
        // Check whether version updated
        if (Objects.equals(modelData.getVersion(), versionInfo.getVersion()) && Objects.equals(modelData.getSource(), resourceRequester.getRepoUrl())) {
            log.info("Ignore update. Have been in the latest version: " + versionInfo.getVersion());
            return;
        }

        List<Vendor> vendors = resourceRequester.requestCodecVendors(versionInfo.getVendors());
        List<CompletableFuture<Map.Entry<String, List<DeviceModelData.DeviceInfo>>>> futures = vendors.stream().map(vendor -> CompletableFuture.supplyAsync(() -> {
            String vendorId = vendor.getId();
            DeviceResourceResponse response = resourceRequester.requestVendorDevices(vendor.getDevices());
            List<DeviceModelData.DeviceInfo> vendorDevices = response.getDevices().stream().map((deviceResourceInfo -> {
                DeviceModelData.DeviceInfo vendorDevice = new DeviceModelData.DeviceInfo();
                vendorDevice.setId(deviceResourceInfo.getId());
                vendorDevice.setName(deviceResourceInfo.getName());
                return vendorDevice;
            })).toList();
            return Map.entry(vendorId, vendorDevices);
        })).toList();

        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        CompletableFuture<Map<String, List<DeviceModelData.DeviceInfo>>> allResults = allOf.thenApply(v ->
                futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

        saveDeviceCodecsToEntity(versionInfo.getVersion(), resourceRequester.getRepoUrl(), vendors, allResults.get());
    }

    private void saveDeviceCodecsToEntity(String version, String repoUrl, List<Vendor> vendors, Map<String, List<DeviceModelData.DeviceInfo>> vendorDevices) {
        // save to deviceModelData of integration entities
        DeviceModelData deviceModelData = new DeviceModelData();
        deviceModelData.setVersion(version);
        deviceModelData.setSource(repoUrl);
        deviceModelData.setVendorInfoList(vendors.stream().map(vendor -> {
            DeviceModelData.VendorInfo vendorInfo = new DeviceModelData.VendorInfo();
            vendorInfo.setId(vendor.getId());
            vendorInfo.setName(vendor.getName());
            vendorInfo.setDevices(vendor.getDevices());
            vendorInfo.setDeviceInfoList(vendorDevices.get(vendor.getId()));
            return vendorInfo;
        }).toList());
        msGwEntityService.saveDeviceModelData(deviceModelData);

        // save to deviceModel enum attribute in add device of integration entities
        syncDeviceModelListToAdd(deviceModelData);
    }

    public boolean isModelDataEmpty(DeviceModelData deviceModelData) {
        return deviceModelData == null
                ||deviceModelData.getVersion() == null
                ||deviceModelData.getVendorInfoList() == null;
    }

    public void syncDeviceModelListToAdd(DeviceModelData deviceModelData) {
        if (isModelDataEmpty(deviceModelData)) {
            return;
        }

        final Map<String, String> deviceIdToName = new LinkedHashMap<>();
        deviceModelData.getVendorInfoList()
                .forEach(vendorInfo -> vendorInfo.getDeviceInfoList()
                        .forEach(deviceInfo -> deviceIdToName.put(DeviceModelData.getDeviceModelId(vendorInfo, deviceInfo), DeviceModelData.getDeviceModelName(vendorInfo, deviceInfo))));

        Entity deviceModelNameEntity = entityServiceProvider.findByKey(MsGwIntegrationEntities.ADD_DEVICE_GATEWAY_DEVICE_MODEL_KEY);
        Map<String, Object> attributes = deviceModelNameEntity.getAttributes();
        if (attributes == null) {
            attributes = new HashMap<>();
        }

        attributes.put(AttributeBuilder.ATTRIBUTE_ENUM, deviceIdToName);
        deviceModelNameEntity.setAttributes(attributes);
        entityServiceProvider.save(deviceModelNameEntity);
    }

    public Map<String, DeviceCodecData> batchGetDeviceCodecData(List<String> vendorDeviceIdList) {
        ResourceRequester resourceRequester = new ResourceRequester(msGwEntityService.getDeviceModelRepoUrl());
        Set<String> deviceModelSet = new HashSet<>(vendorDeviceIdList);
        Map<String, DeviceModelData.VendorDeviceInfo> deviceModelMap = new HashMap<>();
        Map<String, String> vendorResourceMap = new HashMap<>();
        msGwEntityService.getDeviceModelData().iterateWhen((vendorInfo, deviceInfo) -> {
            String deviceModelId = DeviceModelData.getDeviceModelId(vendorInfo, deviceInfo);
            if (deviceModelSet.remove(deviceModelId)) {
                deviceModelMap.put(deviceModelId, new DeviceModelData.VendorDeviceInfo(vendorInfo, deviceInfo));
                vendorResourceMap.put(vendorInfo.getId(), vendorInfo.getDevices());
            }

            return !deviceModelSet.isEmpty();
        });

        if (!deviceModelSet.isEmpty()) {
            // some device model not found. Maybe wrong model was passed or model index has not been updated to the latest version.
            throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED.getErrorCode(), "device model not found: " + deviceModelSet).build();
        }

        // get vendor device map
        List<CompletableFuture<Map.Entry<String, Map<String, DeviceResourceInfo>>>> vendorDevicesFutures = vendorResourceMap.entrySet().stream()
                .map(entry -> CompletableFuture.supplyAsync(() -> Map.entry(entry.getKey(), resourceRequester.requestVendorDevices(entry.getValue()).getDevices().stream().collect(Collectors.toMap(DeviceResourceInfo::getId, deviceResourceInfo -> deviceResourceInfo)))))
                .toList();
        Map<String, Map<String, DeviceResourceInfo>> vendorDevicesMap = CompletableFuture.allOf(vendorDevicesFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> vendorDevicesFutures.stream().map(CompletableFuture::join).toList())
                .join()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // get vendor device data
        List<CompletableFuture<Map.Entry<String, DeviceCodecData>>> vendorDeviceCodecFutures = deviceModelMap.entrySet().stream()
                .map(entry -> CompletableFuture.supplyAsync(() -> {
                    DeviceCodecData codecData = new DeviceCodecData();
                    DeviceModelData.VendorDeviceInfo vendorDeviceInfo = entry.getValue();
                    Map<String, DeviceResourceInfo> vendorDeviceResource = vendorDevicesMap.get(vendorDeviceInfo.getVendorId());
                    if (ObjectUtils.isEmpty(vendorDeviceResource)) {
                        throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED.getErrorCode(), "vendor data found: " + vendorDeviceInfo.getVendorId()).build();
                    }

                    DeviceResourceInfo resourceInfo = vendorDeviceResource.get(vendorDeviceInfo.getDeviceId());
                    if (ObjectUtils.isEmpty(resourceInfo)) {
                        throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED.getErrorCode(), "device data found: " + vendorDeviceInfo.getVendorId()).build();
                    }

                    codecData.setResourceInfo(resourceInfo);

                    CompletableFuture<String> decoderScriptFuture = CompletableFuture.supplyAsync(() -> resourceRequester.requestResourceAsString(resourceInfo.getDecoderScript()));
                    CompletableFuture<String> encoderScriptFuture = CompletableFuture.supplyAsync(() -> resourceRequester.requestResourceAsString(resourceInfo.getEncoderScript()));
                    CompletableFuture<DeviceDef> deviceDefFuture = CompletableFuture.supplyAsync(() -> resourceRequester.requestDeviceDef(resourceInfo.getCodec()));
                    codecData.setDecoderStr(decoderScriptFuture.join());
                    codecData.setEncoderStr(encoderScriptFuture.join());
                    codecData.setDef(deviceDefFuture.join());
                    return Map.entry(entry.getKey(), codecData);
                })).toList();

        return CompletableFuture.allOf(vendorDeviceCodecFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> vendorDeviceCodecFutures.stream().map(CompletableFuture::join).toList())
                .join()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private DeviceCodecService self() {
        return (DeviceCodecService) AopContext.currentProxy();
    }
}
