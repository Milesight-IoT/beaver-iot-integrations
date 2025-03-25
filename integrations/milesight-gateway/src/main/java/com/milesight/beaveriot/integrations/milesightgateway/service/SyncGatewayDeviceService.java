package com.milesight.beaveriot.integrations.milesightgateway.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.milesight.beaveriot.base.annotations.shedlock.DistributedLock;
import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.DeviceBuilder;
import com.milesight.beaveriot.context.integration.wrapper.EntityWrapper;
import com.milesight.beaveriot.context.security.TenantContext;
import com.milesight.beaveriot.integrations.milesightgateway.codec.DeviceHelper;
import com.milesight.beaveriot.integrations.milesightgateway.model.DeviceCodecData;
import com.milesight.beaveriot.integrations.milesightgateway.model.DeviceModelData;
import com.milesight.beaveriot.integrations.milesightgateway.model.GatewayDeviceData;
import com.milesight.beaveriot.integrations.milesightgateway.model.GatewayDeviceOperation;
import com.milesight.beaveriot.integrations.milesightgateway.model.api.DeviceListItemFields;
import com.milesight.beaveriot.integrations.milesightgateway.model.request.SyncGatewayDeviceRequest;
import com.milesight.beaveriot.integrations.milesightgateway.model.response.SyncDeviceListItem;
import com.milesight.beaveriot.integrations.milesightgateway.util.Constants;
import com.milesight.beaveriot.integrations.milesightgateway.util.GatewayRequester;
import com.milesight.beaveriot.integrations.milesightgateway.util.GatewayString;
import com.milesight.beaveriot.integrations.milesightgateway.util.LockConstants;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * SyncGatewayDeviceService class.
 *
 * @author simon
 * @date 2025/3/13
 */
@Component("milesightSyncGatewayDeviceService")
@Slf4j
public class SyncGatewayDeviceService {
    @Autowired
    GatewayRequester gatewayRequester;

    @Autowired
    GatewayService gatewayService;

    @Autowired
    DeviceService deviceService;

    @Autowired
    DeviceCodecService deviceCodecService;

    @Autowired
    DeviceServiceProvider deviceServiceProvider;

    @Autowired
    MsGwEntityService msGwEntityService;

    private static final ObjectMapper json = GatewayString.jsonInstance();

    private static final String NONE_CODEC_ID = "0";

    public List<SyncDeviceListItem> getGatewayDeviceSyncList(String gatewayEui) {
        Device gateway = gatewayService.getGatewayByEui(gatewayEui);
        if (gateway == null) {
            throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED.getErrorCode(), "gateway not found: " + gatewayEui).build();
        }

        List<Map<String, Object>> deviceDataMap = gatewayRequester.requestAllDeviceList(gatewayEui, gatewayService.getGatewayApplicationId(gateway));
        if (deviceDataMap.isEmpty()) {
            return List.of();
        }

        List<String> existedDeviceEui = deviceService.getGatewayDeviceRelation().get(gatewayEui);
        Set<String> existedDeviceEuiSet = new HashSet<>();
        if (!ObjectUtils.isEmpty(existedDeviceEui)) {
            existedDeviceEuiSet.addAll(existedDeviceEui);
        }

