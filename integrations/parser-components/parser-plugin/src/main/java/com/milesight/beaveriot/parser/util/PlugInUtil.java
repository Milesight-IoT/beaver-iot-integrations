package com.milesight.beaveriot.parser.util;


import cn.hutool.core.text.CharSequenceUtil;
import com.milesight.beaveriot.parser.annotaion.PlugInType;
import com.milesight.beaveriot.parser.constants.CommonConstants;
import com.milesight.beaveriot.parser.enums.ParserDataType;
import com.milesight.beaveriot.parser.enums.ParserStringEncoding;
import com.milesight.beaveriot.parser.model.DynamicByteBuffer;
import com.milesight.beaveriot.parser.model.ParserDecodeResponse;
import com.milesight.beaveriot.parser.model.ParsingParamSpec;
import com.milesight.beaveriot.parser.plugin.PlugIn;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.springframework.util.StringUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * 插件工具类
 */
@Slf4j
public class PlugInUtil {

    // 自增序号失效时间，单位：秒 设置为一周
    private static final long SERIAL_NUMBER_EXPIRE_TIME = 7 * 24 * 60 * 60L;

    private PlugInUtil() {
    }

    /**
     * 设置解码正常数据值
     */
    public static void setDecodeDataValue(ByteBuffer buffer, ParsingParamSpec parsingParamSpec, ParserDataType dataType) {
        // 数据类型
        switch (dataType) {
            case BOOL:
                parsingParamSpec.setBoolValue(buffer.get() != 0);
                break;
            case INT8:
                parsingParamSpec.setDataType(ParserDataType.INT);
                parsingParamSpec.setIntValue((int) buffer.get());
                break;
            case UINT8:
                parsingParamSpec.setDataType(ParserDataType.INT);
                parsingParamSpec.setIntValue(buffer.get() & 0xff);
                break;
            case INT16:
                parsingParamSpec.setDataType(ParserDataType.INT);
                // 数字类型字节反转
                parsingParamSpec.setIntValue((int) buffer.getShort());
                break;
            case UINT16:
                parsingParamSpec.setDataType(ParserDataType.INT);
                // 数字类型字节反转
                parsingParamSpec.setIntValue(buffer.getShort() & 0xffff);
                break;
            case INT32:
                parsingParamSpec.setDataType(ParserDataType.LONG);
                // 数字类型字节反转
                parsingParamSpec.setLongValue((long) buffer.getInt());
                break;
            case UINT32:
                parsingParamSpec.setDataType(ParserDataType.LONG);
                // 数字类型字节反转
                parsingParamSpec.setLongValue(buffer.getInt() & 0xffffffffL);
                break;
            case INT64:
                parsingParamSpec.setDataType(ParserDataType.LONG);
                // 数字类型字节反转
                parsingParamSpec.setLongValue(buffer.getLong());
                break;
            case FLOAT:
                // 数字类型字节反转
                parsingParamSpec.setFloatValue(buffer.getFloat());
                break;
            case DOUBLE:
                // 数字类型字节反转
                parsingParamSpec.setDoubleValue(buffer.getDouble());
                break;
//            case STRING:
//                // 获取字节数组
//                byte[] bytes = getByteArray(buffer, parserDataSpec);
//                if (parserDataSpec.getEndianness() != null && parserDataSpec.getEndianness().equals(Endianness.LE)) {
//                    // 小端模式-反转字节数组
//                    bytes = PrimitiveArrayUtil.reverse(bytes);
//                }
//                // 解码处理字符串编码类型
//                String str = handleDecodeStringEncoding(bytes, parserDataSpec.getEncoding());
//                if (CharSequenceUtil.isNotEmpty(str)) {
//                    // 去除空格
//                    str = str.trim();
//                }
//                parsingParamSpec.setStringValue(str);
//                break;
            default:
                break;
        }
    }

