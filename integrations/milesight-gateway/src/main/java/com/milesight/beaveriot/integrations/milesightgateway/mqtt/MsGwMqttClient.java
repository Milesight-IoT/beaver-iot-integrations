package com.milesight.beaveriot.integrations.milesightgateway.mqtt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.context.api.*;
import com.milesight.beaveriot.context.integration.model.DeviceStatus;
import com.milesight.beaveriot.context.model.response.DeviceTemplateInputResult;
import com.milesight.beaveriot.context.mqtt.enums.MqttQos;
import com.milesight.beaveriot.context.mqtt.model.MqttConnectEvent;
import com.milesight.beaveriot.context.mqtt.model.MqttDisconnectEvent;
import com.milesight.beaveriot.context.mqtt.model.MqttMessage;
import com.milesight.beaveriot.integrations.milesightgateway.model.MilesightGatewayErrorCode;
import com.milesight.beaveriot.integrations.milesightgateway.mqtt.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import com.milesight.beaveriot.integrations.milesightgateway.util.GatewayString;
import org.springframework.util.StringUtils;

/**
 * MsGwMqttClient class.
 *
 * @author simon
 * @date 2025/2/12
 */
@Component
@Slf4j
public class MsGwMqttClient {
    private final AtomicBoolean isInit = new AtomicBoolean(false);

    private static final Integer REQUEST_TIMEOUT_SECONDS = 8;

    public static final Integer GATEWAY_REQUEST_BATCH_SIZE = 3;

    @Autowired
    EntityValueServiceProvider entityValueServiceProvider;

    @Autowired
    MqttPubSubServiceProvider mqttServiceProvider;

    @Autowired
    TaskExecutor taskExecutor;

    @Autowired
    DeviceTemplateParserProvider deviceTemplateParserProvider;

    @Autowired
    DeviceStatusServiceProvider deviceStatusServiceProvider;

    @Autowired
    MsGwStatus msGwStatus;

    @Autowired
    RequestCoalescerProvider requestCoalescer;

    private final Map<String, CompletableFuture<MqttRawResponse>> pendingRequests = new ConcurrentHashMap<>();

    private final ObjectMapper json = GatewayString.jsonInstance();

    public void init() {
        if (!isInit.compareAndSet(false, true)) {
            return;
        }

        msGwStatus.init();

        mqttServiceProvider.subscribe(MsGwMqttUtil.getUplinkTopic(MsGwMqttUtil.MQTT_TOPIC_PLACEHOLDER), (MqttMessage message) -> {
            this.onDataUplink(MsGwMqttUtil.parseGatewayIdFromTopic(message.getTopicSubPath()), new String(message.getPayload(), StandardCharsets.UTF_8));
        }, true);

        mqttServiceProvider.subscribe(MsGwMqttUtil.getResponseTopic(MsGwMqttUtil.MQTT_TOPIC_PLACEHOLDER), (MqttMessage message) -> {
            this.onResponse(MsGwMqttUtil.parseGatewayIdFromTopic(message.getTopicSubPath()), new String(message.getPayload(), StandardCharsets.UTF_8), message);
        }, false);

        mqttServiceProvider.onConnect(this::onGatewayConnect);
        mqttServiceProvider.onDisconnect(this::onGatewayDisconnect);
    }

    private void onDataUplink(String gatewayEui, String message) {
        log.debug("{} uplink: {}", gatewayEui, message);
        try {
            MqttUplinkData uplinkData = json.readValue(message, MqttUplinkData.class);
            String deviceEui = GatewayString.standardizeEUI(uplinkData.getDevEUI());

            byte[] binData = Base64.getDecoder().decode(uplinkData.getData());
            String deviceKey = GatewayString.getDeviceKey(deviceEui);
            DeviceTemplateInputResult inputResult = deviceTemplateParserProvider.input(deviceKey, binData, Map.of("fPort", uplinkData.getFPort()));

            log.debug("Payload: {}", inputResult.getPayload());
            entityValueServiceProvider.saveValuesAndPublishAsync(inputResult.getPayload(), "DEVICE_UPLINK");
            requestCoalescer.execute(deviceKey + "-" + DeviceStatus.ONLINE, () -> {
                deviceStatusServiceProvider.online(inputResult.getDevice());
                return deviceKey;
            });
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }

        msGwStatus.updateGatewayStatus(gatewayEui, DeviceStatus.ONLINE, System.currentTimeMillis());
    }