        DeviceModelData deviceModelData =  msGwEntityService.getModelData();
        if (deviceModelData.getVendorInfoList() == null) {
            throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED.getErrorCode(), "Please synchronize your codec repo first!").build();
        }

        Map<String, String> modelNameToId = deviceModelData.getVendorInfoList().stream()
                .flatMap(vendorInfo -> vendorInfo.getDeviceInfoList().stream().map(deviceInfo -> new DeviceModelData.VendorDeviceInfo(vendorInfo, deviceInfo)))
                .collect(Collectors.toMap(DeviceModelData.VendorDeviceInfo::getDeviceName, DeviceModelData::getDeviceModelId, (prev, cur) -> prev));

        return deviceDataMap.stream()
                .filter(deviceData -> {
                    String eui = (String) deviceData.get(DeviceListItemFields.DEV_EUI);
                    return !existedDeviceEuiSet.contains(eui);
                })
                .map(deviceData -> {
                    SyncDeviceListItem item = new SyncDeviceListItem();
                    item.setEui(GatewayString.standardizeEUI((String) deviceData.get(DeviceListItemFields.DEV_EUI)));
                    item.setName((String) deviceData.get(DeviceListItemFields.NAME));
                    String codecName = (String) deviceData.get(DeviceListItemFields.PAYLOAD_NAME);
                    if (codecName != null) {
                        item.setGuessModelId(modelNameToId.get(codecName));
                    }
                    return item;
                })
                .toList();
    }

    @Data
    private static class UpdateGatewayDeviceResponse {
        GatewayDeviceData deviceData;
        String deviceName;
    }

    @DistributedLock(name = LockConstants.SYNC_GATEWAY_DEVICE_LOCK)
    public void syncGatewayDevice(String gatewayEui, SyncGatewayDeviceRequest request) {
        // check connection of gateway. In case a large number of doomed-to-fail requests were sent.
        gatewayRequester.requestDeviceList(gatewayEui, 0, 0, null);

        Device gateway = gatewayService.getGatewayByEui(gatewayEui);
        String applicationId = gatewayService.getGatewayApplicationId(gateway);

        // batch reset device codec
        final String tenantId = TenantContext.getTenantId();
        List<CompletableFuture<UpdateGatewayDeviceResponse>> futures = request.getDevices().stream()
                .map(syncRequest -> CompletableFuture.supplyAsync(() -> {
                    TenantContext.setTenantId(tenantId);
                    Map<String, Object> deviceItemData = gatewayService.doUpdateGatewayDevice(gatewayEui, syncRequest.getEui(), applicationId, Map.of(
                                    DeviceListItemFields.PAYLOAD_CODEC_ID, NONE_CODEC_ID,
                                    DeviceListItemFields.PAYLOAD_NAME, ""
                            ));
                    UpdateGatewayDeviceResponse response = new UpdateGatewayDeviceResponse();
                    response.setDeviceName((String) deviceItemData.get(DeviceListItemFields.NAME));
                    GatewayDeviceData deviceData = new GatewayDeviceData();
                    deviceData.setEui(syncRequest.getEui());
                    deviceData.setGatewayEUI(gatewayEui);
                    deviceData.setDeviceModel(syncRequest.getModelId());
                    deviceData.setFPort(GatewayString.jsonInstance().convertValue(deviceItemData.get(DeviceListItemFields.F_PORT), Long.class));
                    deviceData.setAppKey((String) deviceItemData.get(DeviceListItemFields.APP_KEY));
                    deviceData.setFrameCounterValidation(!(Boolean) deviceItemData.get(DeviceListItemFields.SKIP_F_CNT_CHECK));
                    response.setDeviceData(deviceData);
                    return response;
                })).toList();
        List<UpdateGatewayDeviceResponse> deviceItemList = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenApply(v ->
                        futures.stream()
                                .map(CompletableFuture::join)
                                .toList()).join();

        // get codecs
        Map<String, DeviceCodecData> deviceCodecDataMap = deviceCodecService.batchGetDeviceCodecData(deviceItemList.stream().map(updateGatewayDeviceResponse -> updateGatewayDeviceResponse.getDeviceData().getDeviceModel()).toList());

        // save devices
        deviceItemList.forEach(deviceItem -> {
            GatewayDeviceData deviceData = deviceItem.getDeviceData();
            Device device = new DeviceBuilder(Constants.INTEGRATION_ID)
                    .name(deviceItem.getDeviceName())
                    .identifier(GatewayString.standardizeEUI(deviceData.getEui()))
                    .additional(json.convertValue(deviceData, new TypeReference<>() {}))
                    .build();
            DeviceCodecData codecData = deviceCodecDataMap.get(deviceData.getDeviceModel());
            DeviceHelper.UpdateResourceResult updateResourceResult = DeviceHelper.updateResourceInfo(device, codecData.getDef());
            // save device
            deviceService.manageGatewayDevices(deviceData.getGatewayEUI(), deviceData.getEui(), GatewayDeviceOperation.ADD);
            deviceServiceProvider.save(device);

            // save script
            new EntityWrapper(updateResourceResult.getDecoderEntity()).saveValue(codecData.getDecoderStr());
            new EntityWrapper(updateResourceResult.getEncoderEntity()).saveValue(codecData.getEncoderStr());
        });
    }
}
