package com.milesight.beaveriot.integrations.aiinference.api.model.response;

import lombok.Data;

import java.util.Map;

/**
 * author: Luxb
 * create: 2025/6/6 16:31
 **/
public class CamThinkModelInferResponse extends CamThinkResponse<CamThinkModelInferResponse.ModelInferData> {
    @Data
    public static class ModelInferData {
        private Map<String, Object> outputs;
    }
}
