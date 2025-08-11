package com.milesight.beaveriot.integrations.camthinkaiinference.api.client;

import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.context.integration.wrapper.AnnotatedEntityWrapper;
import com.milesight.beaveriot.integrations.camthinkaiinference.api.config.Config;
import com.milesight.beaveriot.integrations.camthinkaiinference.api.enums.CamThinkErrorCode;
import com.milesight.beaveriot.integrations.camthinkaiinference.api.enums.ServerErrorCode;
import com.milesight.beaveriot.integrations.camthinkaiinference.api.model.request.CamThinkModelInferRequest;
import com.milesight.beaveriot.integrations.camthinkaiinference.api.model.response.*;
import com.milesight.beaveriot.integrations.camthinkaiinference.api.utils.OkHttpUtil;
import com.milesight.beaveriot.integrations.camthinkaiinference.entity.CamThinkAiInferenceConnectionPropertiesEntities;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.Map;

/**
 * author: Luxb
 * create: 2025/6/5 8:49
 **/
@Slf4j
@Data
@Component
public class CamThinkAiInferenceClient {
    private Config config = Config.builder().build();

    public Map<String, String> getCommonHeaders() {
        return Map.of("X-Access-Token", config.getToken());
    }

    public CamThinkModelListResponse getModels() {
        String url = config.getModelsUrl();
        String params = "page=1&page_size=9999";
        url = url + "?" + params;
        ClientResponse clientResponse = OkHttpUtil.get(url, getCommonHeaders());
        validateResponse(clientResponse, Config.URL_MODELS, true);
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
        ClientResponse clientResponse = OkHttpUtil.get(url, getCommonHeaders());
        validateResponse(clientResponse, Config.URL_MODEL_DETAIL, false);
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
        ClientResponse clientResponse = OkHttpUtil.post(url, getCommonHeaders(), JsonUtils.toJSON(camThinkModelInferRequest));
        validateResponse(clientResponse, Config.URL_MODEL_INFER, false);
        try {
            return JsonUtils.fromJSON(clientResponse.getData(), CamThinkModelInferResponse.class);
        } catch (Exception e) {
            log.error("Error: ", e);
            return null;
        }
    }

    private void validateResponse(ClientResponse clientResponse, String uri, boolean isUpdateApiStatus) {
        try {
            if (!clientResponse.isSuccessful() || clientResponse.getData() == null) {
                throw buildServiceException(clientResponse, uri);
            }
        } catch (ServiceException e) {
            if (isUpdateApiStatus) {
                AnnotatedEntityWrapper<CamThinkAiInferenceConnectionPropertiesEntities> wrapper = new AnnotatedEntityWrapper<>();
                wrapper.saveValue(CamThinkAiInferenceConnectionPropertiesEntities::getApiStatus, false).publishSync();
            }
            throw e;
        }
    }

    private ServiceException buildServiceException(ClientResponse clientResponse, String uri) {
        int code = clientResponse.getCode();
        ServiceException exception;
        String detailMessage = "";
        CamThinkCommonResponse camThinkResponse = null;
        if (clientResponse.getData() == null) {
            return buildServiceException(ServerErrorCode.SERVER_DATA_NOT_FOUND, detailMessage);
        } else {
            try {
                camThinkResponse = JsonUtils.fromJSON(clientResponse.getData(), CamThinkCommonResponse.class);
                detailMessage = camThinkResponse.getMessage();
            } catch (Exception e) {
                detailMessage = e.getMessage();
            }
        }
        if (code == HttpStatus.BAD_REQUEST.value()) {
            exception = buildServiceException(ServerErrorCode.SERVER_NOT_REACHABLE, detailMessage);
        } else if (code == HttpStatus.UNAUTHORIZED.value()) {
            exception = buildServiceException(ServerErrorCode.SERVER_TOKEN_INVALID, detailMessage);
        } else if (code == HttpStatus.FORBIDDEN.value()) {
            exception = buildServiceException(ServerErrorCode.SERVER_TOKEN_ACCESS_DENIED, detailMessage);
        } else if (code == HttpStatus.NOT_FOUND.value()) {
            if (camThinkResponse != null && CamThinkErrorCode.TOKEN_NOT_FOUND.getValue().equals(camThinkResponse.getErrorCode())) {
                exception = buildServiceException(ServerErrorCode.SERVER_TOKEN_INVALID, detailMessage);
            } else if (Config.URL_MODELS.equals(uri)) {
                exception = buildServiceException(ServerErrorCode.SERVER_NOT_REACHABLE, detailMessage);
            } else {
                exception = buildServiceException(ServerErrorCode.SERVER_DATA_NOT_FOUND, detailMessage);
            }
        } else if (code == HttpStatus.UNPROCESSABLE_ENTITY.value()) {
            exception = buildServiceException(ServerErrorCode.SERVER_INVALID_INPUT_DATA, detailMessage);
        } else if (code == HttpStatus.TOO_MANY_REQUESTS.value()) {
            exception = buildServiceException(ServerErrorCode.SERVER_RATE_LIMIT_EXCEEDED, detailMessage);
        } else if (code == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            exception = buildServiceException(ServerErrorCode.SERVER_INTERNAL_SERVER_ERROR, detailMessage);
        } else if (code == HttpStatus.SERVICE_UNAVAILABLE.value()) {
            exception = buildServiceException(ServerErrorCode.SERVER_MODEL_WORKER_BUSY, detailMessage);
        } else {
            exception = buildServiceException(ServerErrorCode.SERVER_OTHER_ERROR, ServerErrorCode.SERVER_OTHER_ERROR.getErrorMessage() + code, detailMessage);
        }
        return exception;
    }

    private ServiceException buildServiceException(ServerErrorCode serverErrorCode, String detailMessage) {
        return buildServiceException(serverErrorCode, serverErrorCode.getErrorMessage(), detailMessage);
    }

    private ServiceException buildServiceException(ServerErrorCode serverErrorCode, String errorMessage, String detailMessage) {
        ServiceException.ServiceExceptionBuilder builder = ServiceException.with(serverErrorCode.getErrorCode(), errorMessage);
        if (!StringUtils.isEmpty(detailMessage)) {
            builder.detailMessage(detailMessage);
        }
        return builder.build();
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
            ClientResponse clientResponse = OkHttpUtil.get(url, getCommonHeaders());
            return clientResponse != null && clientResponse.isSuccessful();
        } catch (Exception e) {
            return false;
        }
    }
}