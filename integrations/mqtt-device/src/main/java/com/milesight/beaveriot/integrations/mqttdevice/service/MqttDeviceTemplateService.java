package com.milesight.beaveriot.integrations.mqttdevice.service;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.context.api.*;
import com.milesight.beaveriot.context.constants.IntegrationConstants;
import com.milesight.beaveriot.context.integration.model.*;
import com.milesight.beaveriot.context.integration.model.event.ExchangeEvent;
import com.milesight.beaveriot.context.model.DeviceTemplateModel;
import com.milesight.beaveriot.context.model.request.SearchDeviceTemplateRequest;
import com.milesight.beaveriot.context.model.response.DeviceTemplateInputResult;
import com.milesight.beaveriot.context.model.response.DeviceTemplateOutputResult;
import com.milesight.beaveriot.context.model.response.DeviceTemplateResponseData;
import com.milesight.beaveriot.eventbus.annotations.EventSubscribe;
import com.milesight.beaveriot.eventbus.api.Event;
import com.milesight.beaveriot.eventbus.api.EventResponse;
import com.milesight.beaveriot.integrations.mqttdevice.entity.MqttDeviceServiceEntities;
import com.milesight.beaveriot.integrations.mqttdevice.enums.ServerErrorCode;
import com.milesight.beaveriot.integrations.mqttdevice.model.request.*;
import com.milesight.beaveriot.integrations.mqttdevice.model.response.DeviceTemplateDefaultContentResponse;
import com.milesight.beaveriot.integrations.mqttdevice.model.response.DeviceTemplateDetailResponse;
import com.milesight.beaveriot.integrations.mqttdevice.model.response.DeviceTemplateInfoResponse;
import com.milesight.beaveriot.integrations.mqttdevice.model.response.DeviceTemplateTestResponse;
import com.milesight.beaveriot.integrations.mqttdevice.support.DataCenter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * author: Luxb
 * create: 2025/5/14 14:25
 **/
@Service
public class MqttDeviceTemplateService {
    private final IntegrationServiceProvider integrationServiceProvider;
    private final DeviceTemplateServiceProvider deviceTemplateServiceProvider;
    private final DeviceTemplateParserProvider deviceTemplateParserProvider;
    private final MqttDeviceService mqttDeviceService;
    private final DeviceServiceProvider deviceServiceProvider;
    private final EntityValueServiceProvider entityValueServiceProvider;

