package com.milesight.beaveriot.parser.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 解析器数据类型
 */
@Getter
@RequiredArgsConstructor
public enum ParserDataType {
    BOOL,
    STRING,
    STRUCT,
    ARRAY,
    INT8,
    INT16,
    INT32,
    INT64,
    UINT8,
    UINT16,
    UINT32,
    FLOAT,
    FLOAT16,
    DOUBLE,
    INT,
    LONG,
    COMMON;

    @Override
    public String toString() {
        return name();
    }
}
