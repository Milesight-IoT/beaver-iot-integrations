package com.milesight.beaveriot.parser.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 匹配类型
 */
@Getter
@RequiredArgsConstructor
public enum MatchType {

    // 精确匹配，必须完全相等
    EXACT,

    // 前缀匹配，匹配开头部分
    PREFIX,

    // 后缀匹配，匹配结尾部分
    SUFFIX,

    // 部分匹配，匹配包含的任何位置
    CONTAINS;

    @Override
    public String toString() {
        return name();
    }
}