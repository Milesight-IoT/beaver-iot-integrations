package com.milesight.beaveriot.integrations.milesightgateway.service;

import com.milesight.beaveriot.context.integration.model.DeviceStatus;
import com.milesight.beaveriot.integrations.milesightgateway.model.GatewayData;
import com.milesight.beaveriot.integrations.milesightgateway.mqtt.MsGwStatus;
import com.milesight.beaveriot.integrations.milesightgateway.requester.GatewayRequesterFactory;
import com.milesight.beaveriot.integrations.milesightgateway.util.GatewayString;
import com.milesight.beaveriot.scheduler.core.Scheduler;
import com.milesight.beaveriot.scheduler.core.model.ScheduleRule;
import com.milesight.beaveriot.scheduler.core.model.ScheduleSettings;
import com.milesight.beaveriot.scheduler.core.model.ScheduleType;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * GatewayStatusDetector class.
 *
 * @author simon
 * @date 2025/11/11
 */
@Slf4j
public class GatewayStatusDetector {
    private final Map<String, GatewayData> pendingGatewayMap;

    private final MsGwStatus msGwStatus;

    private final GatewayRequesterFactory gatewayRequesterFactory;

    private final Scheduler scheduler;

    private static final int PRE_OFFLINE_SECONDS = 30;

    private static final int CONFIRM_OFFLINE_SECONDS = 60;

    private final Consumer<String> foundLambda;

    public GatewayStatusDetector(
            GatewayRequesterFactory gatewayRequesterFactory,
            Scheduler scheduler,
            MsGwStatus msGwStatus,
            List<GatewayData> gatewayDataList
    ) {
        this.gatewayRequesterFactory = gatewayRequesterFactory;
        this.scheduler = scheduler;
        this.pendingGatewayMap = gatewayDataList.stream().collect(Collectors.toConcurrentMap(GatewayData::getEui, Function.identity()));
        this.msGwStatus = msGwStatus;
        foundLambda = this::found;
    }

    public void schedule() {
        this.scheduleDetection();
    }

    public void found(String gatewayEui) {
        pendingGatewayMap.remove(GatewayString.standardizeEUI(gatewayEui));
        msGwStatus.unregisterGatewayStatusHandler(gatewayEui, foundLambda);
    }

    private ScheduleSettings getScheduleSettings(long seconds) {
        return ScheduleSettings.builder()
                .scheduleType(ScheduleType.ONCE)
                .scheduleRule(ScheduleRule.builder().startEpochSecond(Instant.now().getEpochSecond() + seconds).build())
                .build();
    }

    private void scheduleDetection() {
        if (pendingGatewayMap.isEmpty()) {
            return;
        }

        pendingGatewayMap.values().forEach(gatewayData -> msGwStatus.registerGatewayStatusHandler(gatewayData.getEui(), foundLambda));

        scheduler.schedule(
                "Detect-Gateway-" + System.currentTimeMillis() + "-" + GatewayString.generateRandomString(6),
                getScheduleSettings(PRE_OFFLINE_SECONDS),
                detectTask -> {
                    log.debug("schedule gateways: " + pendingGatewayMap.values().stream().map(GatewayData::getEui).toList());
                    pendingGatewayMap.values().forEach(gatewayData -> gatewayRequesterFactory.create(gatewayData).detect());
                    this.scheduleOffline();
                });
    }

    private void scheduleOffline() {
        scheduler.schedule(
                "Offline-Gateway-" + System.currentTimeMillis() + GatewayString.generateRandomString(6),
                getScheduleSettings(CONFIRM_OFFLINE_SECONDS),
                offlineTask -> {
                    log.debug("offline gateways: " + pendingGatewayMap.values().stream().map(GatewayData::getEui).toList());
                    pendingGatewayMap.values().forEach(gatewayData -> {
                        msGwStatus.unregisterGatewayStatusHandler(gatewayData.getEui(), foundLambda);
                        msGwStatus.updateGatewayStatus(gatewayData.getEui(), DeviceStatus.OFFLINE, System.currentTimeMillis(), false);
                    });
                    pendingGatewayMap.clear();
                });
    }
}
