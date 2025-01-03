package com.milesight.beaveriot.parser.util;


import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.milesight.beaveriot.parser.constants.CommonConstants;
import com.milesight.beaveriot.parser.enums.ParserDataType;
import com.milesight.beaveriot.parser.enums.UpLinkPacketType;
import com.milesight.beaveriot.parser.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

import static com.milesight.beaveriot.parser.constants.CommonConstants.HISTORICAL_DATA;
import static com.milesight.beaveriot.parser.constants.CommonConstants.TIMESTAMP;
import static com.milesight.beaveriot.parser.util.ParserUtil.*;

/**
 * Float工具类
 */
@Slf4j
public class ParserStructUtil {

    /**
     * 结构体类型-解码处理解析器
     *
     * @param interimList        临时集合
     * @param buffer             ByteBuffer 缓冲区
     * @param parserDataSpecList 解析器数据结构集合
     * @param unpackList         解码结果集合
     * @return boolean
     */
    @SuppressWarnings({"java:S3776"})
    public static boolean setStructDataDecoders(List<ParsingParamSpec> interimList, ByteBuffer buffer, List<ParserDataSpec> parserDataSpecList, List<ParserDecodeResponse> unpackList) {
        // 是否异常值
        boolean isException;
        // 是否历史数据
        boolean isHistorical = false;
        // 获取父节点
        ParserDataSpec parentParser = parserDataSpecList.get(0);
        // 判断是否为历史数据
        if (parentParser != null && parentParser.getId() != null && parentParser.getId().contains(HISTORICAL_DATA)) {
            isHistorical = true;
            // 临时解码结果集合
            List<ParserDecodeResponse> interimUnpackList = new ArrayList<>();
            // 解码处理-历史数据
            isException = structDataHandle(interimList, buffer, parserDataSpecList, interimUnpackList, true);
            if (!interimUnpackList.isEmpty()) {
                // 添加历史数据到解码结果（不为空的情况，时间戳的日期不是1973）
                unpackList.addAll(interimUnpackList);
            }
        } else {
            // 解码处理-正常数据
            isException = structDataHandle(interimList, buffer, parserDataSpecList, unpackList, false);
        }
        // 历史数据拆分的优先级：异常值（时间戳异常值1973 & 属性异常值） > 历史数据拆分
        if (!isException && isHistorical) {
            // 历史数据拆分
            historicalDataSplitting(interimList, unpackList, parentParser.getId());
        }
        return isException;
    }

