package com.milesight.beaveriot.parser.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.milesight.beaveriot.parser.enums.ParserDataType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 解析参数定义
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class ParsingParamSpec {

    /**
     * 解析器id
     */
    private String parserId;

    /**
     * 路径
     */
    private String path;

    /**
     * 通道
     */
    private Integer channel;

    /**
     * 数据类型
     */
    private ParserDataType dataType;

    /**
     * 父级id
     */
    private String parentId;

    /**
     * 包序号（预留）
     */
    private Integer seq;

    /**
     * 数组索引
     */
    private Integer arrayIndex;

    /**
     * int类型值
     */
    private Integer intValue;

    /**
     * long类型值
     */
    private Long longValue;

    /**
     * float类型值
     */
    private Float floatValue;

    /**
     * double类型值
     */
    private Double doubleValue;

    /**
     * string类型值
     */
    private String stringValue;

    /**
     * bool类型值
     */
    private Boolean boolValue;

    /**
     * 异常值
     */
    private String exceptionValue;

    /**
     * 任务id（用于标识是哪个任务下发的，如果是设备主动上报的，可以不填写）
     */
    private String taskId;

    /**
     * 任务触发时间阈值（单位：秒，用于标识任务下发后，设备在多少秒内没有上报数据，就触发任务）
     */
    private Long taskTriggerTime;
}
