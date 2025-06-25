package com.milesight.beaveriot.integrations.mqttdevice.enums;

import com.milesight.beaveriot.base.exception.ErrorCodeSpec;
import org.springframework.http.HttpStatus;

/**
 * author: Luxb
 * create: 2025/6/5 17:34
 **/
public enum ServerErrorCode implements ErrorCodeSpec {
    INTEGRATION_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "integration_not_found", "integration not found"),
    TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "template_not_found", "template not found"),
    TOPIC_EXISTS(HttpStatus.BAD_REQUEST.value(), "topic_exists", "topic exists")
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
