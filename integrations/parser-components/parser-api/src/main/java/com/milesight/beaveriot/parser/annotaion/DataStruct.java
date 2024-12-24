package com.milesight.beaveriot.parser.annotaion;

import com.milesight.beaveriot.parser.enums.ParserDataType;

import java.lang.annotation.*;

/**
 * 数据结构
 *
 * @author lzy
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataStruct {

    /**
     * 解析器数据类型
     */
    ParserDataType value();
}
