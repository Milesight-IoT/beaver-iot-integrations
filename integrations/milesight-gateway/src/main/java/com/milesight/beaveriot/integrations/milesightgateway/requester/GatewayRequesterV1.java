package com.milesight.beaveriot.integrations.milesightgateway.requester;

import com.milesight.beaveriot.base.error.ErrorHolder;
import com.milesight.beaveriot.base.exception.ErrorCodeSpec;
import com.milesight.beaveriot.base.exception.MultipleErrorException;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.integrations.milesightgateway.model.MilesightGatewayErrorCode;
import com.milesight.beaveriot.integrations.milesightgateway.model.api.AddDeviceRequest;
import com.milesight.beaveriot.integrations.milesightgateway.model.api.DeviceListItemFields;
import com.milesight.beaveriot.integrations.milesightgateway.model.api.DeviceListResponse;
import com.milesight.beaveriot.integrations.milesightgateway.mqtt.MsGwMqttClient;
import com.milesight.beaveriot.integrations.milesightgateway.mqtt.MsGwMqttUtil;
import com.milesight.beaveriot.integrations.milesightgateway.mqtt.model.MqttRequest;
import com.milesight.beaveriot.integrations.milesightgateway.mqtt.model.MqttRequestError;
import com.milesight.beaveriot.integrations.milesightgateway.mqtt.model.MqttResponse;
import com.milesight.beaveriot.integrations.milesightgateway.util.Constants;
import com.milesight.beaveriot.integrations.milesightgateway.util.GatewayString;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * GatewayRequesterV1 class.
 * Suitable for UG65 / UG67 Series
 *
 * @author simon
 * @date 2025/10/30
 */
@Slf4j
public class GatewayRequesterV1 implements GatewayRequester {
    private final MsGwMqttClient msGwMqttClient;

    private final String eui;

    private final String applicationId;

    private final boolean compatibleMode;

    public GatewayRequesterV1(MsGwMqttClient msGwMqttClient, String eui, String applicationId, boolean compatibleMode) {
        this.msGwMqttClient = msGwMqttClient;
        this.eui = GatewayString.standardizeEUI(eui);
        this.applicationId = applicationId;
        this.compatibleMode = compatibleMode;
    }

    @Override
    public String getVersion() {
        return Constants.GATEWAY_VERSION_V1;
    }

    @Override
    public String getGatewayEui() { return this.eui; }

    private MqttRequest buildDeviceListRequest(int offset, int limit) {
        MqttRequest req = new MqttRequest();
        req.setMethod("GET");
        String url = "/api/urdevices?order=asc&offset=" + offset + "&limit=" + limit + "&type=default";
        if (this.applicationId != null) {
            url += "&applicationID=" + this.applicationId;
        }

        req.setUrl(url);
        return req;
    }

    @Override
    public MqttResponse<DeviceListResponse> requestDeviceList(int offset, int limit) {
        MqttRequest req = buildDeviceListRequest(offset, limit);
        MqttResponse<DeviceListResponse> response = msGwMqttClient.request(this.eui, req, DeviceListResponse.class);
        if (response.getErrorBody() != null) {
            throw ServiceException
                    .with(MilesightGatewayErrorCode.GATEWAY_RESPOND_ERROR)
                    .args(response.getErrorBody().toMap())
                    .build();
        }

        return response;
    }

