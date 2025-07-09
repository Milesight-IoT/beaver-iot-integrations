package com.milesight.beaveriot.integrations.camthinkaiinference.api.model.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * author: Luxb
 * create: 2025/6/5 14:18
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class CamThinkModelListResponse extends CamThinkResponse<List<CamThinkModelListResponse.ModelData>>{
    @Data
    public static class ModelData {
        private String id;
        private String name;
        private String version;
        private String description;
        private String engineType;
    }
}