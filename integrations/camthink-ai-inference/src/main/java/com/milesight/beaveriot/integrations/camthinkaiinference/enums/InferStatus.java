package com.milesight.beaveriot.integrations.camthinkaiinference.enums;

import lombok.Getter;

/**
 * author: Luxb
 * create: 2025/6/21 15:38
 **/
@Getter
public enum InferStatus {
    OK("Ok"),
    FAILED("Failed"),
    UNKNOWN("");

    private final String value;

    InferStatus(String value) {
        this.value = value;
    }
}
