package com.milesight.beaveriot.integrations.camthinkaiinference.model.response;

import com.milesight.beaveriot.integrations.camthinkaiinference.api.model.response.CamThinkModelInferResponse;
import lombok.Data;

import java.util.Map;

/**
 * author: Luxb
 * create: 2025/6/9 8:42
 **/
@Data
public class ModelInferResponse {
    private Map<String, Object> outputs;

    public ModelInferResponse(CamThinkModelInferResponse camThinkModelInferResponse) {
        this.outputs = camThinkModelInferResponse.getData().getOutputs();
    }
}
