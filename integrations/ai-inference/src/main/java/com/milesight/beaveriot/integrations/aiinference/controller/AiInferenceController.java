package com.milesight.beaveriot.integrations.aiinference.controller;

import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.response.ResponseBody;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.api.IntegrationServiceProvider;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.context.integration.model.Integration;
import com.milesight.beaveriot.integrations.aiinference.api.enums.ServerErrorCode;
import com.milesight.beaveriot.integrations.aiinference.api.model.response.CamThinkModelDetailResponse;
import com.milesight.beaveriot.integrations.aiinference.constant.Constants;
import com.milesight.beaveriot.integrations.aiinference.model.request.DeviceSearchRequest;
import com.milesight.beaveriot.integrations.aiinference.model.response.DeviceImageEntityResponse;
import com.milesight.beaveriot.integrations.aiinference.model.response.DeviceResponse;
import com.milesight.beaveriot.integrations.aiinference.model.response.ModelOutputSchemaResponse;
import com.milesight.beaveriot.integrations.aiinference.service.AiInferenceService;
import com.milesight.beaveriot.integrations.aiinference.support.DataCenter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * author: Luxb
 * create: 2025/5/30 16:13
 **/
@RestController
@RequestMapping("/" + Constants.INTEGRATION_ID)
public class AiInferenceController {
    private final IntegrationServiceProvider integrationServiceProvider;
    private final DeviceServiceProvider deviceServiceProvider;
    private final EntityValueServiceProvider entityValueServiceProvider;
    private final AiInferenceService service;

    public AiInferenceController(IntegrationServiceProvider integrationServiceProvider, DeviceServiceProvider deviceServiceProvider, EntityValueServiceProvider entityValueServiceProvider, AiInferenceService service) {
        this.integrationServiceProvider = integrationServiceProvider;
        this.deviceServiceProvider = deviceServiceProvider;
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
        integrations.forEach(integration -> {
            List<Device> integrationDevices = deviceServiceProvider.findAll(integration.getId());
            if (!CollectionUtils.isEmpty(integrationDevices)) {
                List<Device> filteredDevices = integrationDevices.stream().filter(device -> filterDevice(device, searchName)).toList();
                if (!CollectionUtils.isEmpty(filteredDevices)) {
                    devices.addAll(filteredDevices);
                }
            }
        });
        return ResponseBuilder.success(DeviceResponse.build(devices));
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
