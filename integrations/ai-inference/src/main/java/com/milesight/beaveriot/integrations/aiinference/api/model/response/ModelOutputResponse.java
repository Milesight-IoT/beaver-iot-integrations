package com.milesight.beaveriot.integrations.aiinference.api.model.response;

import lombok.Data;

import java.util.List;

/**
 * author: Luxb
 * create: 2025/6/9 8:42
 **/
@Data
public class ModelOutputResponse {
    private List<CamThinkModelDetailResponse.OutputSchema> outputSchema;

    public ModelOutputResponse(CamThinkModelDetailResponse camThinkModelDetailResponse) {
        this.outputSchema = camThinkModelDetailResponse.getData().getOutputSchema();
    }
}
