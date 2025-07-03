package com.milesight.beaveriot.integrations.camthinkaiinference.model.response;

import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.integrations.camthinkaiinference.api.model.response.CamThinkModelDetailResponse;
import lombok.Data;

import java.util.List;

/**
 * author: Luxb
 * create: 2025/6/9 8:42
 **/
@Data
public class ModelOutputSchemaResponse {
    private List<Entity> inputEntities;
    private List<CamThinkModelDetailResponse.OutputSchema> outputSchema;
}
