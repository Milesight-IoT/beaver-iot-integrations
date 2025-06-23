package com.milesight.beaveriot.integrations.aiinference.api.model.response;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * author: Luxb
 * create: 2025/6/6 16:31
 **/
public class CamThinkModelInferResponse extends CamThinkResponse<CamThinkModelInferResponse.ModelInferData> {
    @Data
    public static class ModelInferData {
        public static final String FIELD_DATA = "data";
        private Map<String, Object> outputs;

        @Data
        public static class OutputData {
            private String fileName;
            private List<Detection> detections;

            @Data
            public static class Detection {
                private List<Integer> box;
                private Double conf;
                private String cls;
                private List<List<Integer>> masks;
                private List<List<Double>> points;
                private List<List<Integer>> skeleton;
            }
        }
    }
}
