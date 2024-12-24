package com.milesight.beaveriot.parser.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 解析器物模型，版本 - 实体
 *
 * @author luo.hh
 * @version 1.0
 * @date 2024/07/16
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ParserEncodeParam implements Serializable {
    /**
     * 物模型ID
     */
    private String parserId;
}
