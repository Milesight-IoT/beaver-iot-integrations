package com.milesight.beaveriot.integrations.milesightgateway.mqtt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.api.MqttPubSubServiceProvider;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.integration.wrapper.AnnotatedEntityWrapper;
import com.milesight.beaveriot.context.mqtt.enums.MqttQos;
import com.milesight.beaveriot.context.mqtt.model.MqttConnectEvent;
import com.milesight.beaveriot.context.mqtt.model.MqttDisconnectEvent;
import com.milesight.beaveriot.context.mqtt.model.MqttMessage;
import com.milesight.beaveriot.integrations.milesightgateway.codec.CodecExecutor;
import com.milesight.beaveriot.integrations.milesightgateway.codec.EntityValueConverter;
import com.milesight.beaveriot.integrations.milesightgateway.entity.MsGwIntegrationEntities;
import com.milesight.beaveriot.integrations.milesightgateway.model.DeviceConnectStatus;
import com.milesight.beaveriot.integrations.milesightgateway.service.MsGwEntityService;
import com.milesight.beaveriot.integrations.milesightgateway.util.Constants;
import com.milesight.beaveriot.integrations.milesightgateway.mqtt.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import com.milesight.beaveriot.integrations.milesightgateway.util.GatewayString;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import static com.milesight.beaveriot.integrations.milesightgateway.mqtt.MsGwMqttUtil.getMqttTopic;
import static com.milesight.beaveriot.integrations.milesightgateway.mqtt.MsGwMqttUtil.parseGatewayIdFromTopic;

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

    private static final Integer REQUEST_TIMEOUT_SECONDS = 6;

    @Autowired
    EntityValueServiceProvider entityValueServiceProvider;

    @Autowired
    MqttPubSubServiceProvider mqttServiceProvider;

    @Autowired
    MsGwEntityService msGwEntityService;

    @Autowired
    DeviceServiceProvider deviceServiceProvider;

    private final Map<String, CompletableFuture<MqttRawResponse>> pendingRequests = new ConcurrentHashMap<>();

    private final ObjectMapper json = GatewayString.jsonInstance();

    public void init() {
        if (!isInit.compareAndSet(false, true)) {
            return;
        }

        mqttServiceProvider.subscribe(getMqttTopic("+", Constants.GATEWAY_MQTT_UPLINK_SCOPE), (MqttMessage message) -> {
            this.onDataUplink(parseGatewayIdFromTopic(message.getTopicSubPath()), new String(message.getPayload(), StandardCharsets.UTF_8));
        }, true);

        mqttServiceProvider.subscribe(getMqttTopic("+", Constants.GATEWAY_MQTT_RESPONSE_SCOPE), (MqttMessage message) -> {
            this.onResponse(parseGatewayIdFromTopic(message.getTopicSubPath()), new String(message.getPayload(), StandardCharsets.UTF_8), message);
        }, false);

        mqttServiceProvider.onConnect(this::onGatewayConnect);
        mqttServiceProvider.onDisconnect(this::onGatewayDisconnect);
    }

    private void onDataUplink(String gatewayEui, String message) {
        log.debug("{} uplink: {}", gatewayEui, message);
        try {
            MqttUplinkData uplinkData = json.readValue(message, MqttUplinkData.class);
            String deviceEui = GatewayString.standardizeEUI(uplinkData.getDevEUI());

            // decode uplink data
            String decoderScript = msGwEntityService.getDeviceDecoderScript(deviceEui);
            if (!StringUtils.hasText(decoderScript)) {
                log.warn("Decode Script not found: " + deviceEui);
                return;
            }

            byte[] binData = Base64.getDecoder().decode(uplinkData.getData());
            int[] intArray = new int[binData.length];
            for (int i = 0; i < binData.length; i++) {
                intArray[i] = binData[i] & 0xFF;
            }

            String decodeResult = CodecExecutor.runDecode(decoderScript, uplinkData.getFPort(), intArray);
            log.debug("decoded {}", decodeResult);

            // save uplink data to entity
            String deviceKey = GatewayString.getDeviceKey(deviceEui);
            Map<String, Object> entityValueMap = EntityValueConverter.convertToEntityKeyMap(deviceKey, json.readTree(decodeResult));
            log.debug("entity value map {}", entityValueMap);
            if (ObjectUtils.isEmpty(entityValueMap)) {
                return;
            }

            entityValueServiceProvider.saveValuesAndPublishAsync(ExchangePayload.create(entityValueMap), "DEVICE_UPLINK");
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }
    }

    private void onResponse(String gatewayEui, String message, MqttMessage mqttMessage) {
        log.debug("{} response: {}", gatewayEui, message);
        try {
            MqttRawResponse rawResponse = json.readValue(message, MqttRawResponse.class);
            rawResponse.getCtx().setUsername(mqttMessage.getUsername());
            CompletableFuture<MqttRawResponse> request = pendingRequests.get(rawResponse.getId());
            if (request == null) {
                log.warn("No request found for {}: {}", gatewayEui, rawResponse.getId());
                return;
            }

            request.complete(rawResponse);
        } catch (Exception e) {
            log.error("read response error", e);
        }
    }

    private void onGatewayConnect(MqttConnectEvent event) {
        updateGatewayStatus(event.getClientId(), DeviceConnectStatus.ONLINE, event.getTs());
    }

    private void onGatewayDisconnect(MqttDisconnectEvent event) {
        updateGatewayStatus(event.getClientId(), DeviceConnectStatus.OFFLINE, event.getTs());
    }

    private void updateGatewayStatus(String gatewayClientId, DeviceConnectStatus status, Long ts) {
        String eui = GatewayString.parseGatewayEuiFromClientId(gatewayClientId);
        if (eui == null) {
            return;
        }

        String identifier = GatewayString.getGatewayIdentifier(eui);
        DeviceConnectStatus curStatus = msGwEntityService.getGatewayStatus(List.of(identifier)).get(identifier);
        if (curStatus == null) {
            curStatus = DeviceConnectStatus.ONLINE;
        }

        if (status.equals(curStatus)) {
            return;
        }

        Device gateway = deviceServiceProvider.findByIdentifier(identifier, Constants.INTEGRATION_ID);
        if (gateway == null) {
            return;
        }

        entityValueServiceProvider.saveValuesAndPublishAsync(ExchangePayload.create(Map.of(
                GatewayString.getGatewayStatusKey(identifier), status.name()
        )));
        new AnnotatedEntityWrapper<MsGwIntegrationEntities.GatewayStatusEvent>().saveValues(Map.of(
                MsGwIntegrationEntities.GatewayStatusEvent::getStatus, status.name(),
                MsGwIntegrationEntities.GatewayStatusEvent::getGatewayName, gateway.getName(),
                MsGwIntegrationEntities.GatewayStatusEvent::getEui, eui,
                MsGwIntegrationEntities.GatewayStatusEvent::getStatusTimestamp, ts
        )).publishAsync();
    }

    private void mqttPublish(String topic, byte[] data) {
        mqttServiceProvider.publish(topic, data, MqttQos.AT_MOST_ONCE, false);
    }

    public void downlink(String gatewayEui, String deviceEui, Integer fPort, String data) {
        final String gatewayTopic = getMqttTopic(gatewayEui, Constants.GATEWAY_MQTT_DOWNLINK_SCOPE);
        MqttDownlinkData downlinkData = new MqttDownlinkData();
        downlinkData.setDevEUI(deviceEui);
        downlinkData.setFPort(fPort);
        downlinkData.setData(data);
        try {
            mqttPublish(gatewayTopic, json.writeValueAsBytes(downlinkData));
        } catch (JsonProcessingException e) {
            throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), "Downlink Gateway Error: " + e.getMessage()).build();
        }
    }

    public <T> MqttResponse<T> request(String gatewayEui, MqttRequest req, Class<T> responseType) {
        log.trace("request {}", req);

        CompletableFuture<MqttRawResponse> pendingRequest = new CompletableFuture<>();
        pendingRequests.put(req.getId(), pendingRequest);

        final MqttResponse<T> response = new MqttResponse<>();
        final String gatewayTopic = getMqttTopic(gatewayEui, Constants.GATEWAY_MQTT_REQUEST_SCOPE);
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
        } catch (InterruptedException e) {
            throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), "Request Gateway Timeout").build();
        } catch (Exception e) {
            throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), "Request Gateway Error: " + e.getMessage()).build();
        } finally {
            pendingRequests.remove(req.getId());
        }

        return response;
    }

    public <T> List<MqttResponse<T>> batchRequest(String gatewayEui, List<MqttRequest> req, Class<T> responseType) {
        if (req.isEmpty()) {
            return List.of();
        }

        List<MqttResponse<T>> ret = new ArrayList<>();
        List<CompletableFuture<MqttResponse<T>>> allFutures = req
                .stream()
                .map(r -> CompletableFuture.supplyAsync(() -> request(gatewayEui, r, responseType)))
                .toList();
        CompletableFuture<?>[] futuresArray = allFutures.toArray(new CompletableFuture<?>[0]);
        CompletableFuture.allOf(futuresArray).join();
        allFutures.forEach(f -> ret.add(f.join()));
        return ret;
    }
}
