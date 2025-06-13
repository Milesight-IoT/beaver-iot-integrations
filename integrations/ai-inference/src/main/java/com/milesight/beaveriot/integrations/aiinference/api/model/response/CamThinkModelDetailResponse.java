package com.milesight.beaveriot.integrations.aiinference.api.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

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
        // TO modify
    }
}