    @Override
    public Optional<Map<String, Object>> requestDeviceItemByEui(String deviceEui) {
        MqttRequest req = new MqttRequest();
        req.setMethod("GET");
        req.setUrl("/api/urdevices?search=" + deviceEui + "&applicationID=" + this.applicationId);
        MqttResponse<DeviceListResponse> response = msGwMqttClient.request(this.eui, req, DeviceListResponse.class);
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
     * @param deviceEui id of the updated device
     * @param itemData data must be from `requestDeviceItemByEui`
     */
    @Override
    public void requestUpdateDeviceItem(String deviceEui, Map<String, Object> itemData) {
        MqttRequest req = new MqttRequest();
        req.setMethod("PUT");
        req.setUrl("/api/urdevices/" + deviceEui);
        req.setBody(itemData);
        MqttResponse<Void> response = msGwMqttClient.request(this.eui, req, Void.class);
        if (response.getErrorBody() != null) {
            throw ServiceException
                    .with(MilesightGatewayErrorCode.GATEWAY_RESPOND_ERROR)
                    .args(response.getErrorBody().toMap())
                    .build();
        }
    }

    @Override
    public void requestAddDevice(AddDeviceRequest requestData) {
        MqttRequest req = new MqttRequest();
        req.setMethod("POST");
        req.setUrl("/api/urdevices");
        req.setBody(GatewayString.convertToMap(requestData));
        MqttResponse<Void> response = msGwMqttClient.request(this.eui, req, null);
        MqttRequestError errorBody = response.getErrorBody();
        if (errorBody != null) {
            ErrorCodeSpec codeSpec = MilesightGatewayErrorCode.GATEWAY_RESPOND_ERROR;
            if (errorBody.getCode().equals(6)) {
                codeSpec = MilesightGatewayErrorCode.DUPLICATED_DEVICE_ON_GATEWAY;
            }

            throw ServiceException
                    .with(codeSpec)
                    .args(errorBody.toMap())
                    .build();
        }
    }

    @Override
    public void requestDeleteDeviceAsync(List<String> deviceEuiList) {
        deviceEuiList.forEach(deviceEui -> {
            MqttRequest req = new MqttRequest();
            req.setMethod("DELETE");
            req.setUrl("/api/urdevices/" + deviceEui);
            msGwMqttClient.requestWithoutResponse(this.eui, req);
        });
    }

    private void requestDeleteDevice(List<String> deviceEuiList) {
        this.requestBase();
        List<MqttRequest> reqList = deviceEuiList.stream().map(deviceEui -> {
            MqttRequest req = new MqttRequest();
            req.setMethod("DELETE");
            req.setUrl("/api/urdevices/" + deviceEui);
            return req;
        }).toList();

        List<MqttResponse<Void>> responses = msGwMqttClient.batchRequest(this.eui, reqList, Void.class);
        List<Map<String, Object>> errors = new ArrayList<>();
        responses.forEach(response -> {
            if (response.getErrorBody() != null) {
                if (response.getErrorBody().getCode().equals(5)) {
                    // The device has been removed from gateway
                    log.warn(response.getUrl() + " did not exists in gateway " + this.eui);
                    return;
                }

                errors.add(response.getErrorBody().toMap());
            }
        });

        if (!errors.isEmpty()) {
            throw MultipleErrorException.with(
                    MilesightGatewayErrorCode.GATEWAY_RESPOND_ERROR.getErrorMessage(),
                    ErrorHolder.of(errors.stream().map(error -> ServiceException
                            .with(MilesightGatewayErrorCode.GATEWAY_RESPOND_ERROR)
                            .args(error)
                            .build()).collect(Collectors.toList()))
            );
        }
    }

    private static final int DEVICE_GET_BATCH_SIZE = 50;

    @Override
    public List<Map<String, Object>> requestAllDeviceList() {
        DeviceListResponse initResponse = requestDeviceList(0, DEVICE_GET_BATCH_SIZE).getSuccessBody();
        if (initResponse.getDevTotalCount() == 0) {
            return List.of();
        }

        List<Map<String, Object>> result = new ArrayList<>(initResponse.getDeviceResult());

        List<MqttRequest> reqList = new ArrayList<>();
        for (int offset = DEVICE_GET_BATCH_SIZE; offset < initResponse.getDevTotalCount(); offset += DEVICE_GET_BATCH_SIZE) {
            reqList.add(buildDeviceListRequest(offset, DEVICE_GET_BATCH_SIZE));
        }

        List<MqttResponse<DeviceListResponse>> restResponses = msGwMqttClient.batchRequest(this.eui, reqList, DeviceListResponse.class);
        restResponses.forEach(response -> {
            if (response.getSuccessBody() != null) {
                result.addAll(response.getSuccessBody().getDeviceResult());
            }
        });

        return result;
    }

    @Override
    public void downlink(String nodeDeviceEui, Integer fPort, String data) {
        Map<String, Object> downlinkPayload = new HashMap<>();
        downlinkPayload.put("fPort", fPort);
        downlinkPayload.put("data", data);
        downlinkPayload.put("confirmed", false);
        if (compatibleMode) {
            downlinkPayload.put("devEUI", nodeDeviceEui);
            msGwMqttClient.downlink(MsGwMqttUtil.getDownlinkTopic(this.eui, null), downlinkPayload);
            return;
        }

        msGwMqttClient.downlink(MsGwMqttUtil.getDownlinkTopic(this.eui, nodeDeviceEui), downlinkPayload);
    }

    @Override
    public void detect() {
        MqttRequest req = buildDeviceListRequest(0, 1);
        msGwMqttClient.requestWithoutResponse(this.eui, req);
    }
}
