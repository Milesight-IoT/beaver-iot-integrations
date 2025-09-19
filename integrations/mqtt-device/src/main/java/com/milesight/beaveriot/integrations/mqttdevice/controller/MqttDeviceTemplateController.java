package com.milesight.beaveriot.integrations.mqttdevice.controller;

import com.milesight.beaveriot.base.response.ResponseBody;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import com.milesight.beaveriot.context.model.request.SearchDeviceTemplateRequest;
import com.milesight.beaveriot.context.model.response.DeviceTemplateResponseData;
import com.milesight.beaveriot.integrations.mqttdevice.model.request.*;
import com.milesight.beaveriot.integrations.mqttdevice.model.response.DeviceTemplateDefaultContentResponse;
import com.milesight.beaveriot.integrations.mqttdevice.model.response.DeviceTemplateDetailResponse;
import com.milesight.beaveriot.integrations.mqttdevice.model.response.DeviceTemplateTestResponse;
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

    @GetMapping("/default")
    public ResponseBody<DeviceTemplateDefaultContentResponse> getDefaultDeviceTemplateContent() {
        return ResponseBuilder.success(mqttDeviceTemplateService.getDefaultDeviceTemplateContent());
    }

    @PostMapping("/validate")
    public ResponseBody<Void> validate(@RequestBody ValidateDeviceTemplateRequest validateDeviceTemplateRequest) {
        mqttDeviceTemplateService.validate(validateDeviceTemplateRequest);
        return ResponseBuilder.success();
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

    @PutMapping("/{id}")
    public ResponseBody<Void> updateDeviceTemplate(@PathVariable("id") Long id, @RequestBody UpdateDeviceTemplateRequest updateDeviceTemplateRequest) {
        mqttDeviceTemplateService.updateDeviceTemplate(id, updateDeviceTemplateRequest);
        return ResponseBuilder.success();
    }

    @PostMapping("/{id}/test")
    public ResponseBody<DeviceTemplateTestResponse> testDeviceTemplate(@PathVariable("id") Long id, @RequestBody TestDeviceTemplateRequest testDeviceTemplateRequest) {
        DeviceTemplateTestResponse deviceTemplateTestResponse = mqttDeviceTemplateService.testDeviceTemplate(id, testDeviceTemplateRequest);
        return ResponseBuilder.success(deviceTemplateTestResponse);
    }

    @GetMapping("/{id}")
    public ResponseBody<DeviceTemplateDetailResponse> getDeviceTemplateDetail(@PathVariable("id") Long id) {
        return ResponseBuilder.success(mqttDeviceTemplateService.getDeviceTemplateDetail(id));
    }

    @PostMapping("/batch-delete")
    public ResponseBody<Void> batchDeleteDeviceTemplates(@RequestBody BatchDeleteDeviceTemplateRequest batchDeleteDeviceTemplateRequest) {
        mqttDeviceTemplateService.batchDeleteDeviceTemplates(batchDeleteDeviceTemplateRequest);
        return ResponseBuilder.success();
    }
}
