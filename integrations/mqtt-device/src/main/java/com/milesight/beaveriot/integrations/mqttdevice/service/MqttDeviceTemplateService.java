package com.milesight.beaveriot.integrations.mqttdevice.service;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.context.api.DeviceTemplateParserProvider;
import com.milesight.beaveriot.context.api.DeviceTemplateServiceProvider;
import com.milesight.beaveriot.context.constants.IntegrationConstants;
import com.milesight.beaveriot.context.integration.model.DeviceTemplate;
import com.milesight.beaveriot.context.integration.model.DeviceTemplateBuilder;
import com.milesight.beaveriot.context.model.request.SearchDeviceTemplateRequest;
import com.milesight.beaveriot.context.model.response.DeviceTemplateDiscoverResponse;
import com.milesight.beaveriot.context.model.response.DeviceTemplateResponseData;
import com.milesight.beaveriot.integrations.mqttdevice.model.request.*;
import com.milesight.beaveriot.integrations.mqttdevice.model.response.DeviceTemplateDefaultContent;
import com.milesight.beaveriot.integrations.mqttdevice.model.response.DeviceTemplateInfoResponse;
import com.milesight.beaveriot.integrations.mqttdevice.support.DataCenter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * author: Luxb
 * create: 2025/5/14 14:25
 **/
@Service
public class MqttDeviceTemplateService {
    private final DeviceTemplateServiceProvider deviceTemplateServiceProvider;
    private final DeviceTemplateParserProvider deviceTemplateParserProvider;
    private final MqttDeviceMqttService mqttDeviceMqttService;

    public MqttDeviceTemplateService(DeviceTemplateServiceProvider deviceTemplateServiceProvider, DeviceTemplateParserProvider deviceTemplateParserProvider, MqttDeviceMqttService mqttDeviceMqttService) {
        this.deviceTemplateServiceProvider = deviceTemplateServiceProvider;
        this.deviceTemplateParserProvider = deviceTemplateParserProvider;
        this.mqttDeviceMqttService = mqttDeviceMqttService;
    }

