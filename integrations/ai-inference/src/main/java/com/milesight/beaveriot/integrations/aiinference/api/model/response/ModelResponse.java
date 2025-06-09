package com.milesight.beaveriot.integrations.aiinference.api.model.response;

import lombok.Data;

import java.util.List;

/**
 * author: Luxb
 * create: 2025/6/5 14:18
 **/
@Data
public class ModelResponse {
    private List<ModelData> data;
    private Pagination pagination;

    @Data
    public static class ModelData {
        private String modelId;
        private String name;
        private String version;
        private String description;
        private String engineType;
    }
}