    private void onResponse(String gatewayEui, String message, MqttMessage mqttMessage) {
        log.debug("{} response: {}", gatewayEui, message);
        try {
            MqttRawResponse rawResponse = json.readValue(message, MqttRawResponse.class);
            rawResponse.getCtx().setUsername(mqttMessage.getUsername());
            CompletableFuture<MqttRawResponse> request = pendingRequests.get(rawResponse.getId());
            if (request == null) {
                log.debug("No request found for {}: {}", gatewayEui, rawResponse.getId());
            } else {
                request.complete(rawResponse);
            }
        } catch (Exception e) {
            log.error("read response error", e);
        }

        msGwStatus.updateGatewayStatus(gatewayEui, DeviceStatus.ONLINE, System.currentTimeMillis());
    }

    private void onGatewayConnect(MqttConnectEvent event) {
        updateGatewayStatusFromClientId(event.getClientId(), DeviceStatus.ONLINE, event.getTs());
    }

    private void onGatewayDisconnect(MqttDisconnectEvent event) {
        updateGatewayStatusFromClientId(event.getClientId(), DeviceStatus.OFFLINE, event.getTs());
    }

    private void updateGatewayStatusFromClientId(String clientId, DeviceStatus status, Long ts) {
        String eui = GatewayString.parseGatewayEuiFromClientId(clientId);
        if (eui == null) {
            return;
        }

        msGwStatus.updateGatewayStatus(eui, status, ts);
    }

    private void mqttPublish(String topic, byte[] data) {
        mqttServiceProvider.publish(topic, data, MqttQos.AT_MOST_ONCE, false);
    }

    public void downlink(String topic, Object data) {
        try {
            mqttPublish(topic, json.writeValueAsBytes(data));
        } catch (JsonProcessingException e) {
            throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), "Downlink Gateway Error: " + e.getMessage()).build();
        }
    }

    public <T> MqttResponse<T> request(String gatewayEui, MqttRequest req, Class<T> responseType) {
        log.trace("request {}", req);

        CompletableFuture<MqttRawResponse> pendingRequest = new CompletableFuture<>();
        pendingRequests.put(req.getId(), pendingRequest);

        final MqttResponse<T> response = new MqttResponse<>();
        final String gatewayTopic = MsGwMqttUtil.getRequestTopic(gatewayEui);
        try {
            mqttPublish(gatewayTopic, json.writeValueAsBytes(req));

            MqttRawResponse rawResponse = pendingRequest.get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            BeanUtils.copyProperties(rawResponse, response);
            if (!StringUtils.hasText(json.convertValue(response.getBody().get("error"), String.class))) {
                if (responseType != null) {
                    response.setSuccessBody(json.readValue(json.writeValueAsString(rawResponse.getBody()), responseType));
                }
            } else {
                response.setErrorBody(json.readValue(json.writeValueAsString(rawResponse.getBody()), MqttRequestError.class));
            }
        } catch (Exception e) {
            log.error("Request Gateway Error: " + e.getMessage());
            throw ServiceException.with(MilesightGatewayErrorCode.GATEWAY_REQUEST_TIMEOUT).build();
        } finally {
            pendingRequests.remove(req.getId());
        }

        return response;
    }

    public <T> List<MqttResponse<T>> batchRequest(String gatewayEui, List<MqttRequest> req, Class<T> responseType) {
        if (req.isEmpty()) {
            return List.of();
        }

        final List<MqttResponse<T>> result = new ArrayList<>();

        int offset = 0;
        while (offset < req.size()) {
            int end = Math.min(req.size(), offset + GATEWAY_REQUEST_BATCH_SIZE);
            List<CompletableFuture<MqttResponse<T>>> allFutures = req
                    .subList(offset, end)
                    .stream()
                    .map(r -> CompletableFuture.supplyAsync(() -> request(gatewayEui, r, responseType), taskExecutor))
                    .toList();
            CompletableFuture<?>[] futuresArray = allFutures.toArray(new CompletableFuture<?>[0]);
            CompletableFuture.allOf(futuresArray).join();
            allFutures.forEach(f -> result.add(f.join()));
            offset = end;
        }

        return result;
    }

    public void requestWithoutResponse(String gatewayEui, MqttRequest req) {
        log.trace("request {}", req);
        final String gatewayTopic = MsGwMqttUtil.getRequestTopic(gatewayEui);
        try {
            mqttPublish(gatewayTopic, json.writeValueAsBytes(req));
        } catch (Exception e) {
            log.error("Request Gateway Error: " + e.getMessage());
            throw ServiceException.with(MilesightGatewayErrorCode.GATEWAY_REQUEST_TIMEOUT).build();
        }
    }
}
