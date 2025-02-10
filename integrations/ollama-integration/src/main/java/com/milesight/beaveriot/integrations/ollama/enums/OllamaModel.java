package com.milesight.beaveriot.integrations.ollama.enums;

import com.milesight.beaveriot.base.enums.EnumCode;

/**
 * @Author yuanh
 * @Description
 * @Package com.milesight.beaveriot.integrations.ollama.enums
 * @Date 2025/2/8 14:36
 */
public enum OllamaModel implements EnumCode {
    ;
    private String code;
    private String value;

    OllamaModel(String code, String value) {
        this.code = code;
        this.value = value;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getValue() {
        return value;
    }
}
