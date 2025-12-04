package com.milesight.beaveriot.integrations.milesightgateway.requester;

import com.milesight.beaveriot.base.exception.ErrorCodeSpec;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.integrations.milesightgateway.model.MilesightGatewayErrorCode;
import com.milesight.beaveriot.integrations.milesightgateway.model.api.*;
import com.milesight.beaveriot.integrations.milesightgateway.mqtt.MsGwMqttClient;
import com.milesight.beaveriot.integrations.milesightgateway.mqtt.MsGwMqttUtil;
import com.milesight.beaveriot.integrations.milesightgateway.mqtt.model.MqttRequest;
import com.milesight.beaveriot.integrations.milesightgateway.mqtt.model.MqttRequestError;
import com.milesight.beaveriot.integrations.milesightgateway.mqtt.model.MqttResponse;
import com.milesight.beaveriot.integrations.milesightgateway.util.Constants;
import com.milesight.beaveriot.integrations.milesightgateway.util.GatewayString;
import lombok.Data;
import org.springframework.util.ObjectUtils;

import java.util.*;

/**
 * GatewayRequesterV2 class.
 * Suitable for SG50 / UG63-V2 Series
 *
 * @author simon
 * @date 2025/10/30
 */
public class GatewayRequesterV2 implements GatewayRequester {
    private final MsGwMqttClient msGwMqttClient;

    private final String eui;

    private final String applicationId;

    private static final List<DeviceListAppItem> supportedApplications = List.of(
            new DeviceListAppItem("1", "1"),
            new DeviceListAppItem("2", "2"),
            new DeviceListAppItem("3", "3")
    );

    private static final String CLASS_A_OTAA = "ClassA-OTAA";

    private static final String CLASS_C_OTAA = "ClassC-OTAA";

    private static final String NET_OTAA = "OTAA";

    private static final String NET_CONFIG_CLASS_MODE = "classMode";

    private static final String NET_CONFIG_ACCESS = "netAccess";

    private static final Map<String, Map<String, String>> NET_CONFIG = Map.of(
            CLASS_A_OTAA, Map.of(NET_CONFIG_CLASS_MODE, "Class A", NET_CONFIG_ACCESS, NET_OTAA),
            CLASS_C_OTAA, Map.of(NET_CONFIG_CLASS_MODE, "Class C", NET_CONFIG_ACCESS, NET_OTAA)
    );

    private static final List<DeviceListProfileItem> supportedProfiles = List.of(
            new DeviceListProfileItem(CLASS_A_OTAA, CLASS_A_OTAA),
            new DeviceListProfileItem(CLASS_C_OTAA, CLASS_C_OTAA)
    );

    public GatewayRequesterV2(MsGwMqttClient msGwMqttClient, String eui, String applicationId) {
        this.msGwMqttClient = msGwMqttClient;
        this.eui = GatewayString.standardizeEUI(eui);
        this.applicationId = applicationId;
    }

    @Override
    public String getVersion() {
        return Constants.GATEWAY_VERSION_V2;
    }

    @Override
    public String getGatewayEui() { return this.eui; }

    @Data
    private static class DeviceListResponseV2 {
        private Integer total;
        private List<Map<String, Object>> result;
        private Integer deviceMax;
    }

    private <T, R> MqttResponse<T> convertToResponse(MqttResponse<R> srcObjResponse, T obj) {
        MqttResponse<T> response = new MqttResponse<>();
        response.setId(srcObjResponse.getId());
        response.setBody(srcObjResponse.getBody());
        response.setMethod(srcObjResponse.getMethod());
        response.setUrl(srcObjResponse.getUrl());
        response.setCtx(srcObjResponse.getCtx());
        response.setErrorBody(srcObjResponse.getErrorBody());
        response.setSuccessBody(obj);
        return response;
    }

    private MqttRequest buildDeviceListRequest(String search, int offset, int limit) {
        MqttRequest req = new MqttRequest();
        req.setMethod("GET");
        String url = "/ns/device?search=" + search + "&offset=" + offset + "&limit=" + limit;
        if (this.applicationId != null) {
            url += "&applicationId=" + this.applicationId;
        }

        req.setUrl(url);

        return req;
    }

    private MqttResponse<DeviceListResponse> requestDeviceList(String search, int offset, int limit) {
        MqttResponse<DeviceListResponseV2> response = msGwMqttClient.request(this.eui, buildDeviceListRequest(search, offset, limit), DeviceListResponseV2.class);
        if (response.getErrorBody() != null) {
            throw ServiceException
                    .with(MilesightGatewayErrorCode.GATEWAY_RESPOND_ERROR)
                    .args(response.getErrorBody().toMap())
                    .build();
        }

        DeviceListResponse result = new DeviceListResponse();
        result.setAppResult(supportedApplications);
        result.setAppTotalCount(supportedApplications.size());
        result.setDeviceResult(response.getSuccessBody().getResult());
        result.setDevTotalCount(response.getSuccessBody().getTotal());
        result.setProfileResult(supportedProfiles);
        result.setPfTotalCount(supportedProfiles.size());

        return convertToResponse(response, result);
    }

