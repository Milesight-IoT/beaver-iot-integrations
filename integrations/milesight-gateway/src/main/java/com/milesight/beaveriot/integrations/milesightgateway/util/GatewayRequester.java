package com.milesight.beaveriot.integrations.milesightgateway.util;

import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.integrations.milesightgateway.model.MilesightGatewayErrorCode;
import com.milesight.beaveriot.integrations.milesightgateway.model.api.AddDeviceRequest;
import com.milesight.beaveriot.integrations.milesightgateway.model.api.DeviceListItemFields;
import com.milesight.beaveriot.integrations.milesightgateway.model.api.DeviceListResponse;
import com.milesight.beaveriot.integrations.milesightgateway.mqtt.MsGwMqttClient;
import com.milesight.beaveriot.integrations.milesightgateway.mqtt.model.MqttRequest;
import com.milesight.beaveriot.integrations.milesightgateway.mqtt.model.MqttResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * MilesightGatewayRequestor class.
 *
 * @author simon
 * @date 2025/2/14
 */
@Component("milesightGatewayRequester")
@Slf4j
public class GatewayRequester {
    @Autowired
    MsGwMqttClient msGwMqttClient;

    private MqttRequest buildDeviceListRequest(int offset, int limit, String applicationId) {
        MqttRequest req = new MqttRequest();
        req.setMethod("GET");
        String url = "/api/urdevices?order=asc&offset=" + offset + "&limit=" + limit + "&type=default";
        if (applicationId != null) {
            url += "&applicationID=" + applicationId;
        }

        req.setUrl(url);
        return req;
    }

    public MqttResponse<DeviceListResponse> requestDeviceList(String gatewayEui, int offset, int limit, String applicationId) {
        MqttRequest req = buildDeviceListRequest(offset, limit, applicationId);
        MqttResponse<DeviceListResponse> response = msGwMqttClient.request(gatewayEui, req, DeviceListResponse.class);
        if (response.getErrorBody() != null) {
            throw ServiceException
                    .with(MilesightGatewayErrorCode.GATEWAY_RESPOND_ERROR)
                    .args(response.getErrorBody().toMap())
                    .build();
        }

        return response;
    }

    public Optional<Map<String, Object>> requestDeviceItemByEui(String gatewayId, String deviceEui, String applicationId) {
        MqttRequest req = new MqttRequest();
        req.setMethod("GET");
        req.setUrl("/api/urdevices?search=" + deviceEui + "&applicationID=" + applicationId);
        MqttResponse<DeviceListResponse> response = msGwMqttClient.request(gatewayId, req, DeviceListResponse.class);
        if (response.getErrorBody() != null) {
            throw ServiceException
                    .with(MilesightGatewayErrorCode.GATEWAY_RESPOND_ERROR)
                    .args(response.getErrorBody().toMap())
                    .build();
        }

        return response.getSuccessBody()
                .getDeviceResult().stream()
                .filter(item -> ((String) item.get(DeviceListItemFields.DEV_EUI)).equalsIgnoreCase(deviceEui))
                .findFirst();
    }

    /**
     * Update device item to gateway
     * @param gatewayId gateway id of the updated device
     * @param deviceEui id of the updated device
     * @param itemData data must be from `requestDeviceItemByEui`
     */
    public void requestUpdateDeviceItem(String gatewayId, String deviceEui, Map<String, Object> itemData) {
        MqttRequest req = new MqttRequest();
        req.setMethod("PUT");
        req.setUrl("/api/urdevices/" + deviceEui);
        req.setBody(itemData);
        MqttResponse<Void> response = msGwMqttClient.request(gatewayId, req, Void.class);
        if (response.getErrorBody() != null) {
            throw ServiceException
                    .with(MilesightGatewayErrorCode.GATEWAY_RESPOND_ERROR)
                    .args(response.getErrorBody().toMap())
                    .build();
        }
    }

    public void requestAddDevice(String gatewayId, AddDeviceRequest requestData) {
        MqttRequest req = new MqttRequest();
        req.setMethod("POST");
        req.setUrl("/api/urdevices");
        req.setBody(GatewayString.convertToMap(requestData));
        MqttResponse<Void> response = msGwMqttClient.request(gatewayId, req, null);
        if (response.getErrorBody() != null) {
            throw ServiceException
                    .with(MilesightGatewayErrorCode.GATEWAY_RESPOND_ERROR)
                    .args(Map.of("errors", List.of(response.getErrorBody().toMap())))
                    .build();
        }
    }

    public void requestDeleteDevice(String gatewayEui, List<String> deviceEuiList) {
        List<MqttRequest> reqList = deviceEuiList.stream().map(deviceEui -> {
            MqttRequest req = new MqttRequest();
            req.setMethod("DELETE");
            req.setUrl("/api/urdevices/" + deviceEui);
            return req;
        }).toList();

        List<MqttResponse<Void>> responses = msGwMqttClient.batchRequest(gatewayEui, reqList, Void.class);
        List<Map<String, Object>> errors = new ArrayList<>();
        responses.forEach(response -> {
            if (response.getErrorBody() != null) {
                if (response.getErrorBody().getCode().equals(5)) {
                    // The device has been removed from gateway
                    log.warn(response.getUrl() + " did not exists in gateway " + response.getGatewayEUI());
                    return;
                }

                errors.add(response.getErrorBody().toMap());
            }
        });

        if (!errors.isEmpty()) {
            throw ServiceException
                    .with(MilesightGatewayErrorCode.GATEWAY_RESPOND_ERROR)
                    .args(Map.of("errors", errors))
                    .build();
        }
    }

    private static final int DEVICE_GET_BATCH_SIZE = 50;

    public List<Map<String, Object>> requestAllDeviceList(String gatewayId, String applicationId) {
        DeviceListResponse initResponse = requestDeviceList(gatewayId, 0, DEVICE_GET_BATCH_SIZE, applicationId).getSuccessBody();
        if (initResponse.getDevTotalCount() == 0) {
            return List.of();
        }

        List<Map<String, Object>> result = new ArrayList<>(initResponse.getDeviceResult());

        List<MqttRequest> reqList = new ArrayList<>();
        for (int offset = DEVICE_GET_BATCH_SIZE; offset < initResponse.getDevTotalCount(); offset += DEVICE_GET_BATCH_SIZE) {
            reqList.add(buildDeviceListRequest(offset, DEVICE_GET_BATCH_SIZE, applicationId));
        }

        List<MqttResponse<DeviceListResponse>> restResponses = msGwMqttClient.batchRequest(gatewayId, reqList, DeviceListResponse.class);
        restResponses.forEach(response -> {
            if (response.getSuccessBody() != null) {
                result.addAll(response.getSuccessBody().getDeviceResult());
            }
        });

        return result;
    }

    public void downlink(String gatewayEui, String deviceEui, Integer fPort, String data) {
        msGwMqttClient.downlink(gatewayEui, deviceEui, fPort, data);
    }
}
