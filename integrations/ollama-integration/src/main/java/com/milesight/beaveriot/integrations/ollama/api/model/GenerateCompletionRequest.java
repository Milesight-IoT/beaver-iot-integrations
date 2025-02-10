package com.milesight.beaveriot.integrations.ollama.api.model;

import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.integrations.ollama.entity.OllamaServiceEntities;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

import java.util.List;

/**
 * @Author yuanh
 * @Description
 * @Package com.milesight.beaveriot.integrations.ollama.api.model
 * @Date 2025/2/7 10:29
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GenerateCompletionRequest {
    private String model;
    private String prompt;
    private String suffix;
    private Object images;
    private Object format;
    private Object options;
    private Object system;
    private Object template;
    private Boolean stream;
    private Boolean row;
    private Integer keepAlive;

    public GenerateCompletionRequest converterPayload(OllamaServiceEntities.GenerateCompletion payload) {
        if (StringUtils.isNotBlank(payload.getModel())){
            model = payload.getModel();
        }
        if (StringUtils.isNotBlank(payload.getPrompt())){
            prompt = payload.getPrompt();
        }
        if (StringUtils.isNotBlank(payload.getSuffix())){
            suffix = payload.getSuffix();
        }
        if (StringUtils.isNotBlank(payload.getImages())){
            images = toJsonNode(payload.getImages());
        }
        if (StringUtils.isNotBlank(payload.getFormat())) {
            format = toJsonNode(payload.getFormat());
        }
        if (StringUtils.isNotBlank(payload.getOptions())) {
            options = JsonUtils.toJsonNode(payload.getOptions());
        }
        if (StringUtils.isNotBlank(payload.getSystem())) {
            system = toJsonNode(payload.getSystem());
        }
        if (StringUtils.isNotBlank(payload.getTemplate())) {
            template = JsonUtils.toJsonNode(payload.getTemplate());
        }
        stream = false;
        if (payload.getRow() != null){
            row = payload.getRow();
        }
        if (payload.getKeepAlive() != null){
            keepAlive = payload.getKeepAlive();
        }
        return this;
    }

    private Object toJsonNode(String json) {
        try {
            return JsonUtils.toJsonNode(json);
        } catch (Exception e) {
            return json;
        }
    }
}
