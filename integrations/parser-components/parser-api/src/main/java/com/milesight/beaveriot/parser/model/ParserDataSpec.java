package com.milesight.beaveriot.parser.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.milesight.beaveriot.parser.enums.Endianness;
import com.milesight.beaveriot.parser.enums.ParserDataType;
import com.milesight.beaveriot.parser.enums.ParserStringEncoding;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * 解析定义数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParserDataSpec implements Serializable {

    /**
     * ID
     */
    private String id;

    /**
     * 通道
     */
    private Integer channel;

    /**
     * 通道类型
     */
    private Integer type;

    /**
     * 开始字节
     */
    private Integer startByte;

    /**
     * 结束字节
     */
    private Integer endByte;

    /**
     * 开始比特位
     */
    private Integer startBit;

    /**
     * 结束比特位
     */
    private Integer endBit;

    /**
     * 字符串编码: ascii, utf-8, hex, base64
     */
    private ParserStringEncoding encoding;

    /**
     * 数据类型
     */
    private ParserDataType dataType;

    /**
     * 字节顺序：LE, BE
     */
    private Endianness endianness;

    /**
     * 父节点
     */
    private String parentId;

    /**
     * 固定值(编码时使用)
     */
    private Long fixedValue;

    /**
     * 数组内元素类型(array类型时使用)
     */
    private ParserDataType elementDataType;

    /**
     * exceptionValues 异常信息枚举映射
     */
    private Map<String, String> exceptionValues;

    /**
     * 响应通道 (默认0xFE)
     */
    private Integer responseChannel;

    /**
     * 解析辅助字段, 通常用于动态解析场景
     */
    private JsonNode assist;

    /**
     * 解析顺序
     */
    @Builder.Default
    private int order = -1;

    /**
     * (预留设计) 别名
     */
    private String alias;


    /**
     * 当前解析层级（内部业务逻辑使用）
     */
    private int currentLevel;

    /**
     * 当前解析层级链路（内部业务逻辑使用）
     */
    private int[] currentLevelLink;

    /**
     * 当前路径（内部业务逻辑使用）
     */
    private String currentPath;

}
