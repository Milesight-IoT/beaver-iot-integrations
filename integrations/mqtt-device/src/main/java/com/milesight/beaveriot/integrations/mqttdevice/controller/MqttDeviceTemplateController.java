package com.milesight.beaveriot.integrations.mqttdevice.controller;

import com.milesight.beaveriot.base.response.ResponseBody;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import com.milesight.beaveriot.context.model.request.SearchDeviceTemplateRequest;
import com.milesight.beaveriot.context.model.response.DeviceTemplateDiscoverResponse;
import com.milesight.beaveriot.context.model.response.DeviceTemplateResponseData;
import com.milesight.beaveriot.integrations.mqttdevice.model.request.*;
import com.milesight.beaveriot.integrations.mqttdevice.model.response.DeviceTemplateDefaultContent;
import com.milesight.beaveriot.integrations.mqttdevice.model.response.DeviceTemplateInfoResponse;
import com.milesight.beaveriot.integrations.mqttdevice.service.MqttDeviceTemplateService;
import com.milesight.beaveriot.integrations.mqttdevice.support.DataCenter;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

/**
 * author: Luxb
 * create: 2025/5/26 16:49
 **/
@RestController
@RequestMapping("/" + DataCenter.INTEGRATION_ID + "/device-template")
public class MqttDeviceTemplateController {
    private final MqttDeviceTemplateService mqttDeviceTemplateService;

    public MqttDeviceTemplateController(MqttDeviceTemplateService mqttDeviceTemplateService) {
        this.mqttDeviceTemplateService = mqttDeviceTemplateService;
    }

    @PostMapping
    public ResponseBody<String> createDeviceTemplate(@RequestBody CreateDeviceTemplateRequest createDeviceTemplateRequest) {
        mqttDeviceTemplateService.createDeviceTemplate(createDeviceTemplateRequest);
        return ResponseBuilder.success();
    }

    @PostMapping("/search")
    public ResponseBody<Page<DeviceTemplateResponseData>> searchDeviceTemplate(@RequestBody SearchDeviceTemplateRequest searchDeviceTemplateRequest) {
        return ResponseBuilder.success(mqttDeviceTemplateService.searchDeviceTemplate(searchDeviceTemplateRequest));
    }

    @PostMapping("/{deviceTemplateId}/test")
    public ResponseBody<DeviceTemplateDiscoverResponse> testDeviceTemplate(@PathVariable("deviceTemplateId") Long deviceTemplateId, @RequestBody TestDeviceTemplateRequest testDeviceTemplateRequest) {
        DeviceTemplateDiscoverResponse deviceTemplateDiscoverResponse = mqttDeviceTemplateService.testDeviceTemplate(deviceTemplateId, testDeviceTemplateRequest);
        return ResponseBuilder.success(deviceTemplateDiscoverResponse);
    }

    @PutMapping("/{deviceTemplateId}")
    public ResponseBody<Void> updateDeviceTemplate(@PathVariable("deviceTemplateId") Long deviceTemplateId, @RequestBody UpdateDeviceTemplateRequest updateDeviceTemplateRequest) {
        mqttDeviceTemplateService.updateDeviceTemplate(deviceTemplateId, updateDeviceTemplateRequest);
        return ResponseBuilder.success();
    }

    @PostMapping("/batch-delete")
    public ResponseBody<Void> batchDeleteDeviceTemplates(@RequestBody BatchDeleteDeviceTemplateRequest batchDeleteDeviceTemplateRequest) {
        mqttDeviceTemplateService.batchDeleteDeviceTemplates(batchDeleteDeviceTemplateRequest);
        return ResponseBuilder.success();
    }

    @GetMapping("/{deviceTemplateId}")
    public ResponseBody<DeviceTemplateInfoResponse> getDeviceDetail(@PathVariable("deviceTemplateId") Long deviceTemplateId) {
        return ResponseBuilder.success(mqttDeviceTemplateService.getDeviceDetail(deviceTemplateId));
    }

    @PostMapping("/validate")
    public ResponseBody<Void> validate(@RequestBody ValidateDeviceTemplateRequest validateDeviceTemplateRequest) {
        mqttDeviceTemplateService.validate(validateDeviceTemplateRequest);
        return ResponseBuilder.success();
    }

    @GetMapping("/content/default")
    public ResponseBody<DeviceTemplateDefaultContent> getDefaultDeviceTemplateContent() {
        return ResponseBuilder.success(mqttDeviceTemplateService.getDefaultDeviceTemplateContent());
    }
}
