package com.milesight.beaveriot.integrations.ollama.api.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * @Author yuanh
 * @Description
 * @Package com.milesight.beaveriot.integrations.ollama.api.config
 * @Date 2025/2/7 13:14
 */
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class Config {
    private String baseUrl;

    private static final String GENERATE_COMPLETION_URL = "/api/generate";
    private static final String TAGS_URL = "/api/tags";

    public String getGenerateCompletionUrl() {
        return baseUrl + GENERATE_COMPLETION_URL;
    }

    public String getTagsUrl() {
        return baseUrl + TAGS_URL;
    }
}
