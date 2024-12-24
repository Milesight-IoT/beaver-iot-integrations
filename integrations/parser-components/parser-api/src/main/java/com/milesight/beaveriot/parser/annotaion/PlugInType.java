package com.milesight.beaveriot.parser.annotaion;

import com.milesight.beaveriot.parser.enums.MatchType;

import java.lang.annotation.*;

/**
 * 插件类型
 *
 * @author lzy
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PlugInType {

    /**
     * 属性id（与物模型id或者IPSO标识id对应）
     */
    String id();

    /**
     * 通道
     */
    int channel();

    /**
     * 通道类型（不填默认 0）
     */
    int type() default 0;

    /**
     * 读写类型 （读:R、写:W、读写:R,W）
     */
    String[] accessModes() default {"R","W"};

    /**
     * sn标识["前缀-后缀"]
     */
    String[] snMark();

    /**
     * 匹配类型（默认为精确匹配）
     */
    MatchType matchType() default MatchType.EXACT;
}
