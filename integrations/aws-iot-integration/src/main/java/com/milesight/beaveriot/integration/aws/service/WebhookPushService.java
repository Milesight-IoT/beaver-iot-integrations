package com.milesight.beaveriot.integration.aws.service;


import cn.hutool.core.util.StrUtil;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.integration.aws.entity.AwsConnectionPropertiesEntities;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
@Slf4j
public class WebhookPushService {

    @Autowired
    private EntityValueServiceProvider entityValueServiceProvider;


    public HttpResponse<String> alarmPush(String jsonPayload) {
        val webhookUrl = entityValueServiceProvider.findValueByKey(AwsConnectionPropertiesEntities.getKey("openapi.alarm_url"));
        if (webhookUrl == null || webhookUrl.isNull() || StrUtil.isEmpty(webhookUrl.textValue())) {
            return null;
        }
        try {
            // Create HttpClient
            HttpClient client = HttpClient.newHttpClient();
            // Create HttpRequest
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl.textValue()))
                    .header("User-Agent", "Apifox/1.0.0 (https://apifox.com)")
                    .header("Content-Type", "application/json")
                    .header("Accept", "*/*")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            // Send the request and get the response
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Response status code: " + response.statusCode());
            System.out.println("Response body: " + response.body());
            return response;
        } catch (Exception e) {
            log.error("Error sending webhook request: ", e);
            return null;
        }
    }
    public HttpResponse<String> webhookPush(String jsonPayload) {
        val webhookUrl = entityValueServiceProvider.findValueByKey(AwsConnectionPropertiesEntities.getKey("openapi.webhook_url"));
        if (webhookUrl == null || webhookUrl.isNull() || StrUtil.isEmpty(webhookUrl.textValue())) {
            return null;
        }
        try {
            // Create HttpClient
            HttpClient client = HttpClient.newHttpClient();
            // Create HttpRequest
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl.textValue()))
                    .header("User-Agent", "Apifox/1.0.0 (https://apifox.com)")
                    .header("Content-Type", "application/json")
                    .header("Accept", "*/*")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            // Send the request and get the response
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Response status code: " + response.statusCode());
            System.out.println("Response body: " + response.body());
            return response;
        } catch (Exception e) {
            log.error("Error sending webhook request: ", e);
            return null;
        }
    }

}
