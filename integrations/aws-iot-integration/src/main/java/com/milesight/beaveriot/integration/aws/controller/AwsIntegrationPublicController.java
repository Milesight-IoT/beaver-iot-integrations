package com.milesight.beaveriot.integration.aws.controller;

import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.parser.model.LnsAwsPayload;
import com.milesight.beaveriot.integration.aws.service.AwsWebhookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * webhook
 */
@Slf4j
@RestController
@RequestMapping("/public/integration/aws")
public class AwsIntegrationPublicController {

    @Autowired
    private AwsWebhookService awsWebhookService;

    @PostMapping("/webhook")
    public String webhook(@RequestBody String payload) {
        log.info("Received webhook data: {}", payload);
        try {
            // LORA设备接收值
            LnsAwsPayload lnsAwsPayload = JsonUtils.fromJSON(payload, LnsAwsPayload.class);
            awsWebhookService.handleWebhookData(lnsAwsPayload);
        } catch (Exception e) {
            log.error("Received webhook data error: {}", e.getMessage());
        }
        return "success";
    }

}
