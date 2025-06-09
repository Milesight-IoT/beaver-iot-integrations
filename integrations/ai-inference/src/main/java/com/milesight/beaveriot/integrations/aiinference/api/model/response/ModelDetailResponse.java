package com.milesight.beaveriot.integrations.aiinference.api.model.response;

import lombok.Data;

import java.util.List;

/**
 * author: Luxb
 * create: 2025/6/6 10:52
 **/
@Data
public class ModelDetailResponse {

    private String modelId;
    private String name;
    private String version;
    private String description;
    private String engineType;

    private Parameters parameters;

    @Data
    public static class Parameters {
        private List<Input> input;
        private List<Output> output;
    }

    @Data
    public static class Input {
        private String name;
        private String type;
        private String description;
        private Boolean required;
    }

    @Data
    public static class Output {
        private String name;
        private String type;
        private String description;
        private Object itemSchema; // 可以替换为具体类型，如果知道结构的话
    }
}