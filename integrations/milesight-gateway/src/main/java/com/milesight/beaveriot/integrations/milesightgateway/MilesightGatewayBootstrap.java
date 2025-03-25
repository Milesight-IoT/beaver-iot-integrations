package com.milesight.beaveriot.integrations.milesightgateway;

import com.milesight.beaveriot.context.integration.bootstrap.IntegrationBootstrap;
import com.milesight.beaveriot.context.integration.model.Integration;
import com.milesight.beaveriot.integrations.milesightgateway.mqtt.MsGwMqttClient;
import com.milesight.beaveriot.integrations.milesightgateway.service.DeviceCodecService;
import com.milesight.beaveriot.integrations.milesightgateway.service.GatewayService;
import com.milesight.beaveriot.integrations.milesightgateway.service.MsGwEntityService;
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
    MsGwEntityService msGwEntityService;

    @Autowired
    DeviceCodecService deviceCodecService;

    @Override
    public void onPrepared(Integration integration) {
        // do nothing
    }

    @Override
    public void onStarted(Integration integrationConfig) {

    }

    @Override
    public void onEnabled(String tenantId, Integration integrationConfig) {
        msGwMqttClient.init();
        gatewayService.syncGatewayListToAddDeviceGatewayEuiList();
        deviceCodecService.syncDeviceModelListToAdd(msGwEntityService.getModelData());
    }

    @Override
    public void onDestroy(Integration integration) {
        // do nothing
    }
}
