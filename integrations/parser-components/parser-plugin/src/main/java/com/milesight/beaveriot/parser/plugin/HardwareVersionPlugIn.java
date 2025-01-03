package com.milesight.beaveriot.parser.plugin;


import com.milesight.beaveriot.parser.annotaion.PlugInType;
import com.milesight.beaveriot.parser.enums.ParserDataType;
import com.milesight.beaveriot.parser.model.DynamicByteBuffer;
import com.milesight.beaveriot.parser.model.ParserDecodeResponse;
import com.milesight.beaveriot.parser.model.ParserEncodeResponse;
import com.milesight.beaveriot.parser.model.ParsingParamSpec;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static com.milesight.beaveriot.parser.util.PlugInUtil.*;

/**
 * @Description: 硬件版本插件
 */
@PlugInType(id = "hardware_version", channel = 0xff, type = 0x09, snMark = {})
@Slf4j
public class HardwareVersionPlugIn implements PlugIn {

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

    /**
     * 读取版本号，硬件版本号
     * （备注：硬件版本目前1111指的其实是11.1.1，不过最后一位到目前为止还没用到过，可以理解为舍弃了~）
     */
    public static String readVersion(byte[] bytes) {
        var major = bytes[0] & 0xff;
        var minor = (bytes[1] & 0xff) >> 4;
        return "v" + major + "." + minor;
    }

    @Override
    public void handleEncoders(List<ParsingParamSpec> list, DynamicByteBuffer dynamicByteBuffer, ParserEncodeResponse parserEncodeResponse) {
        list.forEach(parsingParamSpec -> {
            setCommandHeader(this, dynamicByteBuffer);
            String[] split = getVersionSplit(parsingParamSpec.getStringValue());

            ByteBuffer buffer = ByteBuffer.allocate(split.length);
            buffer.put((byte) (Integer.parseInt(split[0], 16)));
            buffer.put((byte) (Integer.parseInt(split[1] + "0", 16)));
            dynamicByteBuffer.append(buffer.array());
        });
    }
}