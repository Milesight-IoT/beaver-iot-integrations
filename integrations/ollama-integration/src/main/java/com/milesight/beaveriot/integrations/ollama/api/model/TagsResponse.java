package com.milesight.beaveriot.integrations.ollama.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author yuanh
 * @Description
 * @Package com.milesight.beaveriot.integrations.ollama.api.model
 * @Date 2025/2/7 10:15
 */
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class TagsResponse extends BaseResponse {
    private List<Models> models;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Details {
        private String parentModel;
        private String format;
        private String family;
        private List<String> families;
        private String parameterSize;
        private String quantizationLevel;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Models {
        private String name;
        private String model;
        private String modifiedAt;
        private Long size;
        private String digest;
        private Details details;
    }

    public List<String> getModelsAsList() {
        if (models != null && !models.isEmpty()) {
            return models.stream()
                    .map(Models::getModel)
                    .collect(Collectors.toList());
        }
        return null;
    }
}
