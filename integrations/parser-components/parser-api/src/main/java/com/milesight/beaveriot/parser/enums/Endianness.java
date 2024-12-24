package com.milesight.beaveriot.parser.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 解析器字节顺序
 */
@Getter
@RequiredArgsConstructor
public enum Endianness {
    LE,
    BE;

    @Override
    public String toString() {
        return name();
    }
}
