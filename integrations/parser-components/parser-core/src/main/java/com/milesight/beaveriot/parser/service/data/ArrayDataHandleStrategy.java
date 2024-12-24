package com.milesight.beaveriot.parser.service.data;


import com.milesight.beaveriot.parser.annotaion.DataStruct;
import com.milesight.beaveriot.parser.constants.CommonConstants;
import com.milesight.beaveriot.parser.enums.ParserDataType;
import com.milesight.beaveriot.parser.model.*;
import com.milesight.beaveriot.parser.service.DataHandleStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import static com.milesight.beaveriot.parser.service.data.BasicDataHandleStrategy.listToParsingParamMap;
import static com.milesight.beaveriot.parser.util.ParserStructUtil.*;
import static com.milesight.beaveriot.parser.util.ParserUtil.*;


/**
 * 数组数据处理策略
 */
@Slf4j
@Component
@DataStruct(ParserDataType.ARRAY)
public class ArrayDataHandleStrategy implements DataHandleStrategy {


    /**
     * 数组类型-解码处理解析器数据结构
     */
    @SuppressWarnings({"java:S2589"})
    @Override
    public void handleDecoders(List<ParsingParamSpec> list, ByteBuffer buffer, List<ParserDataSpec> parserDataSpecList, List<ParserDecodeResponse> unpackList) {
        // 临时集合
        List<ParsingParamSpec> interimList = new ArrayList<>();
        // 初始化结构体是否异常值
        AtomicBoolean isException = new AtomicBoolean(false);
        // 获取父节点
        ParserDataSpec parentParser = parserDataSpecList.get(0);
        // 设置父节点参数
        ParsingParamSpec parentNode = setParserNodeParam(interimList, parentParser);
        // 获取子节点集合
        List<ParserDataSpec> childList = getListChildNodes(parserDataSpecList);
        // 获取子节点的解析器ids
        String[] parentParserIds = childList.get(0).getId().split("\\.");
        String parentPath = parentParserIds[parentParserIds.length - 1];
        switch (parentParser.getElementDataType()) {
            case ARRAY:
                // 不支持数组嵌套数组 - 插件兼容
                log.error("Arrays are not supported nested arrays - plugin compatible, parserId:{}", parentNode.getParserId());
            case STRUCT:
                // 数组嵌套结构体
                // 取出最后一个元素
                ParserDataSpec endNodeByStruct = childList.get(childList.size() - 1);
                // 结构体个数 = 父节点长度 / 子节点长度
                int sizeByStruct = parentParser.getEndByte() / endNodeByStruct.getEndByte();
                // 遍历数组(只处理固定长度的数组)
                IntStream.range(0, sizeByStruct).forEach(i -> {
                    // 设置array里属性节点
                    ParsingParamSpec nodeStruct = setParserArrayParamNode(interimList, parentNode, parentPath, parentParser.getElementDataType(), i);
                    // 遍历子节点
                    childList.forEach(parserDataSpec -> {
                        if (!ParserDataType.STRUCT.equals(parserDataSpec.getDataType())) {
                            String[] parserIds = parserDataSpec.getId().split("\\.");
                            String nodePath = parserIds[parserIds.length - 1];
                            // 设置解析器参数
                            ParsingParamSpec parsingParamSpec = setParserArrayChildNode(interimList, nodeStruct, nodePath, parserDataSpec.getDataType());
                            parsingParamSpec.setArrayIndex(i);
                            // 解码数据处理-是否异常值
                            if (decodeDataHandle(buffer, parserDataSpec, unpackList, parsingParamSpec)) {
                                isException.set(true);
                            }
                        }
                    });
                });
                break;
            default:
                // 数组嵌套单个基本类型
                // 遍历数组(只处理固定长度的数组)
                ParserDataSpec parserDataSpec = childList.get(0);
                IntStream.range(0, parentParser.getEndByte() / parserDataSpec.getEndByte()).forEach(i -> {
                    //获取子节点的解析器ids
                    String[] parserIds = parserDataSpec.getId().split("\\.");
                    String nodePath = parserIds[parserIds.length - 1];
                    // 设置array里属性节点
                    ParsingParamSpec parsingParamSpec = setParserArrayParamNode(interimList, parentNode, nodePath, parserDataSpec.getDataType(), i);
                    // 解码数据处理-是否异常值
                    if (decodeDataHandle(buffer, parserDataSpec, unpackList, parsingParamSpec)) {
                        isException.set(true);
                    }
                });
                break;
        }
        // 拼包处理
        packageHandle(isException.get(), list, interimList, unpackList);
    }

    /**
     * 数组类型-编码处理解析器数据结构
     */
    @SuppressWarnings({"java:S3776", "java:S1192", "java:S1602"})
    @Override
    public void handleEncoders(List<ParsingParamSpec> parsingParamSpecList, DynamicByteBuffer dynamicByteBuffer, List<ParserDataSpec> parserDataSpecList, ParserEncodeResponse parserEncodeResponse) {
        // 获取父节点
        ParserDataSpec parentNode = parserDataSpecList.get(0);
        // 设置命令头
        setCommandHeader(parentNode, dynamicByteBuffer);
        // 获取解析器参数
        Map<String, ParsingParamSpec> parsingParamMap = listToParsingParamMap(parsingParamSpecList);
        // 获取子节点集合
        List<ParserDataSpec> childList = getListChildNodes(parserDataSpecList);
        // 取出最后一个元素
        ParserDataSpec endNode = childList.get(childList.size() - 1);
        // 结构体个数 = 父节点长度 / 子节点长度
        int structNum = parentNode.getEndByte() / endNode.getEndByte();
        // 遍历数组(只处理固定长度的数组)
        IntStream.range(0, structNum).forEach(i -> {
            // 遍历子节点
            childList.forEach(parserDataSpec -> {
                switch (parentNode.getElementDataType()) {
                    case ARRAY:
                        // 不支持数组嵌套数组 - 插件兼容
                        log.error("Arrays are not supported nested arrays - plugin compatible, parserId:{}", parentNode.getId());
                    case STRUCT:
                        // 数组嵌套结构体
                        if (!ParserDataType.STRUCT.equals(parserDataSpec.getDataType())) {
                            String[] parserIds = parserDataSpec.getId().split("\\.");
                            // 设置路径
                            String path = CommonConstants.getPath(CommonConstants.getArrayStructPath(parentNode.getId(), i), parserIds[parserIds.length - 1]);
                            ParsingParamSpec parsingParamSpec = parsingParamMap.get(path);
                            // 设置编码数据值
                            setEncodeDataValue(dynamicByteBuffer, parsingParamSpec, parserDataSpec);
                        }
                        break;
                    default:
                        // 数组嵌套单个基本类型
                        // 设置路径
                        String path = CommonConstants.getArrayStructPath(parentNode.getId(), i);
                        ParsingParamSpec parsingParamSpec = parsingParamMap.get(path);
                        // 设置编码数据值
                        setEncodeDataValue(dynamicByteBuffer, parsingParamSpec, parserDataSpec);
                        break;
                }
            });
        });
    }

}