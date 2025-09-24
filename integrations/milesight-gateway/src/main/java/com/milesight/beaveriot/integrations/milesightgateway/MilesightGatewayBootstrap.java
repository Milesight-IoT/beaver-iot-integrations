package com.milesight.beaveriot.integrations.milesightgateway;

import com.milesight.beaveriot.context.api.DeviceStatusServiceProvider;
import com.milesight.beaveriot.context.integration.bootstrap.IntegrationBootstrap;
import com.milesight.beaveriot.context.integration.model.Integration;
import com.milesight.beaveriot.integrations.milesightgateway.legacy.VersionUpgradeService;
import com.milesight.beaveriot.integrations.milesightgateway.mqtt.MsGwMqttClient;
import com.milesight.beaveriot.integrations.milesightgateway.service.DeviceModelService;
import com.milesight.beaveriot.integrations.milesightgateway.service.DeviceService;
import com.milesight.beaveriot.integrations.milesightgateway.service.GatewayService;
import com.milesight.beaveriot.integrations.milesightgateway.util.Constants;
import com.milesight.beaveriot.integrations.milesightgateway.util.GatewayString;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
        gatewayService.syncGatewayListToAddDeviceGatewayEuiList();
        deviceModelService.syncDeviceModelListToAdd();
        this.registerStatusManager();
    }

    private void registerStatusManager() {
        deviceStatusServiceProvider.register(Constants.INTEGRATION_ID, device -> {
            String deviceEui = GatewayString.getDeviceIdentifierByKey(device.getKey());
            if (GatewayString.isGatewayIdentifier(deviceEui)) {
                return null;
            }

            return deviceService.getDeviceOfflineTimeout(device);
        });
    }

    @Override
    public void onDestroy(Integration integration) {
        // do nothing
    }
}
