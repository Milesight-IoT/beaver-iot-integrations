package com.milesight.beaveriot.parser.service;

import com.milesight.beaveriot.parser.model.*;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * @Description: 数据处理策略接口
 */
public interface DataHandleStrategy {

    /**
     * 处理解码数据
     */
    void handleDecoders(List<ParsingParamSpec> list, ByteBuffer buffer, List<ParserDataSpec> parserDataSpecList, List<ParserDecodeResponse> unpackList);

    /**
     * 处理编码数据
     */

     void handleEncoders(List<ParsingParamSpec> parsingParamSpecList, DynamicByteBuffer dynamicByteBuffer, List<ParserDataSpec> parserDataSpecList, ParserEncodeResponse parserEncodeResponse);
}