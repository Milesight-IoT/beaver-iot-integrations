package com.milesight.beaveriot.integrations.milesightgateway;

import com.milesight.beaveriot.context.api.BlueprintLibrarySyncerProvider;
import com.milesight.beaveriot.context.api.DeviceStatusServiceProvider;
import com.milesight.beaveriot.context.integration.bootstrap.IntegrationBootstrap;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.DeviceStatusConfig;
import com.milesight.beaveriot.context.integration.model.Integration;
import com.milesight.beaveriot.integrations.milesightgateway.legacy.VersionUpgradeService;
import com.milesight.beaveriot.integrations.milesightgateway.model.GatewayData;
import com.milesight.beaveriot.integrations.milesightgateway.mqtt.MsGwMqttClient;
import com.milesight.beaveriot.integrations.milesightgateway.mqtt.MsGwStatus;
import com.milesight.beaveriot.integrations.milesightgateway.requester.GatewayRequesterFactory;
import com.milesight.beaveriot.integrations.milesightgateway.service.DeviceModelService;
import com.milesight.beaveriot.integrations.milesightgateway.service.DeviceService;
import com.milesight.beaveriot.integrations.milesightgateway.service.GatewayService;
import com.milesight.beaveriot.integrations.milesightgateway.service.GatewayStatusDetector;
import com.milesight.beaveriot.integrations.milesightgateway.util.Constants;
import com.milesight.beaveriot.scheduler.core.Scheduler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MilesightGatewayBootstrap class.
 *
 * @author simon
 * @date 2025/2/12
 */
@Component
@Slf4j
public class MilesightGatewayBootstrap implements IntegrationBootstrap {
    @Autowired
    MsGwMqttClient msGwMqttClient;

    @Autowired
    GatewayService gatewayService;

    @Autowired
    DeviceModelService deviceModelService;

    @Autowired
    DeviceStatusServiceProvider deviceStatusServiceProvider;

    @Autowired
    DeviceService deviceService;

    @Autowired
    VersionUpgradeService versionUpgradeService;

    @Autowired
    BlueprintLibrarySyncerProvider blueprintLibrarySyncerProvider;

    @Autowired
    private GatewayRequesterFactory gatewayRequesterFactory;

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private MsGwStatus msGwStatus;

    @Override
    public void onPrepared(Integration integration) {
        // do nothing
    }

    @Override
    public void onStarted(Integration integrationConfig) {
        msGwMqttClient.init();
    }

    @Override
    @SneakyThrows
    public void onEnabled(String tenantId, Integration integrationConfig) {
        versionUpgradeService.upgrade();
        List<Device> gateways = gatewayService.syncGatewayListToAddDeviceGatewayEuiList();
        deviceModelService.syncDeviceModelListToAdd();
        this.registerStatusManager();
        blueprintLibrarySyncerProvider.addListener(library -> deviceModelService.syncDeviceModelListToAdd());

        // Not required for cloud service
        new GatewayStatusDetector(gatewayRequesterFactory, scheduler, msGwStatus, gateways.stream().map(gateway -> GatewayData.fromMap(gateway.getAdditional())).toList()).schedule();
    }

    private void registerStatusManager() {
        DeviceStatusConfig config = DeviceStatusConfig.builder()
                        .offlineTimeoutFetcher(deviceService::getDeviceOfflineTimeout)
                        .batchOfflineTimeoutFetcher(deviceService::getDeviceOfflineTimeouts)
                        .build();
        deviceStatusServiceProvider.register(Constants.INTEGRATION_ID, config);
    }

    @Override
    public void onDestroy(Integration integration) {
        // do nothing
    }
}
