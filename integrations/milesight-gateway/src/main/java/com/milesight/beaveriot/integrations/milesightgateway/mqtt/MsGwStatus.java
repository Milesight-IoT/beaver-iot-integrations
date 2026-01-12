package com.milesight.beaveriot.integrations.milesightgateway.mqtt;

import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.api.DeviceStatusServiceProvider;
import com.milesight.beaveriot.context.api.RequestCoalescerProvider;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.DeviceStatus;
import com.milesight.beaveriot.context.integration.wrapper.AnnotatedEntityWrapper;
import com.milesight.beaveriot.integrations.milesightgateway.entity.MsGwIntegrationEntities;
import com.milesight.beaveriot.integrations.milesightgateway.model.GatewayData;
import com.milesight.beaveriot.integrations.milesightgateway.mqtt.model.GatewayActiveMessage;
import com.milesight.beaveriot.integrations.milesightgateway.service.MsGwEntityService;
import com.milesight.beaveriot.integrations.milesightgateway.util.Constants;
import com.milesight.beaveriot.integrations.milesightgateway.util.GatewayString;
import com.milesight.beaveriot.pubsub.MessagePubSub;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * MsGwStatus class.
 *
 * @author simon
 * @date 2025/11/13
 */
@Component
@Slf4j
public class MsGwStatus {
    private final Map<String, Set<Consumer<String>>> gatewayStatusHandler = new ConcurrentHashMap<>();

    @Autowired
    MessagePubSub messagePubSub;

    @Autowired
    LockProvider lockProvider;

    @Autowired
    DeviceServiceProvider deviceServiceProvider;

    @Autowired
    MsGwEntityService msGwEntityService;

    @Autowired
    DeviceStatusServiceProvider deviceStatusServiceProvider;

    @Autowired
    RequestCoalescerProvider requestCoalescer;

    public void init() {
        messagePubSub.subscribe(GatewayActiveMessage.class, this::onGatewayActive);
    }

    public void updateGatewayStatus(String inputEui, DeviceStatus status, Long ts) {
        updateGatewayStatus(inputEui, status, ts, true);
    }

    public void updateGatewayStatus(String inputEui, DeviceStatus status, Long ts, boolean shouldPublish) {
        final String eui = GatewayString.standardizeEUI(inputEui);
        requestCoalescer.executeAsync(eui + "-" + status, () -> {
            this.doUpdateGatewayStatus(eui, status, ts, shouldPublish);
            return inputEui;
        });
    }

    private void doUpdateGatewayStatus(String eui, DeviceStatus status, Long ts, boolean shouldPublish) {
        if (shouldPublish) {
            this.publishGatewayActiveMessage(GatewayActiveMessage.builder().eui(eui).status(status).build());
        }

        String identifier = GatewayString.getGatewayIdentifier(eui);
        Device gateway = deviceServiceProvider.findByIdentifier(identifier, Constants.INTEGRATION_ID);
        if (gateway == null) {
            return;
        }

        if (status.equals(DeviceStatus.ONLINE)) {
            deviceStatusServiceProvider.online(gateway);
        } else if (status.equals(DeviceStatus.OFFLINE)) {
            deviceStatusServiceProvider.offline(gateway);
        } else {
            throw new IllegalArgumentException("Unknown device status: " + status);
        }
    }

    public void publishGatewayStatusEvent(DeviceStatus status, Device gateway, Long ts) {
        new AnnotatedEntityWrapper<MsGwIntegrationEntities.GatewayStatusEvent>().saveValues(Map.of(
                MsGwIntegrationEntities.GatewayStatusEvent::getStatus, status,
                MsGwIntegrationEntities.GatewayStatusEvent::getGatewayName, gateway.getName(),
                MsGwIntegrationEntities.GatewayStatusEvent::getEui, GatewayData.fromMap(gateway.getAdditional()).getEui(),
                MsGwIntegrationEntities.GatewayStatusEvent::getStatusTimestamp, ts
        )).publishAsync();
    }

    public void registerGatewayStatusHandler(String eui, Consumer<String> handler) {
        gatewayStatusHandler
                .computeIfAbsent(eui, iEui -> ConcurrentHashMap.newKeySet())
                .add(handler);
    }

    public void unregisterGatewayStatusHandler(String eui, Consumer<String> handler) {
        gatewayStatusHandler
                .computeIfPresent(eui, (matchedEui, handlers) -> {
                    handlers.remove(handler);
                    if (handlers.isEmpty()) {
                        return null;
                    }

                    return handlers;
                });
    }

    public void publishGatewayActiveMessage(GatewayActiveMessage message) {
        if (!gatewayStatusHandler.isEmpty()) {
            messagePubSub.publish(message);
        }
    }

    private void onGatewayActive(GatewayActiveMessage gatewayStatus) {
        gatewayStatusHandler.getOrDefault(gatewayStatus.getEui(), new HashSet<>()).forEach(handler -> handler.accept(gatewayStatus.getEui()));
    }
}
