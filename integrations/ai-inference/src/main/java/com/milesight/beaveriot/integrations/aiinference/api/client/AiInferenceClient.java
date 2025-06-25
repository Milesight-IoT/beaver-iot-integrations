package com.milesight.beaveriot.integrations.aiinference.api.client;

import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.context.integration.wrapper.AnnotatedEntityWrapper;
import com.milesight.beaveriot.integrations.aiinference.api.config.Config;
import com.milesight.beaveriot.integrations.aiinference.api.enums.ServerErrorCode;
import com.milesight.beaveriot.integrations.aiinference.api.model.request.CamThinkModelInferRequest;
import com.milesight.beaveriot.integrations.aiinference.api.model.response.ClientResponse;
import com.milesight.beaveriot.integrations.aiinference.api.model.response.CamThinkModelDetailResponse;
import com.milesight.beaveriot.integrations.aiinference.api.model.response.CamThinkModelInferResponse;
import com.milesight.beaveriot.integrations.aiinference.api.model.response.CamThinkModelListResponse;
import com.milesight.beaveriot.integrations.aiinference.api.utils.OkHttpUtil;
import com.milesight.beaveriot.integrations.aiinference.entity.AiInferenceConnectionPropertiesEntities;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.text.MessageFormat;
import java.util.Map;

/**
 * author: Luxb
 * create: 2025/6/5 8:49
 **/
@Slf4j
@Builder
@Data
public class AiInferenceClient {
    private Config config;

    public void init() {
        OkHttpUtil.setCommonHeaders(Map.of("X-Access-Token", config.getToken()));
    }

    public CamThinkModelListResponse getModels() {
        String url = config.getModelsUrl();
        String params = "page=1&page_size=100";
        url = url + "?" + params;
        ClientResponse clientResponse = OkHttpUtil.get(url);
        validateResponse(clientResponse);
        try {
            return JsonUtils.fromJSON(clientResponse.getData(), CamThinkModelListResponse.class);
        } catch (Exception e) {
            log.error("Error: ", e);
            return null;
        }
    }

    public CamThinkModelDetailResponse getModelDetail(String modelId) {
        String url = config.getModelDetailUrl();
        url = MessageFormat.format(url, modelId);
        ClientResponse clientResponse = OkHttpUtil.get(url);
        validateResponse(clientResponse);
        try {
            return JsonUtils.fromJSON(clientResponse.getData(), CamThinkModelDetailResponse.class);
        } catch (Exception e) {
            log.error("Error: ", e);
            return null;
        }
    }

    public CamThinkModelInferResponse modelInfer(String modelId, CamThinkModelInferRequest camThinkModelInferRequest) {
        String url = config.getModelInferUrl();
        url = MessageFormat.format(url, modelId);
        ClientResponse clientResponse = OkHttpUtil.post(url, JsonUtils.toJSON(camThinkModelInferRequest));
        try {
            return JsonUtils.fromJSON(clientResponse.getData(), CamThinkModelInferResponse.class);
        } catch (Exception e) {
            log.error("Error: ", e);
            return null;
        }
    }

    private void validateResponse(ClientResponse clientResponse) {
        try {
            if (!clientResponse.isSuccessful()) {
                throw buildServiceException(clientResponse.getCode());
            }
            if (clientResponse.getData() == null) {
                throw buildServiceException(ServerErrorCode.SERVER_DATA_NOT_FOUND);
            }
        } catch (ServiceException e) {
            AnnotatedEntityWrapper<AiInferenceConnectionPropertiesEntities> wrapper = new AnnotatedEntityWrapper<>();
            wrapper.saveValue(AiInferenceConnectionPropertiesEntities::getApiStatus, false).publishSync();
            throw e;
        }
    }

    private ServiceException buildServiceException(int code) {
        ServiceException exception;
        if (code == HttpStatus.BAD_REQUEST.value()) {
            exception = buildServiceException(ServerErrorCode.SERVER_NOT_REACHABLE);
        } else if (code == HttpStatus.UNAUTHORIZED.value()) {
            exception = buildServiceException(ServerErrorCode.SERVER_TOKEN_INVALID);
        } else if (code == HttpStatus.FORBIDDEN.value()) {
            exception = buildServiceException(ServerErrorCode.SERVER_TOKEN_ACCESS_DENIED);
        } else if (code == HttpStatus.NOT_FOUND.value()) {
            exception = buildServiceException(ServerErrorCode.SERVER_DATA_NOT_FOUND);
        } else if (code == HttpStatus.UNPROCESSABLE_ENTITY.value()) {
            exception = buildServiceException(ServerErrorCode.SERVER_INVALID_INPUT_DATA);
        } else if (code == HttpStatus.TOO_MANY_REQUESTS.value()) {
            exception = buildServiceException(ServerErrorCode.SERVER_RATE_LIMIT_EXCEEDED);
        } else if (code == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            exception = buildServiceException(ServerErrorCode.SERVER_INTERNAL_SERVER_ERROR);
        } else if (code == HttpStatus.SERVICE_UNAVAILABLE.value()) {
            exception = buildServiceException(ServerErrorCode.SERVER_MODEL_WORKER_BUSY);
        } else {
            exception = ServiceException.with(ServerErrorCode.SERVER_OTHER_ERROR.getErrorCode(), ServerErrorCode.SERVER_OTHER_ERROR.getErrorMessage() + code).build();
        }
        return exception;
    }

    private ServiceException buildServiceException(ServerErrorCode serverErrorCode) {
        return ServiceException.with(serverErrorCode.getErrorCode(), serverErrorCode.getErrorMessage()).build();
    }

    public boolean testConnection() {
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

    private boolean validBaseUrl(String url) {
        try {
            ClientResponse clientResponse = OkHttpUtil.get(url);
            return clientResponse != null && clientResponse.isSuccessful();
        } catch (Exception e) {
            return false;
        }
    }
}