    @Override
    public MqttResponse<DeviceListResponse> requestDeviceList(int offset, int limit) {
        return this.requestDeviceList("", offset, limit);
    }

    @Override
    public Optional<Map<String, Object>> requestDeviceItemByEui(String deviceEui) {
        return requestDeviceList(deviceEui, 0, 1)
                .getSuccessBody()
                .getDeviceResult().stream()
                .filter(item -> ((String) item.get(DeviceListItemFields.DEV_EUI)).equalsIgnoreCase(deviceEui))
                .findFirst();
    }

    @Override
    public void requestUpdateDeviceItem(String deviceEui, Map<String, Object> itemData) {
        // Do nothing
    }

    @Override
    public void requestAddDevice(AddDeviceRequest requestData) {
        MqttRequest req = new MqttRequest();
        req.setMethod("POST");
        req.setUrl("/ns/device/add");
        Map<String, Object> reqBody = new HashMap<>();
        reqBody.put("name", requestData.getName());
        reqBody.put("description", requestData.getDescription());
        reqBody.put("devEUI", requestData.getDevEUI());
        reqBody.putAll(NET_CONFIG.get(requestData.getProfileID()));
        reqBody.put("appKey", ObjectUtils.isEmpty(requestData.getAppKey()) ? "5572404c696e6b4c6f52613230313823" : requestData.getAppKey());
        reqBody.put("applicationId", requestData.getApplicationID());
        reqBody.put("skipFCntCheck", !requestData.getSkipFCntCheck()); // For some reason, it's opposite to UG65/67
        reqBody.put("fPort", requestData.getFPort());
        reqBody.put("fCntUp", 0);
        reqBody.put("fCntDown", 0);

        req.setBody(reqBody);
        MqttResponse<Void> response = msGwMqttClient.request(this.eui, req, null);
        MqttRequestError errorBody = response.getErrorBody();
        if (response.getErrorBody() != null) {
            ErrorCodeSpec codeSpec = MilesightGatewayErrorCode.GATEWAY_RESPOND_ERROR;
            if (errorBody.getCode().equals(20101002)) {
                codeSpec = MilesightGatewayErrorCode.DUPLICATED_DEVICE_ON_GATEWAY;
            } else if (errorBody.getCode().equals(20101003)) {
                codeSpec = MilesightGatewayErrorCode.DEVICE_NUM_LIMITED_ON_GATEWAY;
            }

            throw ServiceException
                    .with(codeSpec)
                    .args(errorBody.toMap())
                    .build();
        }
    }

    @Override
    public void requestDeleteDeviceAsync(List<String> deviceEuiList) {
        MqttRequest req = new MqttRequest();
        req.setMethod("DELETE");
        req.setUrl("/ns/device");
        req.setBody(Map.of(
                "ids",
                deviceEuiList
        ));
        msGwMqttClient.requestWithoutResponse(this.eui, req);
    }

    private static final int DEVICE_GET_BATCH_SIZE = 20;

    @Override
    public List<Map<String, Object>> requestAllDeviceList() {
        DeviceListResponse initResponse = requestDeviceList(0, DEVICE_GET_BATCH_SIZE).getSuccessBody();
        if (initResponse.getDevTotalCount() == 0) {
            return List.of();
        }

        List<Map<String, Object>> result = new ArrayList<>(initResponse.getDeviceResult());

        List<MqttRequest> reqList = new ArrayList<>();
        for (int offset = DEVICE_GET_BATCH_SIZE; offset < initResponse.getDevTotalCount(); offset += DEVICE_GET_BATCH_SIZE) {
            reqList.add(buildDeviceListRequest("", offset, DEVICE_GET_BATCH_SIZE));
        }

        List<MqttResponse<DeviceListResponseV2>> restResponses = msGwMqttClient.batchRequest(this.eui, reqList, DeviceListResponseV2.class);
        restResponses.forEach(response -> {
            if (response.getSuccessBody() != null) {
                result.addAll(response.getSuccessBody().getResult());
            }
        });

        return result;
    }

    @Override
    public void downlink(String nodeDeviceEui, Integer fPort, String data) {
        msGwMqttClient.downlink(MsGwMqttUtil.getDownlinkTopic(this.eui, nodeDeviceEui), Map.of(
                "fPort", fPort,
                "data", data,
                "confirmed", false
        ));
    }

    @Override
    public void detect() {
        MqttRequest req = buildDeviceListRequest("", 0, 1);
        msGwMqttClient.requestWithoutResponse(this.eui, req);
    }
}
