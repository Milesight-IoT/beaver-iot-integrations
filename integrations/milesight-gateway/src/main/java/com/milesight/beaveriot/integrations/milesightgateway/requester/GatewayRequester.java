package com.milesight.beaveriot.integrations.milesightgateway.requester;

import com.milesight.beaveriot.integrations.milesightgateway.model.api.AddDeviceRequest;
import com.milesight.beaveriot.integrations.milesightgateway.model.api.DeviceListResponse;
import com.milesight.beaveriot.integrations.milesightgateway.mqtt.model.MqttResponse;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * GatewayRequester
 *
 * @author simon
 * @date 2025/10/30
 */
public interface GatewayRequester {
    default MqttResponse<DeviceListResponse> requestBase() {
        return this.requestDeviceList(0, 1);
    }

    MqttResponse<DeviceListResponse> requestDeviceList(int offset, int limit);

    Optional<Map<String, Object>> requestDeviceItemByEui(String deviceEui);

    void requestUpdateDeviceItem(String deviceEui, Map<String, Object> itemData);

    void requestAddDevice(AddDeviceRequest requestData);

    void requestDeleteDeviceAsync(List<String> deviceEuiList);

    List<Map<String, Object>> requestAllDeviceList();

    void downlink(String deviceEui, Integer fPort, String data);

    String getVersion();

    String getGatewayEui();
}