    public void createDeviceTemplate(CreateDeviceTemplateRequest createDeviceTemplateRequest) {
        DeviceTemplate deviceTemplate = new DeviceTemplateBuilder(IntegrationConstants.SYSTEM_INTEGRATION_ID)
                .name(createDeviceTemplateRequest.getName())
                .content(createDeviceTemplateRequest.getContent())
                .description(createDeviceTemplateRequest.getDescription())
                .identifier(createDeviceTemplateRequest.getName())
                .build();

        String topic = createDeviceTemplateRequest.getTopic();
        if (DataCenter.isTopicExist(topic)) {
            throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), "topic exists").build();
        }
        deviceTemplateServiceProvider.save(deviceTemplate);
        DataCenter.putTopic(deviceTemplate.getKey(), topic);
        mqttDeviceMqttService.subscribe(topic, deviceTemplate.getKey(), deviceTemplate.getContent());
    }

    public Page<DeviceTemplateResponseData> searchDeviceTemplate(SearchDeviceTemplateRequest searchDeviceTemplateRequest) {
        Page<DeviceTemplateResponseData> deviceTemplateResponseDataPage = deviceTemplateServiceProvider.search(searchDeviceTemplateRequest);
        return deviceTemplateResponseDataPage.map(deviceTemplateResponseData -> new DeviceTemplateInfoResponse(deviceTemplateResponseData, DataCenter.getTopic(deviceTemplateResponseData.getKey())));
    }

    public DeviceTemplateDiscoverResponse testDeviceTemplate(String deviceTemplateKey, TestDeviceTemplateRequest testDeviceTemplateRequest) {
        String deviceTemplateContent = deviceTemplateServiceProvider.findByKey(deviceTemplateKey).getContent();
        return deviceTemplateParserProvider.discover(DataCenter.INTEGRATION_ID, testDeviceTemplateRequest.getTestData(), deviceTemplateKey, deviceTemplateContent);
    }

    public void updateDeviceTemplate(String deviceTemplateKey, UpdateDeviceTemplateRequest updateDeviceTemplateRequest) {
        DeviceTemplate deviceTemplate = deviceTemplateServiceProvider.findByKey(deviceTemplateKey);
        if (deviceTemplate == null) {
            throw ServiceException.with(ErrorCode.DATA_NO_FOUND).build();
        }
        String topic = updateDeviceTemplateRequest.getTopic();
        String deviceTemplateContent = updateDeviceTemplateRequest.getContent();

        deviceTemplate.setName(updateDeviceTemplateRequest.getName());
        String oldDeviceTemplateContent = deviceTemplate.getContent();
        deviceTemplate.setContent(updateDeviceTemplateRequest.getContent());
        deviceTemplate.setDescription(updateDeviceTemplateRequest.getDescription());

        String oldTopic = DataCenter.getTopic(deviceTemplateKey);
        DataCenter.putTopic(deviceTemplate.getKey(), topic);

        deviceTemplateServiceProvider.save(deviceTemplate);
        if (oldTopic.equals(topic)) {
            if (!oldDeviceTemplateContent.equals(deviceTemplateContent)) {
                mqttDeviceMqttService.unsubscribe(oldTopic);
                mqttDeviceMqttService.subscribe(topic, deviceTemplate.getKey(), deviceTemplateContent);
            }
        } else {
            mqttDeviceMqttService.unsubscribe(oldTopic);
            mqttDeviceMqttService.subscribe(topic, deviceTemplate.getKey(), deviceTemplateContent);
        }
    }

    public void batchDeleteDeviceTemplates(BatchDeleteDeviceTemplateRequest batchDeleteDeviceTemplateRequest) {
        if (!CollectionUtils.isEmpty(batchDeleteDeviceTemplateRequest.getKeyList())) {
            batchDeleteDeviceTemplateRequest.getKeyList().forEach(deviceTemplateKey -> {
                String topic = DataCenter.getTopic(deviceTemplateKey);
                if (StringUtils.isNotEmpty(topic)) {
                    DataCenter.removeTopic(deviceTemplateKey);
                    mqttDeviceMqttService.unsubscribe(topic);
                }
                deviceTemplateServiceProvider.deleteByKey(deviceTemplateKey);
            });
        }
    }

    public DeviceTemplateInfoResponse getDeviceDetail(@PathVariable("deviceTemplateKey") String deviceTemplateKey) {
        DeviceTemplate deviceTemplate = deviceTemplateServiceProvider.findByKey(deviceTemplateKey);
        return new DeviceTemplateInfoResponse(convertToResponseData(deviceTemplate), DataCenter.getTopic(deviceTemplateKey));
    }

    public void validate(ValidateDeviceTemplateRequest validateDeviceTemplateRequest) {
        deviceTemplateParserProvider.validate(validateDeviceTemplateRequest.getContent());
    }

    public DeviceTemplateDefaultContent getDefaultDeviceTemplateContent() {
        return DeviceTemplateDefaultContent.build(deviceTemplateParserProvider.getDefaultDeviceTemplateContent());
    }

    private DeviceTemplateResponseData convertToResponseData(DeviceTemplate deviceTemplate) {
        DeviceTemplateResponseData deviceTemplateResponseData = new DeviceTemplateResponseData();
        deviceTemplateResponseData.setId(deviceTemplate.getId().toString());
        deviceTemplateResponseData.setKey(deviceTemplate.getKey());
        deviceTemplateResponseData.setName(deviceTemplate.getName());
        deviceTemplateResponseData.setContent(deviceTemplate.getContent());
        deviceTemplateResponseData.setDescription(deviceTemplate.getDescription());
        deviceTemplateResponseData.setIntegration(IntegrationConstants.SYSTEM_INTEGRATION_ID);
        deviceTemplateResponseData.setAdditionalData(deviceTemplate.getAdditional());
        deviceTemplateResponseData.setCreatedAt(deviceTemplate.getCreatedAt());
        deviceTemplateResponseData.setUpdatedAt(deviceTemplate.getUpdatedAt());

        return deviceTemplateResponseData;
    }
}
