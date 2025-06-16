package com.milesight.beaveriot.integrations.aiinference.api.model.request;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * author: Luxb
 * create: 2025/6/6 16:31
 **/
@Data
public class CamThinkModelInferRequest {
    private Map<String, Object> inputs = new HashMap<>();
}
