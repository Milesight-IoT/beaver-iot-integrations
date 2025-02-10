package com.milesight.beaveriot.integrations.ollama.api;

import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.integrations.ollama.api.config.Config;
import com.milesight.beaveriot.integrations.ollama.api.model.GenerateCompletionRequest;
import com.milesight.beaveriot.integrations.ollama.api.model.GenerateCompletionResponse;
import com.milesight.beaveriot.integrations.ollama.api.model.TagsResponse;
import com.milesight.beaveriot.integrations.ollama.util.OkHttpUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author yuanh
 * @Description
 * @Package com.milesight.beaveriot.integrations.ollama.api
 * @Date 2025/2/6 17:38
 */
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class OllamaClient {
    private Config config;

    /**
     * Tests whether the base URL is valid and reachable.
     *
     * @return true if the base URL is valid and reachable; otherwise, false.
     */
    public boolean test() {
        String baseUrl = config.getBaseUrl();
        boolean apiStatus;
        try {
            apiStatus = validBaseUrl(baseUrl);
        } catch (Exception e) {
            log.warn("[Not reachable]: " + baseUrl);
            apiStatus = false;
        }
        return apiStatus;
    }

    private boolean validBaseUrl(String urlString) {
        try {
            OkHttpUtil.get(urlString, null);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Fetches the tags information.
     *
     * @return TagsResponse object containing the tags information.
     */
    @SneakyThrows
    public TagsResponse getTags() {
        try {
            String body = OkHttpUtil.get(config.getTagsUrl(), null);
            return JsonUtils.fromJSON(body, TagsResponse.class);
        } catch (Exception e) {
            log.error("Error occurs while getting tags", e);
            return TagsResponse.builder().error(e.getMessage()).build();
        }

    }

    /**
     * Sends a POST request to generate completion and returns the response.
     *
     * @param request The GenerateCompletionRequest object containing the details of the request.
     * @return A GenerateCompletionResponse object representing the response from the server.
     */
    public GenerateCompletionResponse postGenerateCompletion(GenerateCompletionRequest request) {
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            String body = OkHttpUtil.postJson(config.getGenerateCompletionUrl(), headers, JsonUtils.toJSON(request));
            return JsonUtils.fromJSON(body, GenerateCompletionResponse.class);
        } catch (Exception e) {
            log.error("Error occurs while generating completion", e);
            return GenerateCompletionResponse.builder().error(e.getMessage()).build();
        }
    }

}
