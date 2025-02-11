package com.milesight.beaveriot.parser.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 解析器字符串编码
 */
@Getter
@RequiredArgsConstructor
public enum ParserStringEncoding {
    HEX,
    ASCII,
    UTF8,
    BASE64,
    ;

    @Override
    public String toString() {
        return name();
    }
}
