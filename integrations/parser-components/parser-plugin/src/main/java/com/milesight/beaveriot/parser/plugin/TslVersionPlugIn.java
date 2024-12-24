package com.milesight.beaveriot.parser.plugin;


import com.milesight.beaveriot.parser.annotaion.PlugInType;
import com.milesight.beaveriot.parser.enums.ParserDataType;
import com.milesight.beaveriot.parser.model.DynamicByteBuffer;
import com.milesight.beaveriot.parser.model.ParserDecodeResponse;
import com.milesight.beaveriot.parser.model.ParserEncodeResponse;
import com.milesight.beaveriot.parser.model.ParsingParamSpec;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static com.milesight.beaveriot.parser.util.PlugInUtil.*;

/**
 * @Description: 物模型版本插件
 */
@PlugInType(id = "tsl_version", channel = 0xff, type = 0xff, snMark = {})
public class TslVersionPlugIn implements PlugIn {

    @Override
    public void handleDecoders(List<ParsingParamSpec> list, ByteBuffer buffer, List<ParserDecodeResponse> unpackList) {
        // 临时集合
        List<ParsingParamSpec> interimList = new ArrayList<>();
        // 设置节点参数
        ParsingParamSpec parsingParamSpec = setPlugInRootNode(this, interimList, ParserDataType.STRING);
        byte[] bytes = new byte[2];
        buffer.get(bytes);
        // 设置解码数据值
        parsingParamSpec.setStringValue(readVersion(bytes));
        // 拼包处理
        packageHandle(false, list, interimList, unpackList);
    }

    @Override
    public void handleEncoders(List<ParsingParamSpec> list, DynamicByteBuffer dynamicByteBuffer, ParserEncodeResponse parserEncodeResponse) {
        list.forEach(parsingParamSpec -> {
            setCommandHeader(this, dynamicByteBuffer);
            currentVersionEncode(dynamicByteBuffer, parsingParamSpec.getStringValue(), 16);
        });
    }
}