    /**
     * 设置解码正常数据值
     */
    public static void setDecodeStringValue(ByteBuffer buffer, ParsingParamSpec parsingParamSpec, int length, ParserStringEncoding encoding) {
        // 获取字节数组
        byte[] bytes = getByteArray(buffer, length);
        // 解码处理字符串编码类型
        String str = handleDecodeStringEncoding(bytes, encoding);
        if (CharSequenceUtil.isNotEmpty(str)) {
            // 去除空格
            str = str.trim();
        }
        parsingParamSpec.setStringValue(str);
    }

    /**
     * 从字节取出起始位到结束位的bits
     *
     * @param value 待处理值
     * @param start 开始位置
     * @param end   结束位置
     * @return int
     */
    public static int getBitsByLocation(int value, int start, int end) {
        BitSet bitset = BitSet.valueOf(new long[]{value});
        byte[] bytes = bitset.get(start, end).toByteArray();
        return bytes.length > 0 ? bytes[0] : 0;
    }

    /**
     * 从指定整数中提取从 startBit 到 endBit 范围内的位，并返回新的整数。
     *
     * @param number   要操作的整数
     * @param startBit 起始位（从 0 开始）
     * @param endBit   结束位（从 0 开始）
     * @return 提取的位组成的新整数
     */
    public static int extractBits(int number, int startBit, int endBit) {
        // 计算位数
        int numBits = endBit - startBit + 1;
        // 创建掩码
        int mask = (1 << numBits) - 1;
        // 右移操作并应用掩码
        return (number >> startBit) & mask;
    }

    /**
     * 获取字节数组
     *
     * @param buffer
     * @param length
     * @return
     */
    private static byte[] getByteArray(ByteBuffer buffer, int length) {
        // 初始化字节数组
        byte[] bytes = new byte[length];
        // 读取数据
        buffer.get(bytes);
        return bytes;
    }

    /**
     * 解码处理字符串编码类型
     */
    public static String handleDecodeStringEncoding(byte[] bytes, ParserStringEncoding encoding) {
        switch (encoding) {
            case HEX:
                return Hex.encodeHexString(bytes);
            case UTF8:
                return new String(bytes, StandardCharsets.UTF_8);
            case BASE64:
                return Base64.getEncoder().encodeToString(bytes);
            case ASCII:
                return new String(bytes, StandardCharsets.US_ASCII);
            default:
                return Arrays.toString(bytes);
        }
    }

    /**
     * 拼包处理
     *
     * @param isException 是否有异常值
     * @param list        解析器参数集合
     * @param interimList 临时解析器参数集合
     * @param unpackList  解包结果集合
     */
    public static void packageHandle(boolean isException, List<ParsingParamSpec> list, List<ParsingParamSpec> interimList, List<ParserDecodeResponse> unpackList) {
        // 判断有没有异常值
        if (!isException) {
            // 判断是否有相同path的解析器
            boolean hasSamePath = list.stream()
                    .filter(item -> !ParserDataType.ARRAY.equals(item.getDataType()))
                    .anyMatch(item -> interimList.stream().anyMatch(interimItem -> Objects.equals(interimItem.getPath(), item.getPath())));
            if (hasSamePath) {
                // 拷贝list到新数组
                List<ParsingParamSpec> newList = new ArrayList<>(list);
                // 上行数据拆包逻辑
                unpackList.add(ParserDecodeResponse.builder().payload(newList.stream().distinct().collect(Collectors.toList())).build());
                list.clear();
            }
            // 添加到解码结果集合
            list.addAll(interimList);
        }
        // 清空临时集合
        interimList.clear();
    }

    /**
     * 设置插件根节点
     *
     * @param plugInClass    插件类
     * @param list           解析器参数集合
     * @param parserDataType 解析器数据类型
     * @return ParsingParamSpec
     */
    public static ParsingParamSpec setPlugInRootNode(PlugIn plugInClass, List<ParsingParamSpec> list, ParserDataType parserDataType) {
        PlugInType annotation = plugInClass.getClass().getAnnotation(PlugInType.class);
        String parserId = annotation.id();
        // 初始化节点
        ParsingParamSpec node = ParsingParamSpec.builder()
                .parserId(parserId)
                .channel(annotation.channel())
                .path(parserId)
                .dataType(parserDataType)
                .build();
        list.add(node);
        return node;
    }

