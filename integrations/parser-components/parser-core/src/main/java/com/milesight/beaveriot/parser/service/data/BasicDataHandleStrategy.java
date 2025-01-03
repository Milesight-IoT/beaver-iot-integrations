package com.milesight.beaveriot.parser.service.data;


import com.milesight.beaveriot.parser.annotaion.DataStruct;
import com.milesight.beaveriot.parser.enums.ParserDataType;
import com.milesight.beaveriot.parser.model.*;
import com.milesight.beaveriot.parser.service.DataHandleStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.milesight.beaveriot.parser.util.ParserUtil.*;

/**
 * 基础数据类型处理策略
 *
 * @author lzy
 */
@Slf4j
@Component
@DataStruct(value = ParserDataType.COMMON)
public class BasicDataHandleStrategy implements DataHandleStrategy {

    /**
     * 基础数据类型-解码处理解析器数据结构
     *
     * @param buffer             缓冲区
     * @param parserDataSpecList 解析器数据结构
     * @param parsingParamSpecList         解析结果
     */
    @Override
    public void handleDecoders(List<ParsingParamSpec> parsingParamSpecList, ByteBuffer buffer, List<ParserDataSpec> parserDataSpecList, List<ParserDecodeResponse> unpackList) {
        // 基础数据类型只有一个值
        parserDataSpecList.forEach(parserDataSpec -> {
            // 临时集合
            List<ParsingParamSpec> interimList = new ArrayList<>();
            ParsingParamSpec parsingParamSpec = ParsingParamSpec.builder()
                    .parserId(parserDataSpec.getId())
                    .path(parserDataSpec.getId())
                    .dataType(parserDataSpec.getDataType())
                    .build();
            interimList.add(parsingParamSpec);
            // 设置异常值
            parsingParamSpec.setExceptionValue(getDecodeExceptionValue(buffer, parserDataSpec));
            // 设置节点
            setDecodeDataValue(buffer, parsingParamSpec, parserDataSpec);
            // 拼包处理
            packageHandle(parsingParamSpec.getExceptionValue()!=null, parsingParamSpecList, interimList, unpackList);
        });
    }



    /**
     * 基础数据类型-编码处理解析器数据结构
     */
    @Override
    public void handleEncoders(List<ParsingParamSpec> parsingParamSpecList, DynamicByteBuffer dynamicByteBuffer, List<ParserDataSpec> parserDataSpecList, ParserEncodeResponse parserEncodeResponse) {
        // 获取解析器参数
        Map<String, ParsingParamSpec> parsingParamMap = listToParsingParamMap(parsingParamSpecList);
        parserDataSpecList.forEach(parserDataSpec -> {
            // 设置命令头
            setCommandHeader(parserDataSpec, dynamicByteBuffer);
            String parserId = parserDataSpec.getId();
            ParsingParamSpec parsingParamSpec = parsingParamMap.get(parserId);
            if (parserDataSpec == null && parserDataSpec.getFixedValue() == null) {
                // 没有合适的处理器，解析失败
                log.error("Parser not found parserId:{}", parserId);
                // 未找到解析器
                throw new RuntimeException("Parser not found parserId:" + parserId);
            }
            // 设置编码数据值
            setEncodeDataValue(dynamicByteBuffer, parsingParamSpec, parserDataSpec);
        });
    }


    /**
     * 列表到解析参数映射
     */
    public static Map<String, ParsingParamSpec> listToParsingParamMap(List<ParsingParamSpec> parsingParamSpecList) {
        return parsingParamSpecList.stream().collect(Collectors.toMap(ParsingParamSpec::getPath, parsingParam -> parsingParam));
    }
}