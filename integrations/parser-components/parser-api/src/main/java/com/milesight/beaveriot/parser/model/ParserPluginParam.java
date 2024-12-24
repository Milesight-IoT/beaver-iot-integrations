package com.milesight.beaveriot.parser.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 解析器通道信息- 实体
 *
 * @author linzy
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ParserPluginParam implements Serializable {
    /**
     * 通道
     */
    private Integer channelId;

    /**
     * 通道类型
     */
    private Integer channelType;
    /**
     * 物模型ID
     */
    private String parserId;
}