    /**
     * 设置插件根节点
     *
     * @param nodePath       节点路径
     * @param channel        插件通道
     * @param list           解析器参数集合
     * @param parserDataType 解析器数据类型
     * @return ParsingParamSpec
     */
    public static ParsingParamSpec setRootNode(String nodePath, Integer channel, List<ParsingParamSpec> list, ParserDataType parserDataType) {
        // 初始化节点
        ParsingParamSpec node = ParsingParamSpec.builder()
                .parserId(nodePath)
                .channel(channel)
                .path(nodePath)
                .dataType(parserDataType)
                .build();
        list.add(node);
        return node;
    }

    /**
     * 设置插件根节点
     *
     * @param plugInClass    插件类
     * @param id             插件id
     * @param list           解析器参数集合
     * @param parserDataType 解析器数据类型
     * @return ParsingParamSpec
     */
    public static ParsingParamSpec setPlugInNode(PlugIn plugInClass, String id, List<ParsingParamSpec> list, ParserDataType parserDataType) {
        PlugInType annotation = plugInClass.getClass().getAnnotation(PlugInType.class);
        // 初始化节点
        ParsingParamSpec node = ParsingParamSpec.builder()
                .parserId(id)
                .channel(annotation.channel())
                .path(id)
                .dataType(parserDataType)
                .build();
        list.add(node);
        return node;
    }

    /**
     * 设置插件array根节点-特殊情况modbus-不做拆包处理（对应子节点做拆包处理）
     *
     * @param plugInClass    插件类
     * @param list           解析器参数集合
     * @param parserDataType 解析器数据类型
     * @return ParsingParamSpec
     */
    public static ParsingParamSpec setPlugInArrayNode(PlugIn plugInClass, List<ParsingParamSpec> list, ParserDataType parserDataType) {
        PlugInType annotation = plugInClass.getClass().getAnnotation(PlugInType.class);
        // 获取解析器参数集合
        Map<String, ParsingParamSpec> map = listToParsingParamMap(list);
        // 初始化节点
        ParsingParamSpec node = map.get(annotation.id());
        if (node == null) {
            // 设置节点
            node = ParsingParamSpec.builder()
                    .parserId(annotation.id())
                    .channel(annotation.channel())
                    .path(annotation.id())
                    .dataType(parserDataType)
                    .build();
            list.add(node);
        }
        return node;
    }

    /**
     * 版本号解码
     */
    public static String versionDecoding(String value, boolean isHexString) {
        StringBuilder versionBuilder = new StringBuilder("v");
        // 0101转成["01","01"]
        List<String> versionSplit = IntStream.range(0, value.length() / 2)
                .mapToObj(i -> value.substring(i * 2, i * 2 + 2))
                .collect(Collectors.toList());
        for (int idx = 0; idx < versionSplit.size(); idx++) {
            // 不是第一个字节，则添加点号分隔符
            if (idx != 0) {
                versionBuilder.append(".");
            }
            int version = Integer.parseInt(versionSplit.get(idx), 16);
            // 添加到版本号构建器中
            versionBuilder.append(isHexString ? Integer.toHexString(version) : version);
        }
        return versionBuilder.toString();
    }

    /**
     * 读取版本号，16进制编码
     */
    @SuppressWarnings("java:S4425")
    public static String readVersion(byte[] bytes) {
        StringBuilder versionBuilder = new StringBuilder("v");
        for (int idx = 0; idx < bytes.length; idx++) {
            // 不是第一个字节，则添加点号分隔符
            if (idx != 0) {
                versionBuilder.append(".");
            }
            // 添加到版本号构建器中
            versionBuilder.append(Integer.toHexString(bytes[idx] & 0xff));
        }
        return versionBuilder.toString();
    }

    /**
     * 列表到解析参数映射
     */
    public static Map<String, ParsingParamSpec> listToParsingParamMap(List<ParsingParamSpec> parsingParamSpecList) {
        return parsingParamSpecList.stream()
                .distinct()
                .collect(Collectors.toMap(
                        ParsingParamSpec::getPath,
                        parsingParamSpec -> parsingParamSpec,
                        (v1, v2) -> v1
                ));
    }

