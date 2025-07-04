package com.milesight.beaveriot.integrations.camthinkaiinference.api.enums;

import lombok.Getter;

/**
 * author: Luxb
 * create: 2025/7/4 11:16
 **/
@Getter
public enum CamThinkErrorCode {
    TOKEN_NOT_FOUND("TOKEN_NOT_FOUND");
    private final String value;

    CamThinkErrorCode(String value) {
        this.value = value;
    }
}
