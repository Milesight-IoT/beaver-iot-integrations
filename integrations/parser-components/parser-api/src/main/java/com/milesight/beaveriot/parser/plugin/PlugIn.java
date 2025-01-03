package com.milesight.beaveriot.parser.plugin;

import com.milesight.beaveriot.parser.model.DynamicByteBuffer;
import com.milesight.beaveriot.parser.model.ParserDecodeResponse;
import com.milesight.beaveriot.parser.model.ParserEncodeResponse;
import com.milesight.beaveriot.parser.model.ParsingParamSpec;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * @Description: 插件接口
 */
public interface PlugIn {


    // 处理解码数据
    void handleDecoders(List<ParsingParamSpec> list, ByteBuffer buffer, List<ParserDecodeResponse> unpackList);


    // 处理编码数据
    void handleEncoders(List<ParsingParamSpec> list, DynamicByteBuffer dynamicByteBuffer, ParserEncodeResponse parserEncodeResponse);
}