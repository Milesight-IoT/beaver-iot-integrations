package com.milesight.beaveriot.integrations.camthinkaiinference.api.model.request;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * author: Luxb
 * create: 2025/6/6 16:31
 **/
@Data
public class CamThinkModelInferRequest {
    public static final String INPUT_IMAGE_FIELD = "image";
    private Map<String, Object> inputs = new HashMap<>();
}
