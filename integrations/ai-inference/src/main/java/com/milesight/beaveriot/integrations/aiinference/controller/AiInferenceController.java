package com.milesight.beaveriot.integrations.aiinference.controller;

import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.response.ResponseBody;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import com.milesight.beaveriot.integrations.aiinference.api.enums.ServerErrorCode;
import com.milesight.beaveriot.integrations.aiinference.api.model.response.ModelDetailResponse;
import com.milesight.beaveriot.integrations.aiinference.api.model.response.ModelOutputResponse;
import com.milesight.beaveriot.integrations.aiinference.constant.Constants;
import com.milesight.beaveriot.integrations.aiinference.service.AiInferenceService;
import org.springframework.web.bind.annotation.*;

/**
 * author: Luxb
 * create: 2025/5/30 16:13
 **/
@RestController
@RequestMapping("/" + Constants.INTEGRATION_ID)
public class AiInferenceController {
    private final AiInferenceService service;

    public AiInferenceController(AiInferenceService service) {
        this.service = service;
    }

    @PostMapping("/model/{modelId}/sync-detail")
    public ResponseBody<ModelOutputResponse> fetchModelDetail(@PathVariable("modelId") String modelId) {
        ModelDetailResponse modelDetailResponse = service.fetchModelDetail(modelId);
        if (modelDetailResponse == null) {
            throw ServiceException.with(ServerErrorCode.SERVER_DATA_NOT_FOUND.getErrorCode(), ServerErrorCode.SERVER_DATA_NOT_FOUND.getErrorMessage()).build();
        }
        ModelOutputResponse outputResponse = new ModelOutputResponse(modelDetailResponse);
        return ResponseBuilder.success(outputResponse);
    }
}
