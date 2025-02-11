package com.milesight.beaveriot.parser.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备异常值payload
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class DeviceExceptionValuePayload {

    /**
     * sn码
     */
    private String sn;

    /**
     * 上报时间戳(毫秒)
     */
    private Long timestamp;

    /**
     * 版本
     */
    private String version;

    /**
     * 解析器id
     */
    private String parserId;

    /**
     * 错误值:设备上报的异常值value
     */
    private String exceptionValue;

    /**
     * 错误消息:设备上报的异常值label
     */
    private String exceptionLabel;

    /**
     * 路径
     */
    private String path;
}
