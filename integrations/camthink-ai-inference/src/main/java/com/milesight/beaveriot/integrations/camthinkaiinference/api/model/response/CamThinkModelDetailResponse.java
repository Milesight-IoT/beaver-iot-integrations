package com.milesight.beaveriot.integrations.camthinkaiinference.api.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

/**
 * author: Luxb
 * create: 2025/6/6 10:52
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class CamThinkModelDetailResponse extends CamThinkResponse<CamThinkModelDetailResponse.DetailData>{
    @Data
    public static class DetailData {
        private String id;
        private String name;
        private String remark;
        private String engineType;
        private Map<String, Object> config;
        private List<InputSchema> inputSchema;
        private List<OutputSchema> outputSchema;
    }

    @Data
    public static class InputSchema {
        private String name;
        private String description;
        private String type;
        private boolean required;
        private String format;
        @JsonProperty("default")
        private String defaultValue;
        private Double minimum;
        private Double maximum;
    }

    @Data
    public static class OutputSchema {
        private String name;
        private String description;
        private boolean required;
        private String type;
        private OutputSchemaItem items;
    }

    @Data
    public static class OutputSchemaItem {
        private String description;
        private boolean required;
        private String type;
        private Map<String, OutputSchemaField> properties;
    }

    @Data
    public static class OutputSchemaField {
        private String description;
        private String type;
        private boolean required;
        private Integer minLength;
        private Integer maxLength;
        private Map<String, OutputSchemaField> properties;
        private OutputSchemaItem items;
    }
}