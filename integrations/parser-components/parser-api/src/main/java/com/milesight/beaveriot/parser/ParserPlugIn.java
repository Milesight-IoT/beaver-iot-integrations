package com.milesight.beaveriot.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.milesight.beaveriot.parser.model.LnsAwsPayload;
import com.milesight.beaveriot.parser.model.ParserPayload;
import com.milesight.beaveriot.parser.model.ProductDesc;
import com.milesight.cloud.sdk.client.model.ThingSpec;

import java.util.List;

/**
 * 解析器解码编码 接口
 *
 * @author linzy
 */
public interface ParserPlugIn {

    /**
     * 获取产品描述
     *
     * @param sn
     * @return ProductDesc
     */
    ProductDesc getProductBySn(String sn);

    /**
     * 获取设备物模型
     *
     * @param sn
     * @return ThingSpec
     */
    List<ThingSpec> getThingSpecBySn(String sn);

    /**
     * 解码
     *
     * @param parserPayload
     */
    ParserPayload decode(ParserPayload parserPayload);

    /**
     * 编码
     *
     * @param parserPayload
     */
    ParserPayload encode(ParserPayload parserPayload);

}

