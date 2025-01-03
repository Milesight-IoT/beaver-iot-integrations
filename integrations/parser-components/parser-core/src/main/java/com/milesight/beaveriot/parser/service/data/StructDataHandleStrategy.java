package com.milesight.beaveriot.parser.service.data;


import com.milesight.beaveriot.parser.annotaion.DataStruct;
import com.milesight.beaveriot.parser.enums.ParserDataType;
import com.milesight.beaveriot.parser.model.*;
import com.milesight.beaveriot.parser.service.DataHandleStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.milesight.beaveriot.parser.service.data.BasicDataHandleStrategy.listToParsingParamMap;
import static com.milesight.beaveriot.parser.util.ParserStructUtil.*;
import static com.milesight.beaveriot.parser.util.ParserUtil.*;


/**
 * 结构体数据处理策略
 */
@Slf4j
@Component
@DataStruct(ParserDataType.STRUCT)
public class StructDataHandleStrategy implements DataHandleStrategy {


    /**
     * 结构体类型-解码处理解析器数据结构
     */
    @Override
    public void handleDecoders(List<ParsingParamSpec> list, ByteBuffer buffer, List<ParserDataSpec> parserDataSpecList, List<ParserDecodeResponse> unpackList) {
        // 临时集合
        List<ParsingParamSpec> interimList = new ArrayList<>();
        // 结构体类型解码处理解析器
        boolean isException = setStructDataDecoders(interimList, buffer, parserDataSpecList, unpackList);
        // 拼包处理
        packageHandle(isException, list, interimList, unpackList);
    }

    /**
     * 结构体类型-编码处理解析器数据结构
     */
    @Override
    public void handleEncoders(List<ParsingParamSpec> parsingParamSpecList, DynamicByteBuffer dynamicByteBuffer, List<ParserDataSpec> parserDataSpecList, ParserEncodeResponse parserEncodeResponse) {
        // 获取父节点
        ParserDataSpec parentParser = parserDataSpecList.get(0);
        // 设置命令头
        setCommandHeader(parentParser, dynamicByteBuffer);
        // 获取子节点集合
        List<ParserDataSpec> childList = getListChildNodes(parserDataSpecList);
        // 获取解析器参数
        Map<String, ParsingParamSpec> parsingParamMap = listToParsingParamMap(parsingParamSpecList);
        // 根据字节起始位和结束位分组
        Map<String, List<ParserDataSpec>> childMap = groupingByByteStartEnd(childList);
        childMap.forEach((key, value) -> {
            if (value.size() > 1) {
                // 存在bit位
                // 根据StartBit排序
                value.sort(Comparator.comparing(ParserDataSpec::getStartBit));
                // 获取字节起始位和结束位
                int byteValue;
                List<Integer> values = new ArrayList<>();
                List<Integer> positions = new ArrayList<>();
                // 设置编码位置数据值
                value.forEach(parserDataSpec -> {
                    String parserId = parserDataSpec.getId();
                    ParsingParamSpec parsingParamSpec = parsingParamMap.get(parserId);
                    setEncodePositionsBitDataValue(values, positions, parsingParamSpec, parserDataSpec);
                });
                // 根据值和位置创建字节
                byteValue = createByteFromBits(values, positions);
                int byteLength = value.get(0).getEndByte() - value.get(0).getStartByte();
                // 设置编码数据值
                setEncodeBitDataValue(dynamicByteBuffer, byteValue, byteLength);
            } else {
                String parserId = value.get(0).getId();
                ParsingParamSpec parsingParamSpec = parsingParamMap.get(parserId);
                // 设置编码数据值
                setEncodeDataValue(dynamicByteBuffer, parsingParamSpec, value.get(0));
            }
        });

    }

}