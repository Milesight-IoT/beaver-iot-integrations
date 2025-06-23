package com.milesight.beaveriot.integrations.aiinference.controller;

import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.response.ResponseBody;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.api.IntegrationServiceProvider;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.integration.model.Integration;
import com.milesight.beaveriot.integrations.aiinference.api.enums.ServerErrorCode;
import com.milesight.beaveriot.integrations.aiinference.api.model.response.CamThinkModelDetailResponse;
import com.milesight.beaveriot.integrations.aiinference.constant.Constants;
import com.milesight.beaveriot.integrations.aiinference.model.InferHistory;
import com.milesight.beaveriot.integrations.aiinference.model.request.BoundDeviceSearchRequest;
import com.milesight.beaveriot.integrations.aiinference.model.request.DeviceBindRequest;
import com.milesight.beaveriot.integrations.aiinference.model.request.DeviceSearchRequest;
import com.milesight.beaveriot.integrations.aiinference.model.request.DeviceUnbindRequest;
import com.milesight.beaveriot.integrations.aiinference.model.response.*;
import com.milesight.beaveriot.integrations.aiinference.service.AiInferenceService;
import com.milesight.beaveriot.integrations.aiinference.support.DataCenter;
import com.milesight.beaveriot.integrations.aiinference.support.EntitySupport;
import com.milesight.beaveriot.integrations.aiinference.support.PageSupport;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * author: Luxb
 * create: 2025/5/30 16:13
 **/
@RestController
@RequestMapping("/" + Constants.INTEGRATION_ID)
public class AiInferenceController {
    private final IntegrationServiceProvider integrationServiceProvider;
    private final DeviceServiceProvider deviceServiceProvider;
    private final EntityServiceProvider entityServiceProvider;
    private final EntityValueServiceProvider entityValueServiceProvider;
    private final AiInferenceService service;

    public AiInferenceController(IntegrationServiceProvider integrationServiceProvider, DeviceServiceProvider deviceServiceProvider, EntityServiceProvider entityServiceProvider, EntityValueServiceProvider entityValueServiceProvider, AiInferenceService service) {
        this.integrationServiceProvider = integrationServiceProvider;
        this.deviceServiceProvider = deviceServiceProvider;
        this.entityServiceProvider = entityServiceProvider;
        this.entityValueServiceProvider = entityValueServiceProvider;
        this.service = service;
    }

    @PostMapping("/model/{modelId}/sync-detail")
    public ResponseBody<ModelOutputSchemaResponse> fetchModelDetail(@PathVariable("modelId") String modelId) {
        CamThinkModelDetailResponse camThinkModelDetailResponse = service.fetchModelDetail(modelId);
        if (camThinkModelDetailResponse == null) {
            throw ServiceException.with(ServerErrorCode.SERVER_DATA_NOT_FOUND.getErrorCode(), ServerErrorCode.SERVER_DATA_NOT_FOUND.getErrorMessage()).build();
        }
        ModelOutputSchemaResponse outputResponse = new ModelOutputSchemaResponse(camThinkModelDetailResponse);
        return ResponseBuilder.success(outputResponse);
    }

    @PostMapping("/device/search")
    public ResponseBody<DeviceResponse> searchDevice(@RequestBody DeviceSearchRequest deviceSearchRequest) {
        String searchName = deviceSearchRequest.getName();
        List<Integration> integrations = integrationServiceProvider.findIntegrations().stream().toList();
        List<Device> devices = new ArrayList<>();
        Map<String, String> integrationMap = new HashMap<>();
        integrations.forEach(integration -> {
            integrationMap.put(integration.getId(), integration.getName());
            List<Device> integrationDevices = deviceServiceProvider.findAll(integration.getId());
            if (!CollectionUtils.isEmpty(integrationDevices)) {
                List<Device> filteredDevices = integrationDevices.stream().filter(device -> filterDevice(device, searchName)).toList();
                if (!CollectionUtils.isEmpty(filteredDevices)) {
                    devices.addAll(filteredDevices);
                }
            }
        });
        List<DeviceData> deviceData = devices.stream().map(device -> new DeviceData(device.getId().toString(), device.getName(), device.getIntegrationId(), integrationMap.get(device.getIntegrationId()))).toList();
        return ResponseBuilder.success(DeviceResponse.build(deviceData));
    }