    /**
     * ParserParamSpec根据字节起始位和结束位分组
     */
    public static Map<String, List<ParserDataSpec>> groupingByByteStartEnd(List<ParserDataSpec> parserDataSpecList) {
        return parserDataSpecList.stream()
                .collect(Collectors.groupingBy(paramEntity -> extractFirstNode(paramEntity.getStartByte() + "|" + paramEntity.getEndByte()),
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    /**
     * 结构体类型-解码处理
     *
     * @param interimList        临时集合
     * @param buffer             ByteBuffer 缓冲区
     * @param parserDataSpecList 解析器数据结构集合
     * @param interimUnpackList  临时解码结果集合
     * @param isHistorical       是否历史数据
     * @return boolean
     */
    @SuppressWarnings({"java:S3776"})
    private static boolean structDataHandle(List<ParsingParamSpec> interimList, ByteBuffer buffer, List<ParserDataSpec> parserDataSpecList, List<ParserDecodeResponse> interimUnpackList, boolean isHistorical) {
        // 是否时间戳异常
        Long seconds = null;
        // 初始化结构体是否异常值
        boolean isException = false;
        // 获取父节点
        ParserDataSpec parentParser = parserDataSpecList.get(0);
        // 设置父节点参数
        setParserNodeParam(interimList, parentParser);
        // 获取子节点集合
        List<ParserDataSpec> childList = getListChildNodes(parserDataSpecList);
        // struct类型里属性划分->根据字节起始位和结束位分组
        Map<String, List<ParserDataSpec>> childMap = groupingByByteStartEnd(childList);
        // 遍历子节点集合
        for (List<ParserDataSpec> values : childMap.values()) {
            // 设置解析器
            ParserDataSpec parserDataSpec = values.get(0);
            // 判断是否存在bit位
            if (values.size() == 1) {
                // 设置节点
                ParsingParamSpec parsingParamSpec = setParserNode(parserDataSpec, interimList);
                // 特殊处理--关联项没有父节点
                if (parserDataSpec.getParentId() != null) {
                    // 设置父节点ID
                    parsingParamSpec.setParentId(parserDataSpec.getParentId());
                }
                // 解码数据处理-是否异常值
                if (decodeDataHandle(buffer, parserDataSpec, interimUnpackList, parsingParamSpec)) {
                    isException = true;
                }
                // 判断是否为 历史数据&时间戳
                if (isHistorical && parsingParamSpec.getParserId().contains(TIMESTAMP)) {
                    switch (parsingParamSpec.getDataType()) {
                        case LONG:
                            // 历史数据-时间戳-数值类型
                            seconds = parseNumberDate(parsingParamSpec.getLongValue());
                            break;
                        default:
                            log.error("Timestamp illegal types are not supported：不支持时间戳非法类型");
                    }
                }
            } else {
                // 存在bit位-暂不考虑异常值
                // 根据StartBit排序
                handleBit(buffer, interimList, parentParser, values, parserDataSpec);
            }
        }
        // 历史数据且是1973年之前的时间戳视为无效
        if (isHistorical && seconds != null && seconds <= 100000000) {
            // 异常值
            isException = true;
            // 清空临时解码结果集合
            interimUnpackList.clear();
            // 时间戳异常值日志输出
            log.warn("The timestamp is invalid: {}", seconds);
        }
        return isException;
    }
    /**
     * 获取子节点列表
     */
    public static List<ParserDataSpec> getListChildNodes(List<ParserDataSpec> parserDataSpecList) {
        // 获取子节点
        List<ParserDataSpec> sortedList = new ArrayList<>(parserDataSpecList);
        // 移除父节点
        sortedList.remove(0);
        // 根据StartByte,StartBit排序
        sortedList.sort(Comparator.comparing(ParserDataSpec::getStartByte));
        return sortedList;
    }



    /**
     * 历史数据拆分
     *
     * @param interimList 临时集合
     * @param unpackList  解码结果集合
     */
    private static void historicalDataSplitting(List<ParsingParamSpec> interimList, List<ParserDecodeResponse> unpackList, String parentParserId) {
        // 解码结果集合
        ParserDecodeResponse parserDecodeResponse = new ParserDecodeResponse();
        // 历史数据拆分集合
        List<ParsingParamSpec> historicalDataList = interimList.stream().map(item -> {
            ParsingParamSpec parsingParamSpec = new ParsingParamSpec();
            BeanUtils.copyProperties(item, parsingParamSpec);
            return parsingParamSpec;
        }).collect(Collectors.toList());
        // 获取时间戳路径
        String timestampPath = CommonConstants.getPath(parentParserId, TIMESTAMP);
        // 移除历史数据
        historicalDataList.removeIf(parsingParamSpec -> parentParserId.equals(parsingParamSpec.getParserId()));
        // 遍历历史数据
        historicalDataList.forEach(parsingParamSpec -> {
            String parserId = parsingParamSpec.getParserId();
            if (timestampPath.equals(parserId)) {
                long timestamp = 0;
                switch (parsingParamSpec.getDataType()) {
                    case LONG:
                        // 时间戳
                        timestamp = parsingParamSpec.getLongValue();
                        break;
                    case STRING:
                        String date = parsingParamSpec.getStringValue();
                        // 日期转时间戳
                        timestamp = DateUtil.parse(date, DatePattern.NORM_DATETIME_PATTERN).getTime();
                        break;
                    default:
                        log.error("Timestamp illegal types are not supported：不支持时间戳非法类型");
                }
                parserDecodeResponse.setTimestamp(timestamp * 1000);
            } else {
                String[] parserIds = parserId.split("\\.");
                String attribute = parserIds[parserIds.length - 1];
                parsingParamSpec.setParserId(attribute);
                parsingParamSpec.setPath(attribute);
                parsingParamSpec.setParentId(null);
                parsingParamSpec.setChannel(null);
            }
        });
        if (parserDecodeResponse.getTimestamp() == null || parserDecodeResponse.getTimestamp() == 0) {
            log.debug("Historical data splitting failed because the timestamp is empty");
        } else {
            // 移除时间戳
            historicalDataList.removeIf(parsingParamSpec -> timestampPath.equals(parsingParamSpec.getParserId()));
            // 设置历史数据拆分集合
            parserDecodeResponse.setPayload(historicalDataList);
            // 添加历史数据拆分集合
            unpackList.add(parserDecodeResponse);
            // 历史数据拆分日志输出
            log.info("Historical data splitting:{}", historicalDataList);
        }
    }

    /**
     * 解码数据处理
     *
     * @param buffer           缓冲区
     * @param parserDataSpec   解析器数据结构
     * @param unpackList       解析结果
     * @param parsingParamSpec 解析器参数
     */
    public static boolean decodeDataHandle(ByteBuffer buffer, ParserDataSpec parserDataSpec, List<ParserDecodeResponse> unpackList, ParsingParamSpec parsingParamSpec) {
        // 获取解码异常数据值
        String decodeDataExceptionValue = getDecodeExceptionValue(buffer, parserDataSpec);
        // 异常数据处理
        if (StrUtil.isNotEmpty(decodeDataExceptionValue)) {
            // 异常数据处理
            exceptionDataHandle(decodeDataExceptionValue, parsingParamSpec, parserDataSpec, unpackList);
            return true;
        } else {
            // 设置解码数据值
            setDecodeDataValue(buffer, parsingParamSpec, parserDataSpec);
        }
        return false;
    }

    /**
     * 异常数据值处理
     */
    public static void exceptionDataHandle(String decodeDataExceptionValue, ParsingParamSpec parsingParamSpec, ParserDataSpec parserDataSpec, List<ParserDecodeResponse> unpackList) {
        // 统一小写
        Map<String, String> lowerCaseMap = new HashMap<>();
        // 异常值转换成小写
        parserDataSpec.getExceptionValues().forEach((key, value) -> lowerCaseMap.put(key.toLowerCase(), value));
        // 初始化设备异常数据值
        DeviceExceptionValuePayload deviceExceptionValuePayload = DeviceExceptionValuePayload.builder()
                .parserId(parsingParamSpec.getParserId())
                .path(parsingParamSpec.getPath())
                .exceptionValue(decodeDataExceptionValue)
                .exceptionLabel(lowerCaseMap.get(decodeDataExceptionValue))
                .build();
        // 初始化解码异常数据值
        ParserDecodeResponse parserDecodeResponse = ParserDecodeResponse.builder()
                .upLinkPacketType(UpLinkPacketType.EXCEPTION_VALUE)
                .exceptionPayload(deviceExceptionValuePayload)
                .build();
        // 设置解码异常数据值
        unpackList.add(parserDecodeResponse);
    }

    /**
     * 解码-设置父节点参数
     */
    public static ParsingParamSpec setParserNodeParam(List<ParsingParamSpec> list, ParserDataSpec parentParser) {
        ParsingParamSpec node = ParsingParamSpec.builder()
                .parserId(parentParser.getId())
                .channel(parentParser.getChannel())
                .path(parentParser.getId())
                .dataType(parentParser.getDataType())
                .build();
        // 设置父节点
        list.add(node);
        return node;
    }

    /**
     * 设置array里属性节点
     *
     * @param list     解析器参数集合
     * @param parent   父节点
     * @param nodePath 子节点路径
     * @param index    索引
     * @return 解析器参数
     */
    public static ParsingParamSpec setParserArrayParamNode(List<ParsingParamSpec> list, ParsingParamSpec parent, String nodePath, ParserDataType parserDataType, int index) {
        // 设置节点
        ParsingParamSpec node = ParsingParamSpec.builder()
                .parserId(CommonConstants.getPath(parent.getParserId(), nodePath))
                .channel(parent.getChannel())
                .path(CommonConstants.getArrayStructPath(parent.getPath(), index))
                .dataType(parserDataType)
                .parentId(parent.getParserId())
                .arrayIndex(index)
                .build();
        list.add(node);
        return node;
    }

    /**
     * 设置子节点
     *
     * @param parent         父节点
     * @param nodePath       子节点路径
     * @param parserDataType 解析器数据类型
     * @return 解析器参数
     */
    public static ParsingParamSpec setParserArrayChildNode(List<ParsingParamSpec> list, ParsingParamSpec parent, String nodePath, ParserDataType parserDataType) {
        // 设置子节点
        ParsingParamSpec node = ParsingParamSpec.builder()
                .parserId(CommonConstants.getPath(parent.getParserId(), nodePath))
                .channel(parent.getChannel())
                .path(CommonConstants.getPath(parent.getPath(), nodePath))
                .dataType(parserDataType)
                .parentId(parent.getParserId())
                .build();
        list.add(node);
        return node;
    }

    private static void handleBit(ByteBuffer buffer, List<ParsingParamSpec> interimList, ParserDataSpec parentParser,
                                  List<ParserDataSpec> values, ParserDataSpec parserDataSpec) {
        // 存在bit位-暂不考虑异常值
        // 根据StartBit排序
        values.sort(Comparator.comparing(ParserDataSpec::getStartBit));
        // 获取字节长度
        int byteLength = parserDataSpec.getEndByte() - parserDataSpec.getStartByte();
        // 获取字节起始位和结束位
        int byteValue = 0;
        switch (byteLength) {
            case 1:
                byteValue = buffer.get() & 0xff;
                break;
            case 2:
                byteValue = buffer.getShort() & 0xffff;
                break;
            case 4:
                byteValue = buffer.getInt();
                break;
            default:
                break;
        }
        int finalByteValue = byteValue;
        for (ParserDataSpec parserDataSpecByte : values) {
            // 设置节点
            ParsingParamSpec parsingParamSpec = setParserNode(parserDataSpecByte, interimList);
            parsingParamSpec.setParentId(parentParser.getId());
            // 设置解码bit数据值---预留-decodeBitDataHandle
            setDecodeBitDataValue(finalByteValue, parsingParamSpec, parserDataSpecByte);
        }
    }

    /**
     * 设置节点
     *
     * @param parserDataSpec 解析器数据
     * @param list           解析器参数集合
     * @return 解析器参数
     */
    public static ParsingParamSpec setParserNode(ParserDataSpec parserDataSpec, List<ParsingParamSpec> list) {
        // 初始化节点
        ParsingParamSpec node = ParsingParamSpec.builder()
                .parserId(parserDataSpec.getId())
                .channel(parserDataSpec.getChannel())
                .path(parserDataSpec.getId())
                .dataType(parserDataSpec.getDataType())
                .build();
        // 设置解码数据值
        list.add(node);
        return node;
    }

    /**
     * 设置Bit解码数据值
     */
    public static void setDecodeBitDataValue(int finalByteValue, ParsingParamSpec parsingParamSpec, ParserDataSpec parserDataSpec) {
        int start = parserDataSpec.getStartBit() > parserDataSpec.getStartByte() * 8 ? parserDataSpec.getStartBit() - parserDataSpec.getStartByte() * 8 : parserDataSpec.getStartBit();
        int end = parserDataSpec.getEndBit() > parserDataSpec.getStartByte() * 8 ? parserDataSpec.getEndBit() - parserDataSpec.getStartByte() * 8 : parserDataSpec.getEndBit();
        // number数据类型
        Number value = parserDataSpec.getFixedValue() != null ? parserDataSpec.getFixedValue() : getBitsByLocation(finalByteValue, start, end);
        // 数据类型
        switch (parsingParamSpec.getDataType()) {
            case BOOL:
                parsingParamSpec.setBoolValue(value.intValue() == 1);
                break;
            case INT8:
            case UINT8:
            case INT16:
            case UINT16:
                parsingParamSpec.setDataType(ParserDataType.INT);
                parsingParamSpec.setIntValue(value.intValue());
                break;
            case INT32:
            case UINT32:
            case INT64:
                parsingParamSpec.setDataType(ParserDataType.LONG);
                parsingParamSpec.setIntValue(value.intValue());
                break;
            default:
                break;
        }
    }

    // 从字节取出起始位到结束位的bits
    public static int getBitsByLocation(int num, int start, int end) {
        BitSet bitset = BitSet.valueOf(new long[]{num & 0xffL});
        byte[] bytes = bitset.get(start, end).toByteArray();
        return bytes.length > 0 ? bytes[0] : 0;
    }

    /**
     * 设置编码Bit位置数据值
     */
    public static void setEncodePositionsBitDataValue(List<Integer> values, List<Integer> positions, ParsingParamSpec parsingParamSpec, ParserDataSpec parserDataSpec) {
        // 位置
        positions.add(parserDataSpec.getStartBit() > parserDataSpec.getStartByte() * 8 ? parserDataSpec.getStartBit() - parserDataSpec.getStartByte() * 8 : parserDataSpec.getStartBit());
        // 获取数据值
        if (parsingParamSpec == null) {
            // 成员属性补 0x00
            parsingParamSpec = ParsingParamSpec.builder()
                    .dataType(ParserDataType.INT)
                    .intValue(0)
                    .build();
        }
        // number数据类型
        Number value = parserDataSpec.getFixedValue() != null ? parserDataSpec.getFixedValue() : Objects.requireNonNull(parserEncodeTransformerNumber(parsingParamSpec));
        // 设置数据值
        values.add(value.intValue());
    }

    /**
     * 根据值和位置创建字节
     *
     * @param values    值
     * @param positions 位置
     * @return 字节
     */
    public static int createByteFromBits(List<Integer> values, List<Integer> positions) {
        int result = 0;
        for (int i = 0; i < values.size(); i++) {
            int value = values.get(i);
            int position = positions.get(i);
            // 将值左移至对应的位置，并通过按位或运算符将其设置到结果中
            result |= value << position;
        }
        return result;
    }

    /**
     * 设置编码Bit数据值
     */
    public static void setEncodeBitDataValue(DynamicByteBuffer dynamicByteBuffer, int byteValue, int byteLength) {
        ByteBuffer buffer;
        byte[] bytes = null;
        // 数据类型
        switch (byteLength) {
            case 1:
                // 获取指定大小的缓冲区-数值类型-小端处理
                buffer = getByteBufferByNumberSize(1, null);
                buffer.put((byte) byteValue);
                bytes = buffer.array();
                break;
            case 2:
                // 获取指定大小的缓冲区-数值类型-小端处理
                buffer = getByteBufferByNumberSize(2, null);
                buffer.putShort((short) byteValue);
                bytes = buffer.array();
                break;
            case 4:
                // 获取指定大小的缓冲区-数值类型-小端处理
                buffer = getByteBufferByNumberSize(4, null);
                buffer.putInt(byteValue);
                bytes = buffer.array();
                break;
            default:
                break;
        }
        assert bytes != null;
        dynamicByteBuffer.append(bytes);
    }

}
