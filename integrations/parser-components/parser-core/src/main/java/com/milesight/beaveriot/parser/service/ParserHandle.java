package com.milesight.beaveriot.parser.service;


import cn.hutool.core.collection.CollUtil;
import com.milesight.beaveriot.parser.cache.PluginCache;
import com.milesight.beaveriot.parser.enums.ParserDataType;
import com.milesight.beaveriot.parser.model.*;
import com.milesight.beaveriot.parser.plugin.PlugIn;
import com.milesight.beaveriot.parser.util.DataHandleStrategyContext;
import com.milesight.beaveriot.parser.util.DataHandleStrategyFactory;
import com.milesight.beaveriot.parser.util.PlugInFactory;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.*;

import static com.milesight.beaveriot.parser.constants.CommonConstants.PLUGIN_KEY;
import static com.milesight.beaveriot.parser.util.ParserUtil.*;

/**
 * @Description: 解析器处理
 */
@Slf4j
public class ParserHandle {


    /**
     * 解码处理
     *
     * @return 是否解析完成
     */
    public static void decoderData(ParserPayload parserPayload) {
        List<ParserDecodeResponse> parserDecodeResponseList = new ArrayList<>();
        List<ParsingParamSpec> list = new ArrayList<>();
        // 获取消息缓存字节数组
        ByteBuffer buffer = getDeviceByteBuffer(parserPayload.getIpsoData());
        while (buffer.hasRemaining()) {
            try {
                int channelId = buffer.get() & 0xFF;
                int channelType = buffer.get() & 0xFF;
                ParserPluginParam parserPluginParam = ParserPluginParam.builder()
                        .channelId(channelId)
                        .channelType(channelType)
                        .build();
                Map<String, PlugIn> plugInMap = getPlugInMap(parserPayload);
                val isPlugin = PluginHandle.decoderHandle(list, plugInMap, buffer, parserDecodeResponseList, parserPluginParam);
                if (Boolean.FALSE.equals(isPlugin)) {
                    parserDecodeHandle(list, parserPayload, parserDecodeResponseList, buffer, parserPluginParam);
                }
            } catch (Exception e) {
                log.error("Error while parsing data", e);
                break;
            }
        }
        if (CollUtil.isNotEmpty(list)) {
            parserDecodeResponseList.add(ParserDecodeResponse.builder()
                    .timestamp(System.currentTimeMillis())
                    .payload(list)
                    .build());
        }
        parserPayload.setParserDecodeResponseList(parserDecodeResponseList);
    }

    @NotNull
    private static Map<String, PlugIn> getPlugInMap(ParserPayload parserPayload) {
        val plugIns = PluginCache.getFromCache(PLUGIN_KEY);
        Map<String, PlugIn> plugInMap = new HashMap<>();
        PlugInFactory.setupPlugin(parserPayload.getSn(), plugInMap, plugIns);
        return plugInMap;
    }

    private static void parserDecodeHandle(List<ParsingParamSpec> list, ParserPayload parserPayload, List<ParserDecodeResponse> unpackList, ByteBuffer buffer, ParserPluginParam parserPluginParam) {
        List<ParserDataSpec> parserDataSpecList = findDecoder(parserPayload, parserPluginParam.getChannelId(), parserPluginParam.getChannelType());
        // 初始化解析器类型
        ParserDataType parserDataType = ParserDataType.COMMON;
        if (parserDataSpecList.size() > 1) {
            // 结构体排序-按照起始位排序
            structSort(parserDataSpecList);
            // 获取解析器类型
            parserDataType = parserDataSpecList.get(0).getDataType();
            switch (parserDataType) {
                case ARRAY:
                case STRUCT:
                    break;
                default:
                    // 特殊情况，结构体存在关联属性列
                    parserDataType = ParserDataType.STRUCT;
                    break;
            }
        }
        // 初始化策略
        DataHandleStrategy receiptHandleStrategy
                = DataHandleStrategyFactory.getReceiptHandleStrategy(parserDataType);
        DataHandleStrategyContext receiptStrategyContext = DataHandleStrategyContext.builder()
                .dataHandleStrategy(receiptHandleStrategy)
                .build();
        // 执行解码解析策略
        receiptStrategyContext.handleDecodersReceipt(list, buffer, parserDataSpecList, unpackList);
    }


    /**
     * 编码处理
     *
     * @return 是否解析完成
     */
    public static void encoderData(ParserPayload parserPayload) {
        DynamicByteBuffer dynamicByteBuffer = new DynamicByteBuffer();
        List<ParsingParamSpec> parsingParamSpecList = parserPayload.getParserDownLinkData();
        val stringListMap = groupingByParserIdPrefix(parsingParamSpecList);
        stringListMap.keySet().forEach(key -> {
            val list = stringListMap.get(key);
            try {
                ParserPluginParam parserPluginParam = ParserPluginParam.builder()
                        .parserId(key)
                        .build();
                Map<String, PlugIn> plugInMap = getPlugInMap(parserPayload);
                val isPlugin = PluginHandle.encoderHandle(plugInMap, parsingParamSpecList, dynamicByteBuffer, parserPluginParam);
                if (Boolean.FALSE.equals(isPlugin)) {
                    parserEncodeHandle(parserPayload, dynamicByteBuffer, key, list);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        val bytes = dynamicByteBuffer.toArray();
        parserPayload.setIpsoData(Base64.getEncoder().encodeToString(bytes));
    }

    private static void parserEncodeHandle(ParserPayload parserPayload, DynamicByteBuffer dynamicByteBuffer, String key, List<ParsingParamSpec> list) {
        List<ParserDataSpec> parserDataSpecList = findEncoder(parserPayload, key);
        // 初始化解析器类型
        ParserDataType parserDataType = ParserDataType.COMMON;
        if (parserDataSpecList.size() > 1) {
            // 结构体排序-按照起始位排序
            structSort(parserDataSpecList);
            // 获取解析器类型
            parserDataType = parserDataSpecList.get(0).getDataType();
            switch (parserDataType) {
                case ARRAY:
                case STRUCT:
                    break;
                default:
                    // 特殊情况，结构体存在关联属性列
                    parserDataType = ParserDataType.STRUCT;
                    break;
            }
        }
        // 初始化策略
        DataHandleStrategy receiptHandleStrategy
                = DataHandleStrategyFactory.getReceiptHandleStrategy(parserDataType);
        DataHandleStrategyContext receiptStrategyContext = DataHandleStrategyContext.builder()
                .dataHandleStrategy(receiptHandleStrategy)
                .build();
        // 执行编码解析策略
        receiptStrategyContext.handleEncodersReceipt(list, dynamicByteBuffer, parserDataSpecList, null);
    }
}