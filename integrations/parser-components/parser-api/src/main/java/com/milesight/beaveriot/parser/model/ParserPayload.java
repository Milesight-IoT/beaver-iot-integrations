package com.milesight.beaveriot.parser.model;


import com.fasterxml.jackson.databind.JsonNode;
import com.milesight.beaveriot.parser.enums.DataType;
import com.milesight.beaveriot.parser.enums.DeviceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


/**
 * 解析器请求参数
 *
 * @author linzy
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ParserPayload {

    /**
     * deviceId
     */
    private String deviceId;

    /**
     * sn
     */
    private String sn;

    /**
     * 产品描述
     */
    private ProductDesc productDesc;

    /**
     * 设备类型
     */
    private DeviceType deviceType;

    /**
     * 数据链路类型
     */
    private DataType dataType;

    /**
     * IPSO数据（base64位）（上行入参）
     */
    private String ipsoData;

    /**
     * JSON数据（下行入参）
     */
    private JsonNode jsonData;

    /**
     * 解析器数据拼包数据（上行拼包）
     */
    private List<ParserDecodeResponse> parserDecodeResponseList;

    /**
     * 下行解析器数据
     */
    private List<ParsingParamSpec> parserDownLinkData;

    /**
     * 异常值数据
     */
    private List<String> exceptions;

    /**
     * 物模型数据
     */
    private List<DeviceData> thingData;

}