    /**
     * 设置插件array里属性节点
     *
     * @param list     解析器参数集合
     * @param parent   父节点
     * @param nodePath 子节点路径
     * @param index    索引
     * @return ParsingParamSpec
     */
    public static ParsingParamSpec setPlugInArrayParamNode(List<ParsingParamSpec> list, ParsingParamSpec parent, String nodePath, ParserDataType parserDataType, int index) {
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
     * 设置插件子节点
     *
     * @param parent         父节点
     * @param nodePath       子节点路径
     * @param parserDataType 解析器数据类型
     * @return ParsingParamSpec
     */
    public static ParsingParamSpec setPlugInChildNode(List<ParsingParamSpec> list, ParsingParamSpec parent, String nodePath, ParserDataType parserDataType) {
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

    /**
     * 获取Modbus节点属性数据
     */
    public static ParsingParamSpec getModbusChildNode(ParsingParamSpec parent, String nodeId) {
        return ParsingParamSpec.builder()
                .parserId(CommonConstants.getPath(parent.getParserId(), nodeId))
                .channel(parent.getChannel())
                .path(CommonConstants.getPath(parent.getPath(), nodeId))
                .parentId(parent.getParserId())
                .build();
    }

    /**
     * 设置编码指令头（只有通道，没有类型）
     */
    public static void setCommandHeaderByChannel(PlugIn plugInClass, DynamicByteBuffer dynamicByteBuffer) {
        PlugInType annotation = plugInClass.getClass().getAnnotation(PlugInType.class);
        ByteBuffer buffer = ByteBuffer.allocate(1);
        buffer.put((byte) (annotation.channel() & 0xFF));
        dynamicByteBuffer.append(buffer.array());
    }

    /**
     * 设置编码指令头
     */
    public static void setCommandHeader(PlugIn plugInClass, DynamicByteBuffer dynamicByteBuffer) {
        PlugInType annotation = plugInClass.getClass().getAnnotation(PlugInType.class);
        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.put((byte) (annotation.channel() & 0xFF));
        buffer.put((byte) (annotation.type() & 0xFF));
        dynamicByteBuffer.append(buffer.array());
    }

    /**
     * 设置指定值
     *
     * @param dynamicByteBuffer 动态字节缓冲区
     * @param bytes             值
     */
    public static void setSpecifiedValue(DynamicByteBuffer dynamicByteBuffer, byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        dynamicByteBuffer.append(buffer.array());
    }

    /**
     * 设置固定值单字节
     *
     * @param dynamicByteBuffer 动态字节缓冲区
     * @param byteValue         固定值
     */
    public static void setFixedSingleByteValue(DynamicByteBuffer dynamicByteBuffer, byte byteValue) {
        ByteBuffer buffer = ByteBuffer.allocate(1);
        buffer.put(byteValue);
        dynamicByteBuffer.append(buffer.array());
    }

    /**
     * 解析器编码转换-数值
     *
     * @param parsingParamSpec 解析器参数
     */
    public static Number parserEncodeTransformerNumber(ParsingParamSpec parsingParamSpec) {
        if (parsingParamSpec == null || parsingParamSpec.getDataType() == null) {
            return null;
        }
        switch (parsingParamSpec.getDataType()) {
            case BOOL:
                return Boolean.TRUE.equals(parsingParamSpec.getBoolValue()) ? (byte) 1 : (byte) 0;
            case INT:
                return parsingParamSpec.getIntValue();
            case LONG:
                return parsingParamSpec.getLongValue();
            case FLOAT:
                return parsingParamSpec.getFloatValue();
            case DOUBLE:
                return parsingParamSpec.getDoubleValue();
            case STRING:
                val str = parsingParamSpec.getStringValue();
                if (str == null) {
                    return null;
                }
                if (str.contains(".")) {
                    return Double.parseDouble(str);
                } else {
                    return Long.decode(str);
                }
            default:
                throw new IllegalArgumentException("parser data parsing failed,parserEncodeTransformerNumber not supported data type:" + parsingParamSpec.getDataType());
        }
    }

    /**
     * 解析器编码转换-字符串
     *
     * @param parsingParamSpec 解析器参数
     */
    public static String parserEncodeTransformerString(ParsingParamSpec parsingParamSpec) {
        switch (parsingParamSpec.getDataType()) {
            case BOOL:
                return parsingParamSpec.getBoolValue().toString();
            case INT:
                return parsingParamSpec.getIntValue().toString();
            case LONG:
                return parsingParamSpec.getLongValue().toString();
            case FLOAT:
                return parsingParamSpec.getFloatValue().toString();
            case DOUBLE:
                return parsingParamSpec.getDoubleValue().toString();
            case STRING:
                return parsingParamSpec.getStringValue();
            default:
                throw new IllegalArgumentException("parser data parsing failed,parserEncodeTransformerString not supported data type:" + parsingParamSpec.getDataType());
        }
    }

    /**
     * 编码处理字符串编码类型
     */
    private static byte[] handleEncodeStringEncoding(ParserStringEncoding encoding, String str) {
        switch (encoding) {
            case HEX:
                try {
                    return Hex.decodeHex(str);
                } catch (DecoderException e) {
                    // 处理解码异常
                    throw new RuntimeException("parser data parsing failed, decodeHex error:" + e.getMessage());
                }
            case UTF8:
                return str.getBytes(StandardCharsets.UTF_8);
            case BASE64:
                return Base64.getDecoder().decode(str);
            case ASCII:
                return str.getBytes(StandardCharsets.US_ASCII);
            default:
                return str.getBytes();
        }
    }

    /**
     * 获取指定大小的缓冲区-数字类型-默认小端模式
     */
    public static ByteBuffer getByteBufferByNumberSize(int size) {
        // 分配指定大小的缓冲区 字节大小
        ByteBuffer buffer = ByteBuffer.allocate(size);
        // 默认小端模式
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return buffer;
    }

    /**
     * 获取指定大小的缓冲区-字符串类型-默认大端模式
     */
    public static ByteBuffer getByteBufferByStringSize(byte[] bytes, ByteOrder byteOrder) {
        // 分配指定大小的缓冲区 字节大小
        ByteBuffer buffer;
        buffer = ByteBuffer.wrap(bytes);
        if (byteOrder != null) {
            buffer.order(byteOrder);
        } else {
            // 默认大端模式
            buffer.order(ByteOrder.BIG_ENDIAN);
        }
        return buffer;
    }

    /**
     * 设置编码数据值
     */
    public static void setEncodeStringDataValue(DynamicByteBuffer dynamicByteBuffer, ParsingParamSpec parsingParamSpec, int payloadLength, ParserStringEncoding encoding, ByteOrder byteOrder) {
        // 字符串数据类型
        String value = Objects.requireNonNull(parserEncodeTransformerString(parsingParamSpec));
        // 获取指定大小的缓冲区-字符串类型
        ByteBuffer buffer = getByteBufferByStringSize(handleEncodeStringEncoding(encoding, value), byteOrder);
        byte[] bytes = buffer.array();
        if (bytes.length > payloadLength) {
            throw new RuntimeException("parser data parsing failed,payloadLength is too small");
        } else if (bytes.length == payloadLength) {
            dynamicByteBuffer.append(bytes);
        } else {
            dynamicByteBuffer.append(bytes);
            int n = payloadLength - bytes.length;
            byte[] padding = new byte[n];
            Arrays.fill(padding, (byte) 0);
            dynamicByteBuffer.append(padding);
        }
    }


    /**
     * 设置编码Bit位置数据值
     */
    public static void setEncodePositionsBitDataValue(List<Integer> values, List<Integer> positions, ParsingParamSpec parsingParamSpec, int startBit) {
        // 位置
        positions.add(startBit);
        // 获取数据值
        Number value = Objects.requireNonNull(parserEncodeTransformerNumber(parsingParamSpec));
        // 设置数据值
        values.add(value.intValue());
    }

    /**
     * 根据值和位置创建字节(单字节)
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
     * 设置编码数据值
     */
    public static Number setEncodeNumberDataValue(DynamicByteBuffer dynamicByteBuffer, ParsingParamSpec parsingParamSpec, ParserDataType parserDataType) {
        ByteBuffer buffer;
        byte[] bytes;
        // number数据类型
        Number value = Objects.requireNonNull(parserEncodeTransformerNumber(parsingParamSpec));
        switch (parserDataType) {
            case BOOL:
            case INT8:
            case UINT8:
                // 获取指定大小的缓冲区-数值类型
                buffer = getByteBufferByNumberSize(1);
                buffer.put(value.byteValue());
                bytes = buffer.array();
                break;
            case INT16:
            case UINT16:
                // 获取指定大小的缓冲区-数值类型
                buffer = getByteBufferByNumberSize(2);
                buffer.putShort(value.shortValue());
                bytes = buffer.array();
                break;
            case INT32:
            case UINT32:
                // 获取指定大小的缓冲区-数值类型
                buffer = getByteBufferByNumberSize(4);
                buffer.putInt(value.intValue());
                bytes = buffer.array();
                break;
            case INT64:
                // 获取指定大小的缓冲区-数值类型
                buffer = getByteBufferByNumberSize(8);
                buffer.putLong(value.longValue());
                bytes = buffer.array();
                break;
            case FLOAT:
                // 获取指定大小的缓冲区-数值类型
                buffer = getByteBufferByNumberSize(4);
                buffer.putFloat(value.floatValue());
                bytes = buffer.array();
                break;
            case DOUBLE:
                // 获取指定大小的缓冲区-数值类型
                buffer = getByteBufferByNumberSize(8);
                buffer.putDouble(value.doubleValue());
                bytes = buffer.array();
                break;
            default:
                throw new RuntimeException("parser data parsing failed, setEncodeDataValue not supported data type:" + parserDataType);
        }
        dynamicByteBuffer.append(bytes);
        return value;
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
                buffer = getByteBufferByNumberSize(1);
                buffer.put((byte) byteValue);
                bytes = buffer.array();
                break;
            case 2:
                // 获取指定大小的缓冲区-数值类型-小端处理
                buffer = getByteBufferByNumberSize(2);
                buffer.putShort((short) byteValue);
                bytes = buffer.array();
                break;
            case 4:
                // 获取指定大小的缓冲区-数值类型-小端处理
                buffer = getByteBufferByNumberSize(4);
                buffer.putInt(byteValue);
                bytes = buffer.array();
                break;
            default:
                break;
        }
        assert bytes != null;
        dynamicByteBuffer.append(bytes);
    }



    /**
     * 设置协议值
     *
     * @param bytes 字节数组
     * @return 协议值
     */
    public static String setProtocolValue(byte[] bytes) {
        StringBuilder byteBuilder = new StringBuilder();
        // 转换成16进制
        for (byte aByte : bytes) {
            byteBuilder.append(String.format("%02x", aByte & 0xff));
        }
        return byteBuilder.toString().toLowerCase();
    }


    /**
     * 校验时间戳是否为1973年之前
     *
     * @param timestampValue 时间戳
     * @return boolean
     */
    public static boolean checkTimestamp(long timestampValue) {
        return timestampValue <= 100000000;
    }

    /**
     * 固件版本编码
     *
     * @param dynamicByteBuffer 动态直直接缓冲区
     * @param value             值
     * @param radix             进制
     */
    public static void currentVersionEncode(DynamicByteBuffer dynamicByteBuffer, String value, int radix) {
        String[] versionSplit = getVersionSplit(value);
        ByteBuffer buffer = ByteBuffer.allocate(versionSplit.length);
        for (String s : versionSplit) {
            buffer.put((byte) (Integer.parseInt(s, radix)));
        }
        dynamicByteBuffer.append(buffer.array());
    }

    public static String[] getVersionSplit(String value) {
        if (StringUtils.isEmpty(value)) {
            log.error("Encode firmware_version value error");
            throw new RuntimeException("Encode firmware_version value error");
        }
        String version = value.replace("v", "");
        return version.split("\\.");
    }
}
