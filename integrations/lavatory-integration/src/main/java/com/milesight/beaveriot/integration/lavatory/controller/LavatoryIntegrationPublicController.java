package com.milesight.beaveriot.integration.lavatory.controller;

import com.milesight.beaveriot.integration.lavatory.model.WebhookPayload;
import com.milesight.beaveriot.integration.lavatory.service.LavatoryWebhookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 *
 */
@Slf4j
@RestController
@RequestMapping("/public/integration/lavatory")
public class LavatoryIntegrationPublicController {

    @Autowired
    private LavatoryWebhookService lavatoryWebhookService;

    @PostMapping("/webhook")
    public String webhook(@RequestHeader(name = "x-msc-request-signature") String signature,
                          @RequestHeader(name = "x-msc-webhook-uuid") String webhookUuid,
                          @RequestHeader(name = "x-msc-request-timestamp") String requestTimestamp,
                          @RequestHeader(name = "x-msc-request-nonce") String requestNonce,
                          @RequestBody List<WebhookPayload> webhookPayloads) {
        lavatoryWebhookService.handleWebhookData(signature, webhookUuid, requestTimestamp, requestNonce, webhookPayloads);
        return "success";
    }

}
