package com.milesight.beaveriot.integrations.camthinkaiinference.api.model.response;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * author: Luxb
 * create: 2025/6/6 16:31
 **/
public class CamThinkModelInferResponse extends CamThinkResponse<CamThinkModelInferResponse.ModelInferData> {
    public static final int BOX_SIZE = 4;
    public static final int BOX_X_INDEX = 0;
    public static final int BOX_Y_INDEX = 1;
    public static final int BOX_WIDTH_INDEX = 2;
    public static final int BOX_HEIGHT_INDEX = 3;
    public static final int MASK_SIZE = 2;
    public static final int MASK_POINT_X_INDEX = 0;
    public static final int MASK_POINT_Y_INDEX = 1;
    public static final int POINT_SIZE = 4;
    public static final int POINT_X_INDEX = 0;
    public static final int POINT_Y_INDEX = 1;
    public static final int POINT_ID_INDEX = 2;
    public static final int LINE_SIZE = 2;
    public static final int LINE_START_POINT_ID_INDEX = 0;
    public static final int LINE_END_POINT_ID_INDEX = 1;
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
