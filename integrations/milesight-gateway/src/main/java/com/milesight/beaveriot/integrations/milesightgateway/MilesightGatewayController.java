package com.milesight.beaveriot.integrations.milesightgateway;

import com.fasterxml.jackson.core.type.TypeReference;
import com.milesight.beaveriot.base.response.ResponseBody;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.integration.model.AttributeBuilder;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.integrations.milesightgateway.entity.MsGwIntegrationEntities;
import com.milesight.beaveriot.integrations.milesightgateway.model.DeviceConnectStatus;
import com.milesight.beaveriot.integrations.milesightgateway.model.GatewayData;
import com.milesight.beaveriot.integrations.milesightgateway.model.request.*;
import com.milesight.beaveriot.integrations.milesightgateway.model.response.*;
import com.milesight.beaveriot.integrations.milesightgateway.service.DeviceService;
import com.milesight.beaveriot.integrations.milesightgateway.service.GatewayService;
import com.milesight.beaveriot.integrations.milesightgateway.service.MsGwEntityService;
import com.milesight.beaveriot.integrations.milesightgateway.service.SyncGatewayDeviceService;
import com.milesight.beaveriot.integrations.milesightgateway.util.Constants;
import com.milesight.beaveriot.integrations.milesightgateway.util.GatewayString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MilesightGatewayController class.
 *
 * @author simon
 * @date 2025/2/24
 */

@RestController
@RequestMapping("/" + Constants.INTEGRATION_ID)
public class MilesightGatewayController {
    @Autowired
    GatewayService gatewayService;

    @Autowired
    DeviceService deviceService;

    @Autowired
    MsGwEntityService msGwEntityService;

    @Autowired
    SyncGatewayDeviceService syncGatewayDeviceService;

    @Autowired
    EntityServiceProvider entityServiceProvider;

    @GetMapping("/gateways")
    public ResponseBody<GatewayListResponse> getGateways() {
        GatewayListResponse response = new GatewayListResponse();
        List<Device> gateways = gatewayService.getAllGateways();
        Map<String, List<String>> gatewayDeviceRelation = msGwEntityService.getGatewayRelation();
        if (ObjectUtils.isEmpty(gatewayDeviceRelation)) {
            return ResponseBuilder.success(response);
        }

        Map<String, DeviceConnectStatus> gatewayStatusMap = msGwEntityService.getGatewayStatus(gateways.stream().map(Device::getIdentifier).toList());
        response.setGateways(gateways.stream().map(gateway -> {
            GatewayListItem listItem = new GatewayListItem();
            listItem.setName(gateway.getName());
            listItem.setDeviceId(gateway.getId().toString());
            listItem.setDeviceKey(gateway.getKey());
            DeviceConnectStatus status = gatewayStatusMap.get(gateway.getIdentifier());
            listItem.setStatus(status == null ? DeviceConnectStatus.ONLINE : status);

            List<String> deviceEuiList = gatewayDeviceRelation.get(gatewayService.getGatewayEui(gateway));
            listItem.setDeviceCount(deviceEuiList == null ? 0 : deviceEuiList.size());

            listItem.setApplicationId(gatewayService.getGatewayApplicationId(gateway));
            String credentialId = gatewayService.getGatewayCredentialId(gateway);
            listItem.setCredentialId(credentialId);
            listItem.setEui(gatewayService.getGatewayEui(gateway));
            return listItem;
        }).toList());

        return ResponseBuilder.success(response);
    }

    @PostMapping("/validate-gateway-info")
    public ResponseBody<Void> validateGatewayInfo(@RequestBody ConnectionValidateRequest validateRequest) {
        gatewayService.validateGatewayInfo(validateRequest.getEui());
        return ResponseBuilder.success();
    }

    @PostMapping("/gateway-credential")
    public ResponseBody<MqttCredentialResponse> fetchGatewayCredential(@RequestBody FetchGatewayCredentialRequest request) {
        return ResponseBuilder.success(gatewayService.fetchCredential(request));
    }

    @PostMapping("/validate-connection")
    public ResponseBody<ConnectionValidateResponse> validateGatewayConnection(@RequestBody ConnectionValidateRequest validateRequest) {
        return ResponseBuilder.success(gatewayService.validateGatewayConnection(validateRequest.getEui(), validateRequest.getCredentialId()));
    }

    @PostMapping("/gateways")
    public ResponseBody<GatewayData> addGateway(@RequestBody AddGatewayRequest request) {
        return ResponseBuilder.success(gatewayService.addGateway(request));
    }

    @PostMapping("/batch-delete-gateways")
    public ResponseBody<Void> batchDeleteGateways(@RequestBody BatchDeleteGatewaysRequest request) {
        gatewayService.batchDeleteGateway(request.getGateways());
        return ResponseBuilder.success();
    }

    @GetMapping("/gateways/{gatewayEUI}/devices")
    public ResponseBody<List<GatewayDeviceListItem>> getGatewayDevices(@PathVariable("gatewayEUI") String eui) {
        List<String> deviceEuiList = msGwEntityService.getGatewayRelation().get(GatewayString.standardizeEUI(eui));
        if (deviceEuiList == null) {
            return ResponseBuilder.success(List.of());
        }

        return ResponseBuilder.success(deviceService.getDevices(deviceEuiList).stream().map(device -> {
            GatewayDeviceListItem item = new GatewayDeviceListItem();
            item.setId(device.getId().toString());
            item.setKey(device.getKey());
            item.setEui(device.getIdentifier());
            item.setName(device.getName());
            item.setCreatedAt(device.getCreatedAt());
            return item;
        }).toList());
    }

    @GetMapping("/gateways/{gatewayEUI}/sync-devices")
    public ResponseBody<List<SyncDeviceListItem>> getGatewayDeviceSyncList(@PathVariable("gatewayEUI") String eui) {
        return ResponseBuilder.success(syncGatewayDeviceService.getGatewayDeviceSyncList(GatewayString.standardizeEUI(eui)));
    }

    @PostMapping("/gateways/{gatewayEUI}/sync-devices")
    public ResponseBody<Void> syncGatewayDevice(@PathVariable("gatewayEUI") String eui, @RequestBody SyncGatewayDeviceRequest request) {
        syncGatewayDeviceService.syncGatewayDevice(GatewayString.standardizeEUI(eui), request);
        return ResponseBuilder.success();
    }

    @GetMapping("/device-models")
    public ResponseBody<Map<String, String>> getDeviceModels() {
        Entity deviceModelNameEntity = entityServiceProvider.findByKey(MsGwIntegrationEntities.ADD_DEVICE_GATEWAY_DEVICE_MODEL_KEY);
        Map<String, Object> attributes = deviceModelNameEntity.getAttributes();
        if (ObjectUtils.isEmpty(attributes)) {
            return ResponseBuilder.success(Map.of());
        }

        return ResponseBuilder.success(GatewayString.jsonInstance().convertValue(deviceModelNameEntity.getAttributes().get(AttributeBuilder.ATTRIBUTE_ENUM), new TypeReference<Map<String, String>>() {}));
    }
}
