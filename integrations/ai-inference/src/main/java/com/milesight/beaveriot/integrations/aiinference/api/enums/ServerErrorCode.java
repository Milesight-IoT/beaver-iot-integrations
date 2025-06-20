package com.milesight.beaveriot.integrations.aiinference.api.enums;

import com.milesight.beaveriot.base.exception.ErrorCodeSpec;
import org.springframework.http.HttpStatus;

/**
 * author: Luxb
 * create: 2025/6/5 17:34
 **/
public enum ServerErrorCode implements ErrorCodeSpec {
    SERVER_NOT_REACHABLE(HttpStatus.BAD_REQUEST.value(), "server_not_reachable", "Server not reachable"),
    SERVER_TOKEN_INVALID(HttpStatus.UNAUTHORIZED.value(), "server_token_invalid", "Server token invalid"),
    SERVER_TOKEN_ACCESS_DENIED(HttpStatus.FORBIDDEN.value(), "server_token_access_denied", "Server token access denied"),
    SERVER_DATA_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "server_data_not_found", "Server data not found"),
    SERVER_INVALID_INPUT_DATA(HttpStatus.UNPROCESSABLE_ENTITY.value(), "server_invalid_input_data", "Server invalid input data"),
    SERVER_RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS.value(), "server_rate_limit_exceeded", "Server rate limit exceeded"),
    SERVER_INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR.value(), "server_internal_server_error", "Server internal server error"),
    SERVER_MODEL_WORKER_BUSY(HttpStatus.SERVICE_UNAVAILABLE.value(), "server_model_worker_busy", "Server model worker busy"),
    SERVER_OTHER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR.value(), "server_other_error", "Server error code:"),
    DEVICE_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "device_not_found", "Device not found"),
    DEVICE_BIND_ERROR(HttpStatus.BAD_REQUEST.value(), "device_bind_error", "Device bind error"),
    ;

    private final String errorCode;
    private String errorMessage;
    private String detailMessage;
    private int status = HttpStatus.INTERNAL_SERVER_ERROR.value();

    ServerErrorCode(int status, String errorCode, String errorMessage, String detailMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.status = status;
        this.detailMessage = detailMessage;
    }

    ServerErrorCode(int status, String errorCode) {
        this.errorCode = errorCode;
        this.status = status;
    }

    ServerErrorCode(int status, String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.status = status;
    }

    ServerErrorCode(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    ServerErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String getDetailMessage() {
        return detailMessage;
    }

    @Override
    public int getStatus() {
        return status;
    }
}