    public MqttDeviceTemplateService(IntegrationServiceProvider integrationServiceProvider, DeviceTemplateServiceProvider deviceTemplateServiceProvider, DeviceTemplateParserProvider deviceTemplateParserProvider, MqttDeviceService mqttDeviceService, DeviceServiceProvider deviceServiceProvider, EntityValueServiceProvider entityValueServiceProvider) {
        this.integrationServiceProvider = integrationServiceProvider;
        this.deviceTemplateServiceProvider = deviceTemplateServiceProvider;
        this.deviceTemplateParserProvider = deviceTemplateParserProvider;
        this.mqttDeviceService = mqttDeviceService;
        this.deviceServiceProvider = deviceServiceProvider;
        this.entityValueServiceProvider = entityValueServiceProvider;
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
            throw ServiceException.with(ServerErrorCode.TOPIC_EXISTS.getErrorCode(), ServerErrorCode.TOPIC_EXISTS.getErrorMessage()).build();
        }
        deviceTemplateServiceProvider.save(deviceTemplate);
        DataCenter.putTopic(topic, deviceTemplate.getId());
        mqttDeviceService.syncTemplates();
    }

    public Page<DeviceTemplateResponseData> searchDeviceTemplate(SearchDeviceTemplateRequest searchDeviceTemplateRequest) {
        Page<DeviceTemplateResponseData> deviceTemplateResponseDataPage = deviceTemplateServiceProvider.search(searchDeviceTemplateRequest);
        return deviceTemplateResponseDataPage.map(deviceTemplateResponseData -> DeviceTemplateInfoResponse.build(deviceTemplateResponseData, DataCenter.getTopic(Long.parseLong(deviceTemplateResponseData.getId()))));
    }

    public DeviceTemplateTestResponse testDeviceTemplate(Long id, TestDeviceTemplateRequest testDeviceTemplateRequest) {
        return inputAndGetTestResponse(DataCenter.INTEGRATION_ID, id, testDeviceTemplateRequest.getTestData());
    }

    public DeviceTemplateOutputResult output(String deviceKey, ExchangePayload payload) {
        return deviceTemplateParserProvider.output(deviceKey, payload);
    }

    private void flattenDeviceEntities(List<Entity> deviceEntities, Map<String, Entity> flatDeviceEntityMap) {
        for (Entity entity : deviceEntities) {
            flatDeviceEntityMap.put(entity.getKey(), entity);
            if (!CollectionUtils.isEmpty(entity.getChildren())) {
                flattenDeviceEntities(entity.getChildren(), flatDeviceEntityMap);
            }
        }
    }

    public void updateDeviceTemplate(Long id, UpdateDeviceTemplateRequest updateDeviceTemplateRequest) {
        DeviceTemplate deviceTemplate = deviceTemplateServiceProvider.findById(id);
        if (deviceTemplate == null) {
            throw ServiceException.with(ErrorCode.DATA_NO_FOUND).build();
        }
        String topic = updateDeviceTemplateRequest.getTopic();
        deviceTemplate.setName(updateDeviceTemplateRequest.getName());
        deviceTemplate.setContent(updateDeviceTemplateRequest.getContent());
        deviceTemplate.setDescription(updateDeviceTemplateRequest.getDescription());

        deviceTemplateServiceProvider.save(deviceTemplate);
        String oldTopic = DataCenter.getTopic(id);
        if (oldTopic != null && !oldTopic.equals(topic)) {
            DataCenter.removeTopic(oldTopic);
        }
        DataCenter.putTopic(topic, id);
        mqttDeviceService.syncTemplates();
    }

    public void batchDeleteDeviceTemplates(BatchDeleteDeviceTemplateRequest batchDeleteDeviceTemplateRequest) {
        if (!CollectionUtils.isEmpty(batchDeleteDeviceTemplateRequest.getIdList())) {
            batchDeleteDeviceTemplateRequest.getIdList().stream().map(Long::parseLong).toList().forEach(id -> {
                deviceTemplateServiceProvider.deleteById(id);
                DataCenter.removeTopicByTemplateId(id);
            });
            mqttDeviceService.syncTemplates();
        }
    }

    public DeviceTemplateDetailResponse getDeviceDetail(@PathVariable("id") Long id) {
        DeviceTemplate deviceTemplate = deviceTemplateServiceProvider.findById(id);
        DeviceTemplateModel deviceTemplateModel = deviceTemplateParserProvider.parse(deviceTemplate.getContent());
        return DeviceTemplateDetailResponse.build(convertToResponseData(deviceTemplate), DataCenter.getTopic(id),
                deviceTemplateModel.getDefinition() == null ? null : deviceTemplateModel.getDefinition().getInput(),
                deviceTemplateModel.getDefinition() ==  null ? null : deviceTemplateModel.getDefinition().getOutput(),
                deviceTemplateModel.getInitialEntities());
    }

    public void validate(ValidateDeviceTemplateRequest validateDeviceTemplateRequest) {
        deviceTemplateParserProvider.validate(validateDeviceTemplateRequest.getContent());
    }

    public DeviceTemplateDefaultContentResponse getDefaultDeviceTemplateContent() {
        return DeviceTemplateDefaultContentResponse.build(deviceTemplateParserProvider.defaultContent());
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

    @EventSubscribe(payloadKeyExpression = DataCenter.INTEGRATION_ID + ".integration." + MqttDeviceServiceEntities.DATA_INPUT_IDENTIFIER + ".*", eventType = ExchangeEvent.EventType.CALL_SERVICE)
    public EventResponse onDataInput(Event<MqttDeviceServiceEntities.DataInput> event) {
        MqttDeviceServiceEntities.DataInput dataInput = event.getPayload();
        String integrationId = dataInput.getIntegration();
        String jsonData = dataInput.getJsonData();
        String template = dataInput.getTemplate();

        if (StringUtils.isEmpty(integrationId)) {
            integrationId = DataCenter.INTEGRATION_ID;
        }
        Integration integration = integrationServiceProvider.getIntegration(integrationId);
        if (integration == null) {
            throw ServiceException.with(ServerErrorCode.INTEGRATION_NOT_FOUND.getErrorCode(), ServerErrorCode.INTEGRATION_NOT_FOUND.getErrorMessage()).build();
        }

        DeviceTemplate deviceTemplate = deviceTemplateServiceProvider.findByKey(template);
        if (deviceTemplate == null) {
            throw ServiceException.with(ServerErrorCode.TEMPLATE_NOT_FOUND.getErrorCode(), ServerErrorCode.TEMPLATE_NOT_FOUND.getErrorMessage()).build();
        }

        DeviceTemplateTestResponse deviceTemplateTestResponse = inputAndGetTestResponse(integrationId, deviceTemplate.getId(), jsonData);
        return getEventResponse(deviceTemplateTestResponse);
    }

    private static EventResponse getEventResponse(Object responseObj) {
        Map<String, Object> response = JsonUtils.toMap(responseObj);
        EventResponse eventResponse = EventResponse.empty();
        for (Map.Entry<String, Object> entry : response.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            eventResponse.put(key, value);
        }
        return eventResponse;
    }

    private DeviceTemplateTestResponse inputAndGetTestResponse(String integrationId, Long deviceTemplateId, String jsonData) {
        DeviceTemplateTestResponse testResponse = new DeviceTemplateTestResponse();
        DeviceTemplateInputResult result = deviceTemplateParserProvider.input(integrationId, deviceTemplateId, jsonData);
        Device device = result.getDevice();
        ExchangePayload payload = result.getPayload();
        if (device != null) {
            deviceServiceProvider.save(device);
            if (payload != null) {
                entityValueServiceProvider.saveValuesAndPublishSync(payload);
                Map<String, Entity> flatDeviceEntityMap = new HashMap<>();
                flattenDeviceEntities(device.getEntities(), flatDeviceEntityMap);
                flatDeviceEntityMap.forEach((key, value) -> {
                    if (payload.containsKey(key)) {
                        testResponse.addEntityData(value.getName(), payload.get(key));
                    }
                });
            }
        }
        return testResponse;
    }
}