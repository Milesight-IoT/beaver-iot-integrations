package com.milesight.beaveriot.integrations.camthinkaiinference.api.config;

import com.milesight.beaveriot.context.integration.wrapper.AnnotatedEntityWrapper;
import com.milesight.beaveriot.integrations.camthinkaiinference.entity.CamThinkAiInferenceConnectionPropertiesEntities;
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
        return getBaseUrl() + URL_MODELS;
    }

    public String getModelDetailUrl() {
        return getBaseUrl() + URL_MODEL_DETAIL;
    }

    public String getModelInferUrl() {
        return getBaseUrl() + URL_MODEL_INFER;
    }

    public String getBaseUrl() {
        AnnotatedEntityWrapper<CamThinkAiInferenceConnectionPropertiesEntities.CamThinkAiInferenceProperties> wrapper = new AnnotatedEntityWrapper<>();
        return (String) wrapper.getValue(CamThinkAiInferenceConnectionPropertiesEntities.CamThinkAiInferenceProperties::getBaseUrl).orElse("");
    }

    public String getToken() {
        AnnotatedEntityWrapper<CamThinkAiInferenceConnectionPropertiesEntities.CamThinkAiInferenceProperties> wrapper = new AnnotatedEntityWrapper<>();
        return (String) wrapper.getValue(CamThinkAiInferenceConnectionPropertiesEntities.CamThinkAiInferenceProperties::getToken).orElse("");
    }
}
