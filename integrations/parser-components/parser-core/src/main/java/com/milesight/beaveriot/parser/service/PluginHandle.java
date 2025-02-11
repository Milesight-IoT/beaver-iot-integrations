package com.milesight.beaveriot.parser.service;


import com.milesight.beaveriot.parser.model.*;
import com.milesight.beaveriot.parser.plugin.PlugIn;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Description: 插件解析器处理
 */
public class PluginHandle  {

    /**
     * 解码查询
     */
    protected static PlugIn findDecoder(Map<String, PlugIn> plugInMap, int channelId, int channelType) {
        return plugInMap.get((channelId & 0xFF) + "|" + (channelType & 0xFF));
    }

    /**
     * 解码处理
     */
    public static boolean decoderHandle(List<ParsingParamSpec> list, Map<String, PlugIn> plugInMap, ByteBuffer buffer, List<ParserDecodeResponse> unpackList,
                                        ParserPluginParam parserPluginParam) {
        Integer channelId = parserPluginParam.getChannelId();
        int channelType = parserPluginParam.getChannelType() == null ? 0 : parserPluginParam.getChannelType();
        PlugIn plugIn = findDecoder(plugInMap, channelId, channelType);
        if (plugIn == null) {
            return false;
        }
        // 解码处理
        plugIn.handleDecoders(list, buffer, unpackList);
        return true;
    }

    /**
     * 编码查询
     */
    protected static PlugIn findEncoder(Map<String, PlugIn> plugInMap, String parserId) {
        return plugInMap.get(parserId);
    }

    /**
     * 编码处理
     */
    public static boolean encoderHandle(Map<String, PlugIn> plugInMap, List<ParsingParamSpec> list, DynamicByteBuffer dynamicByteBuffer,
                                    ParserPluginParam parserPluginParam) {
        PlugIn plugIn = findEncoder(plugInMap, parserPluginParam.getParserId());
        if (plugIn == null) {
            return false;
        }
        // 编码处理
        plugIn.handleEncoders(list, dynamicByteBuffer, null);
        return true;
    }
}