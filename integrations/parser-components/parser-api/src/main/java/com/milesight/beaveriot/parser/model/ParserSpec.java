package com.milesight.beaveriot.parser.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 解析定义数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParserSpec implements Serializable {

    /**
     * type
     */
    private String type;

    /**
     * 物模型相关支持
     */
    private List<JsonNode> thingSpecificationSupports;

    /**
     * 解码定义
     */
    private ParserDefinition decoder;

    /**
     * 编码定义
     */
    private ParserDefinition encoder;


}
