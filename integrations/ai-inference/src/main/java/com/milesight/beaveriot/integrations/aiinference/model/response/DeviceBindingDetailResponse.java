package com.milesight.beaveriot.integrations.aiinference.model.response;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * author: Luxb
 * create: 2025/6/20 10:29
 **/
@Data
public class DeviceBindingDetailResponse {
    private String modelId;
    private String imageEntityKey;
    private Map<String, Object> inferInputs;
    private List<OutputItem> inferOutputs;

    @Data
    public static class OutputItem {
        private String fieldName;
        private String entityName;
    }
}