    @GetMapping("/device/{deviceId}/image-entities")
    public ResponseBody<DeviceImageEntityResponse> getDeviceImageEntities(@PathVariable("deviceId") String deviceId) {
        Device device = deviceServiceProvider.findById(Long.parseLong(deviceId));
        List<Entity> imageEntities = new ArrayList<>();
        if (device.getEntities() != null) {
            imageEntities = device.getEntities().stream().filter(this::filterImageEntity).toList();
        }

        List<DeviceImageEntityResponse.ImageEntityData> imageEntityDataList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(imageEntities)) {
            imageEntityDataList = imageEntities.stream().map(entity -> {
                DeviceImageEntityResponse.ImageEntityData imageEntityData = new DeviceImageEntityResponse.ImageEntityData();
                imageEntityData.setId(entity.getId().toString());
                imageEntityData.setKey(entity.getKey());
                imageEntityData.setName(entity.getName());
                imageEntityData.setFormat(getFormatValue(entity));

                Object value = entityValueServiceProvider.findValueByKey(entity.getKey());
                if (value != null) {
                    String stringValue = value.toString();
                    imageEntityData.setValue(stringValue);
                }
                return imageEntityData;
            }).toList();
        }

        return ResponseBuilder.success(DeviceImageEntityResponse.build(imageEntityDataList));
    }

    @PostMapping("/device/{deviceId}/bind")
    public ResponseBody<Void> deviceBind(@PathVariable("deviceId") String deviceId, @RequestBody DeviceBindRequest deviceBindRequest) {
        Device device = deviceServiceProvider.findById(Long.parseLong(deviceId));
        if (device == null) {
            throw ServiceException.with(ServerErrorCode.DEVICE_NOT_FOUND.getErrorCode(), ServerErrorCode.DEVICE_NOT_FOUND.getErrorMessage()).build();
        }

        String integrationId = device.getIntegrationId();
        String deviceKey = device.getKey();
        String modelId = deviceBindRequest.getModelId();

        Entity modelIdEntity = EntitySupport.buildStringEntity(integrationId, deviceKey, Constants.IDENTIFIER_MODEL_ID, "Model ID");
        entityServiceProvider.save(modelIdEntity);
        saveEntityValue(modelIdEntity.getKey(), modelId);

        List<Entity> modelEntityChildren = new ArrayList<>();
        Entity modelInferInputsEntity = EntitySupport.buildStringEntity(integrationId, deviceKey, Constants.IDENTIFIER_MODEL_INFER_INPUTS, "Model Infer Inputs");
        modelEntityChildren.add(modelInferInputsEntity);

        Entity inferHistoryEntity = EntitySupport.buildStringEntity(integrationId, deviceKey, Constants.IDENTIFIER_INFER_HISTORY, "Model Infer History");
        entityServiceProvider.save(inferHistoryEntity);

        if (CollectionUtils.isEmpty(deviceBindRequest.getInferOutputs())) {
            throw ServiceException.with(ServerErrorCode.DEVICE_BIND_ERROR.getErrorCode(), "Model infer outputs not found").build();
        }

        for (DeviceBindRequest.OutputItem outputItem : deviceBindRequest.getInferOutputs()) {
            Entity outputItemEntity = EntitySupport.buildStringEntity(integrationId, deviceKey, outputItem.getFieldName(), outputItem.getEntityName());
            modelEntityChildren.add(outputItemEntity);
        }
        Entity modelEntity = EntitySupport.buildStringEntity(integrationId, deviceKey, MessageFormat.format(Constants.IDENTIFIER_MODEL_FORMAT, modelId), "Model " + modelId);
        modelEntity.setChildren(modelEntityChildren);
        entityServiceProvider.save(modelEntity);
        saveEntityValue(modelInferInputsEntity.getKey(), JsonUtils.toJSON(deviceBindRequest.getInferInputs()));

        Entity bindAtEntity = EntitySupport.buildStringEntity(integrationId, deviceKey, Constants.IDENTIFIER_BIND_AT, "Bind At");
        entityServiceProvider.save(bindAtEntity);
        saveEntityValue(bindAtEntity.getKey(), System.currentTimeMillis() / 1000);

        DataCenter.putDeviceImageEntity(device.getId(), deviceBindRequest.getImageEntityKey());

        return ResponseBuilder.success();
    }

    @GetMapping("/device/{deviceId}/binding-detail")
    public ResponseBody<DeviceBindingDetailResponse> deviceBindingDetail(@PathVariable("deviceId") String deviceId) {
        Device device = deviceServiceProvider.findById(Long.parseLong(deviceId));
        if (device == null) {
            throw ServiceException.with(ServerErrorCode.DEVICE_NOT_FOUND.getErrorCode(), ServerErrorCode.DEVICE_NOT_FOUND.getErrorMessage()).build();
        }

        String deviceKey = device.getKey();
        DeviceBindingDetailResponse response = new DeviceBindingDetailResponse();
        String modelId = (String) entityValueServiceProvider.findValueByKey(EntitySupport.getDeviceEntityKey(deviceKey, Constants.IDENTIFIER_MODEL_ID));
        response.setModelId(modelId);

        String imageEntityKey = DataCenter.getImageEntityKeyByDeviceId(device.getId());
        response.setImageEntityKey(imageEntityKey);

        if (StringUtils.isEmpty(modelId)) {
            String modelIdentifier = MessageFormat.format(Constants.IDENTIFIER_MODEL_FORMAT, modelId);
            String inferInputs = (String) entityValueServiceProvider.findValueByKey(EntitySupport.getDeviceEntityChildrenKey(deviceKey, modelIdentifier, Constants.IDENTIFIER_MODEL_INFER_INPUTS));
            if (inferInputs != null) {
                response.setInferInputs(JsonUtils.toMap(inferInputs));
            }
            Entity modelEntity = entityServiceProvider.findByKey(EntitySupport.getDeviceEntityKey(deviceKey, modelIdentifier));
            if (modelEntity != null && modelEntity.getChildren() != null) {
                response.setInferOutputs(modelEntity.getChildren().stream().map(entity -> {
                    DeviceBindingDetailResponse.OutputItem outputItem = new DeviceBindingDetailResponse.OutputItem();
                    outputItem.setFieldName(entity.getIdentifier());
                    outputItem.setEntityName(entity.getName());
                    return outputItem;
                }).filter(outputItem -> !Constants.IDENTIFIER_MODEL_INFER_INPUTS.equals(outputItem.getFieldName())).collect(Collectors.toList()));
            }
        }

        return ResponseBuilder.success(response);
    }

    @PostMapping("/bound-device/search")
    public ResponseBody<Page<BoundDeviceData>> boundDeviceSearch(@RequestBody BoundDeviceSearchRequest boundDeviceSearchRequest) {
        String searchName = boundDeviceSearchRequest.getName();
        List<Integration> integrations = integrationServiceProvider.findIntegrations().stream().toList();
        List<Device> devices = new ArrayList<>();
        integrations.forEach(integration -> {
            List<Device> integrationDevices = deviceServiceProvider.findAll(integration.getId());
            if (!CollectionUtils.isEmpty(integrationDevices)) {
                List<Device> filteredDevices = integrationDevices.stream().filter(
                        device -> (StringUtils.isEmpty(searchName) || device.getName().contains(searchName)) && DataCenter.isDeviceInDeviceImageEntityMap(device.getId())
                ).toList();
                if (!CollectionUtils.isEmpty(filteredDevices)) {
                    devices.addAll(filteredDevices);
                }
            }
        });

        int total = devices.size();
        List<Device> pageDeviceList = PageSupport.toPageList(devices, boundDeviceSearchRequest.getPageNumber(), boundDeviceSearchRequest.getPageSize());
        Map<String, String> modelMap = service.getModelMap();
        List<BoundDeviceData> pageBoundDeviceDataList = pageDeviceList.stream().map(device -> convertToBoundDeviceData(device, modelMap)).toList();
        return ResponseBuilder.success(PageSupport.toPage(pageBoundDeviceDataList, boundDeviceSearchRequest.getPageNumber(), boundDeviceSearchRequest.getPageSize(), total));
    }

    @PostMapping("/device/unbind")
    public ResponseBody<Void> deviceUnbind(@RequestBody DeviceUnbindRequest deviceUnbindRequest) {
        List<String> deviceIds = deviceUnbindRequest.getDeviceIds();
        deviceIds.forEach(deviceId -> doUnbindDevice(Long.parseLong(deviceId)));
        return ResponseBuilder.success();
    }

    private void doUnbindDevice(Long deviceId) {
        DataCenter.removeDevice(deviceId);
        Device device = deviceServiceProvider.findById(deviceId);
        if (device == null) {
            return;
        }
        String deviceKey = device.getKey();

        String modelId = (String) entityValueServiceProvider.findValueByKey(EntitySupport.getDeviceEntityKey(deviceKey, Constants.IDENTIFIER_MODEL_ID));
        saveEntityValue(EntitySupport.getDeviceEntityKey(deviceKey, Constants.IDENTIFIER_MODEL_ID), "");
        entityServiceProvider.deleteByKey(EntitySupport.getDeviceEntityKey(deviceKey, MessageFormat.format(Constants.IDENTIFIER_MODEL_FORMAT, modelId)));
    }

    private BoundDeviceData convertToBoundDeviceData(Device device, Map<String, String> modelMap) {
        BoundDeviceData boundDeviceData = new BoundDeviceData();
        boundDeviceData.setDeviceId(device.getId().toString());
        boundDeviceData.setDeviceName(device.getName());

        String modelId = (String) entityValueServiceProvider.findValueByKey(EntitySupport.getDeviceEntityKey(device.getKey(), Constants.IDENTIFIER_MODEL_ID));
        boundDeviceData.setModelName(modelMap.get(modelId));

        String imageEntityKey = DataCenter.getImageEntityKeyByDeviceId(device.getId());
        String originImage = (String) entityValueServiceProvider.findValueByKey(imageEntityKey);
        boundDeviceData.setOriginImage(originImage);

        String inferHistoryKey = EntitySupport.getDeviceEntityKey(device.getKey(), Constants.IDENTIFIER_INFER_HISTORY);
        String inferHistoryJson = (String) entityValueServiceProvider.findValueByKey(inferHistoryKey);
        if (!StringUtils.isEmpty(inferHistoryJson)) {
            InferHistory inferHistory = JsonUtils.fromJSON(inferHistoryJson, InferHistory.class);
            boundDeviceData.fillInferHistory(inferHistory);
        }

        String bindAt = (String) entityValueServiceProvider.findValueByKey(EntitySupport.getDeviceEntityKey(device.getKey(), Constants.IDENTIFIER_BIND_AT));
        boundDeviceData.setCreateAt(bindAt == null ? null : Long.parseLong(bindAt));

        Entity inferHistoryEntity = entityServiceProvider.findByKey(inferHistoryKey);
        if (inferHistoryEntity != null) {
            boundDeviceData.setInferHistoryEntityId(inferHistoryEntity.getId().toString());
        }
        boundDeviceData.setInferHistoryEntityKey(inferHistoryKey);
        return boundDeviceData;
    }

    private void saveEntityValue(String entityKey, Object value) {
        ExchangePayload exchangePayload = ExchangePayload.create(Map.of(
                entityKey, value
        ));
        entityValueServiceProvider.saveValuesAndPublishSync(exchangePayload);
    }

    private boolean filterDevice(Device device, String searchName) {
        boolean isMatch;
        if (StringUtils.isEmpty(searchName)) {
            isMatch = true;
        } else {
            isMatch = device.getName().contains(searchName);
        }

        if (!isMatch) {
            return false;
        }

        if (DataCenter.isDeviceInDeviceImageEntityMap(device.getId())) {
            return false;
        }

        if (device.getEntities() != null) {
            for(Entity entity : device.getEntities()) {
                if (filterImageEntity(entity)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean filterImageEntity(Entity entity) {
        if (entity.getAttributes() == null) {
            return false;
        }
        if (!entity.getAttributes().containsKey(Constants.ATTRIBUTE_KEY_FORMAT)) {
            return false;
        }
        String formatValue = entity.getAttributes().get(Constants.ATTRIBUTE_KEY_FORMAT).toString();
        return Constants.ATTRIBUTE_FORMAT_IMAGE_SET.contains(formatValue);
    }

    private String getFormatValue(Entity entity) {
        if (entity.getAttributes() == null) {
            return "";
        }
        if (!entity.getAttributes().containsKey(Constants.ATTRIBUTE_KEY_FORMAT)) {
            return "";
        }
        return entity.getAttributes().get(Constants.ATTRIBUTE_KEY_FORMAT).toString();
    }
}
