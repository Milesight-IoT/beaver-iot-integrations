package com.milesight.beaveriot.integrations.aiinference.api.config;

import lombok.Builder;
import lombok.Data;

/**
 * author: Luxb
 * create: 2025/6/5 8:34
 **/
@Builder
@Data
public class Config {
    private String baseUrl;
    private String token;
    public static final String URL_MODELS = "/api/v1/models";
    public static final String URL_MODEL_DETAIL = "/api/v1/models/{0}";
    public static final String URL_MODEL_INFER = "/api/v1/models/{0}/infer";

    public String getModelsUrl() {
        return baseUrl + URL_MODELS;
    }

    public String getModelDetailUrl() {
        return baseUrl + URL_MODEL_DETAIL;
    }

    public String getModelInferUrl() {
        return baseUrl + URL_MODEL_INFER;
    }
}
