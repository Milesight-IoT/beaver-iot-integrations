package com.milesight.beaveriot.integrations.mqttdevice.controller;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.response.ResponseBody;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import com.milesight.beaveriot.context.api.CredentialsServiceProvider;
import com.milesight.beaveriot.context.api.MqttPubSubServiceProvider;
import com.milesight.beaveriot.context.integration.enums.CredentialsType;
import com.milesight.beaveriot.context.integration.model.Credentials;
import com.milesight.beaveriot.context.mqtt.model.MqttBrokerInfo;
import com.milesight.beaveriot.integrations.mqttdevice.model.response.BrokerInfoResponse;
import com.milesight.beaveriot.integrations.mqttdevice.support.DataCenter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;

/**
 * author: Luxb
 * create: 2025/5/26 16:49
 **/
@RestController
@RequestMapping("/" + DataCenter.INTEGRATION_ID)
public class MqttDeviceController {
    private final MqttPubSubServiceProvider mqttPubSubServiceProvider;
    private final CredentialsServiceProvider credentialsServiceProvider;

    public MqttDeviceController(MqttPubSubServiceProvider mqttPubSubServiceProvider, CredentialsServiceProvider credentialsServiceProvider) {
        this.mqttPubSubServiceProvider = mqttPubSubServiceProvider;
        this.credentialsServiceProvider = credentialsServiceProvider;
    }

    @GetMapping("/broker-info")
    public ResponseBody<BrokerInfoResponse> getBrokerInfo() {
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
        return ResponseBuilder.success(BrokerInfoResponse.builder()
                .server(mqttBrokerInfo.getHost())
                .port(mqttBrokerInfo.getMqttPort())
                .username(mqttCredentials.getAccessKey())
                .password(mqttCredentials.getAccessSecret())
                .topicPrefix(brokerTopicPrefix)
                .build());
    }

    private String getLocalAddress() {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            return localHost.getHostAddress();
        } catch (Exception e) {
            return null;
        }
    }
}
