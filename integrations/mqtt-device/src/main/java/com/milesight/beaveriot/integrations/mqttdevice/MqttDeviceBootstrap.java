package com.milesight.beaveriot.integrations.mqttdevice;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.context.api.CredentialsServiceProvider;
import com.milesight.beaveriot.context.api.DeviceTemplateServiceProvider;
import com.milesight.beaveriot.context.api.MqttPubSubServiceProvider;
import com.milesight.beaveriot.context.integration.bootstrap.IntegrationBootstrap;
import com.milesight.beaveriot.context.integration.enums.CredentialsType;
import com.milesight.beaveriot.context.integration.model.Credentials;
import com.milesight.beaveriot.context.integration.model.DeviceTemplate;
import com.milesight.beaveriot.context.integration.model.Integration;
import com.milesight.beaveriot.context.integration.wrapper.AnnotatedEntityWrapper;
import com.milesight.beaveriot.context.mqtt.model.MqttBrokerInfo;
import com.milesight.beaveriot.integrations.mqttdevice.entity.MqttDeviceIntegrationEntities;
import com.milesight.beaveriot.integrations.mqttdevice.service.MqttDeviceMqttService;
import com.milesight.beaveriot.integrations.mqttdevice.support.DataCenter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;

/**
 * author: Luxb
 * create: 2025/5/14 13:54
 **/
@Slf4j
@Component
public class MqttDeviceBootstrap implements IntegrationBootstrap {
    private final MqttPubSubServiceProvider mqttPubSubServiceProvider;
    private final CredentialsServiceProvider credentialsServiceProvider;
    private final DeviceTemplateServiceProvider deviceTemplateServiceProvider;
    private final MqttDeviceMqttService mqttDeviceMqttService;

    public MqttDeviceBootstrap(MqttPubSubServiceProvider mqttPubSubServiceProvider, CredentialsServiceProvider credentialsServiceProvider, DeviceTemplateServiceProvider deviceTemplateServiceProvider, MqttDeviceMqttService mqttDeviceMqttService) {
        this.mqttPubSubServiceProvider = mqttPubSubServiceProvider;
        this.credentialsServiceProvider = credentialsServiceProvider;
        this.deviceTemplateServiceProvider = deviceTemplateServiceProvider;
        this.mqttDeviceMqttService = mqttDeviceMqttService;
    }

    @Override
    public void onPrepared(Integration integrationConfig) {

    }

    @Override
    public void onStarted(Integration integrationConfig) {

    }

    @Override
    public void onDestroy(Integration integrationConfig) {

    }

    @Override
    public void onEnabled(String tenantId, Integration integrationConfig) {
        log.info("Mqtt device integration starting");
        initProperties();
        subscribeTopics();
        IntegrationBootstrap.super.onEnabled(tenantId, integrationConfig);
        log.info("Mqtt device integration started");
    }

    private void initProperties() {
        AnnotatedEntityWrapper<MqttDeviceIntegrationEntities.MqttDeviceProperties> wrapper = new AnnotatedEntityWrapper<>();
        boolean isPropertiesInitialized = (boolean) wrapper.getValue(MqttDeviceIntegrationEntities.MqttDeviceProperties::getIsInitialized).orElse(false);
        if (isPropertiesInitialized) {
            return;
        }
        MqttBrokerInfo mqttBrokerInfo = mqttPubSubServiceProvider.getMqttBrokerInfo();
        if (mqttBrokerInfo == null) {
            throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), "Mqtt broker not found").build();
        }
        if (mqttBrokerInfo.getHost() == null) {
            mqttBrokerInfo.setHost(getLocalAddress());
        }
        if (mqttBrokerInfo.getHost() == null || mqttBrokerInfo.getMqttPort() == null) {
            throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), "Mqtt broker host or port empty").build();
        }
        Credentials mqttCredentials = credentialsServiceProvider.getOrCreateCredentials(CredentialsType.MQTT);
        if (StringUtils.isEmpty(mqttCredentials.getAccessKey())) {
            throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), "Mqtt broker username empty").build();
        }
        String brokerTopicPrefix = mqttPubSubServiceProvider.getFullTopicName(mqttCredentials.getAccessKey(), "") + DataCenter.INTEGRATION_ID;
        wrapper.saveValues(Map.of(
                MqttDeviceIntegrationEntities.MqttDeviceProperties::getBrokerServer, mqttBrokerInfo.getHost(),
                MqttDeviceIntegrationEntities.MqttDeviceProperties::getBrokerPort, mqttBrokerInfo.getMqttPort(),
                MqttDeviceIntegrationEntities.MqttDeviceProperties::getBrokerUsername, mqttCredentials.getAccessKey(),
                MqttDeviceIntegrationEntities.MqttDeviceProperties::getBrokerPassword, mqttCredentials.getAccessSecret(),
                MqttDeviceIntegrationEntities.MqttDeviceProperties::getBrokerTopicPrefix, brokerTopicPrefix,
                MqttDeviceIntegrationEntities.MqttDeviceProperties::getIsInitialized, true
        )).publishSync();
        DataCenter.setUserName(mqttCredentials.getAccessKey());
    }

    private String getLocalAddress() {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            return localHost.getHostAddress();
        } catch (Exception e) {
            return null;
        }
    }

    private void subscribeTopics() {
        List<Long> deviceTemplateIds = DataCenter.getDeviceTemplateIds();
        if (CollectionUtils.isEmpty(deviceTemplateIds)) {
            return;
        }

        List<DeviceTemplate> list = deviceTemplateServiceProvider.findByIds(deviceTemplateIds);
        if (CollectionUtils.isEmpty(list)) {
            return;
        }

        list.forEach(deviceTemplate -> {
            String topic = DataCenter.getTopic(deviceTemplate.getId());
            mqttDeviceMqttService.subscribe(topic, deviceTemplate.getId(), deviceTemplate.getContent());
        });
    }
}
