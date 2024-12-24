package com.milesight.beaveriot.weather.enums;

import com.milesight.beaveriot.base.enums.EnumCode;

/**
 * @Author yuanh
 * @Description
 * @Package com.milesight.beaveriot.weather.enums
 * @Date 2024/11/28 10:02
 */
public enum LangEnum implements EnumCode {
    ZH("zh","简体中文"),
    EN("en","英文"),
    JA("ja","日语"),
    KO("ko","韩语"),
    FR("fr","法语"),
    DE("de","德语"),
    ;

    private String code;
    private String value;

    LangEnum(String code, String value) {
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
