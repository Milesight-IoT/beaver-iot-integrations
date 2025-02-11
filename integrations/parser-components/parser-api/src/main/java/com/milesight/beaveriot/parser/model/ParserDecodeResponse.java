package com.milesight.beaveriot.parser.model;

import com.milesight.beaveriot.parser.enums.UpLinkPacketType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 解析器解码输出数据
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ParserDecodeResponse {

    /**
     * 数据包类型
     */
    private UpLinkPacketType upLinkPacketType;

    /**
     * 历史时间戳
     */
    private Long timestamp;

    /**
     * 正常数据data(解析后给物模型的数据)
     */
    private List<ParsingParamSpec> payload;

    /**
     * 设备异常状态payload
     */
    private DeviceExceptionValuePayload exceptionPayload;

}