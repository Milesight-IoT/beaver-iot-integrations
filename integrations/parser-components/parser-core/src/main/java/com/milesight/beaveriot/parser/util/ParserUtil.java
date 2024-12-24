package com.milesight.beaveriot.parser.util;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.PrimitiveArrayUtil;
import com.milesight.beaveriot.parser.cache.ProductCache;
import com.milesight.beaveriot.parser.enums.Endianness;
import com.milesight.beaveriot.parser.enums.ParserDataType;
import com.milesight.beaveriot.parser.enums.ParserStringEncoding;
import com.milesight.beaveriot.parser.model.*;
import com.milesight.cloud.sdk.client.model.TslEventSpec;
import com.milesight.cloud.sdk.client.model.TslPropertySpec;
import com.milesight.cloud.sdk.client.model.TslServiceSpec;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.springframework.lang.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.milesight.beaveriot.parser.util.FloatUtil.float16ToFloat;
import static com.milesight.beaveriot.parser.util.FloatUtil.floatToFloat16;
import static com.milesight.beaveriot.parser.util.PlugInFactory.getSnMaskBySn;

/**
 * 解析器工具类
 */
@Slf4j
public class ParserUtil {

    // 16进制前缀
    private static final String HEX_PREFIX = "0x";

    // 16进制格式前缀
    private static final String HEX_FORMAT_PREFIX = "%02x";


    /**
     * 根据sn获取产品描述
     *
     * @param sn
     * @return
     */
    public static ProductDesc getProductDesc(String sn) {
        val snMaskBySn = getSnMaskBySn(sn);
        return ProductCache.getFromCache(snMaskBySn);
    }

    /**
     * 结构体排序
     * 按照起始位排序，如果起始位相同按照结束位倒序排序
     *
     * @param parserDataSpecList 待排序的列表
     */
    public static void structSort(List<ParserDataSpec> parserDataSpecList) {
        if (parserDataSpecList.isEmpty()) {
            // 如果列表为空，直接返回
            return;
        }

        // 根据order字段是否有效来选择排序策略
        boolean useOrder = parserDataSpecList.stream().allMatch(spec -> spec.getOrder() != -1);

        if (useOrder) {
            // 使用order字段排序
            parserDataSpecList.sort(Comparator.comparingInt(ParserDataSpec::getOrder));
        } else {
            // 按照起始位排序，如果起始位相同按照结束位倒序排序
            parserDataSpecList.sort(Comparator
                    .comparingInt((ParserDataSpec o) -> o.getStartByte() == null ? 0 : o.getStartByte())
                    .thenComparing((ParserDataSpec o) -> o.getEndByte() == null ? 9999 : o.getEndByte(), Comparator.reverseOrder()));
        }
    }

    /**
     * 获取解析器字节数组
     *
     * @param payload 解码参数
     * @return
     */
    public static ByteBuffer getDeviceByteBuffer(String payload) {
        byte[] bytes = Base64.getDecoder().decode(payload);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        // 小端模式
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return buffer;
    }

    /**
     * ParserDataSpec根据解析通道分组
     */
    public static Map<String, List<ParserDataSpec>> getParserDecoder(List<ParserDataSpec> parserDataSpecList) {
        return parserDataSpecList.stream()
                .collect(Collectors.groupingBy(parserEntity -> parserEntity.getChannel() + "|" + (parserEntity.getType() == null ? 0 : parserEntity.getType())));
    }

    /**
     * ParserDataSpec根据parserId前缀分组
     */
    public static Map<String, List<ParserDataSpec>> getParserEncoder(List<ParserDataSpec> parserDataSpecList) {
        return parserDataSpecList.stream()
                .collect(Collectors.groupingBy(parserEntity -> extractFirstNode(parserEntity.getId())));
    }


    /**
     * ParserParamSpec根据parserId前缀分组
     */
    public static Map<String, List<ParsingParamSpec>> groupingByParserIdPrefix(List<ParsingParamSpec> parsingParamSpecList) {
        return parsingParamSpecList.stream()
                .collect(Collectors.groupingBy(paramEntity -> extractFirstNode(paramEntity.getParserId()),
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    /**
     * ParserParamSpec根据parserId前缀分组
     */
    public static Map<String, List<TslPropertySpec>> groupingByTslPropertyPrefix(List<TslPropertySpec> list) {
        return list.stream()
                .collect(Collectors.groupingBy(paramEntity -> extractFirstNode(paramEntity.getId()),
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    /**
     * ParserParamSpec根据parserId前缀分组
     */
    public static Map<String, List<TslEventSpec>> groupingByTslEventPrefix(List<TslEventSpec> list) {
        return list.stream()
                .collect(Collectors.groupingBy(paramEntity -> extractFirstNode(paramEntity.getId()),
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    /**
     * 提取第一个节点
     */
    public static String extractFirstNode(String id) {
        int dotIndex = id.indexOf(".");
        if (dotIndex != -1) {
            String firstNode = id.substring(0, dotIndex);
            int bracketIndex = firstNode.indexOf("[");
            if (bracketIndex != -1) {
                return firstNode.substring(0, bracketIndex);
            }
            return firstNode;
        }
        return id;
    }

    /**
     * 解码查询
     *
     * @return List<ParserDataSpec>
     */
    public static List<ParserDataSpec> findDecoder(ParserPayload parserPayload, int channelId, int channelType) {
        Map<String, List<ParserDataSpec>> map = getParserDecoder(parserPayload.getProductDesc().getParser().getDecoder().getDefinitions());
        List<ParserDataSpec> resList = new ArrayList<>();
        String key = (channelId & 0xFF) + "|" + (channelType & 0xFF);
        if (map != null && map.get(key) != null) {
            // 获取分组解码器
            resList.addAll(map.get(key));
        }
        return resList;
    }

    /**
     * 编码查询
     *
     * @return List<ParserDataSpec>
     */
    public static List<ParserDataSpec> findEncoder(ParserPayload parserPayload, String key) {
        Map<String, List<ParserDataSpec>> encodeMap = getParserEncoder(parserPayload.getProductDesc().getParser().getEncoder().getDefinitions());
        // 添加反向编码支持
        supportReverseParsingEncoding(parserPayload, encodeMap);
        List<ParserDataSpec> resList = new ArrayList<>();
        if (encodeMap.get(key) != null) {
            // 获取分组解码器
            resList.addAll(encodeMap.get(key));
        }
        return resList;
    }

    /**
     * 反向编码支持
     */
    private static void supportReverseParsingEncoding(ParserPayload parserPayload, Map<String, List<ParserDataSpec>> encodeMap) {
        Map<String, List<ParserDataSpec>> decodeMap = getParserEncoder(parserPayload.getProductDesc().getParser().getEncoder().getDefinitions());
        encodeMap.putAll(decodeMap);
    }

    /**
     * 获取解码异常值
     *
     * @param buffer
     * @param parserDataSpec
     * @return
     */
    public static String getDecodeExceptionValue(ByteBuffer buffer, ParserDataSpec parserDataSpec) {
        //  保存buffer的position
        int position = buffer.position();
        // 物模型异常值
        Map<String, String> exceptionMap = parserDataSpec.getExceptionValues();
        if (CollUtil.isEmpty(exceptionMap)) {
            return null;
        }
        // 获取设备上报值
        String dataValue = getReportedValue(buffer, parserDataSpec);
        // 重置位置
        buffer.position(position);
        // 判断是否有异常值
        if (findExceptionValue(exceptionMap, dataValue) != null) {
            return dataValue;
        }
        return null;
    }


    /**
     * 获取设备上报值
     *
     * @param buffer
     * @param parserDataSpec
     * @return
     */
    private static String getReportedValue(ByteBuffer buffer, ParserDataSpec parserDataSpec) {
        // 获取字节数组
        byte[] bytes = getByteArray(buffer, parserDataSpec);
        // 异常值数组字节序反转
        byte[] finalBytes = exceptionArrayEndianReversal(bytes, parserDataSpec);
        // 获取字节数组16进制字符串
        return IntStream.range(0, bytes.length)
                .mapToObj(i -> String.format(HEX_FORMAT_PREFIX, finalBytes[i] & 0xff))
                .reduce(HEX_PREFIX, (acc, s) -> acc + s)
                .toLowerCase();
    }


    /**
     * 获取字节数组
     *
     * @param buffer
     * @param parserDataSpec
     * @return
     */
    public static byte[] getByteArray(ByteBuffer buffer, ParserDataSpec parserDataSpec) {
        Number startByte = parserDataSpec.getStartByte();
        Number endByte = parserDataSpec.getEndByte();
        // 检查边界
        if (startByte == null || endByte == null || endByte.intValue() < startByte.intValue()) {
            System.out.println("Invalid byte range: " + startByte + " to " + endByte);
        }
        // 初始化字节数组
        byte[] bytes = new byte[endByte.intValue() - startByte.intValue()];
        // 读取数据
        buffer.get(bytes);
        return bytes;
    }

    /**
     * 异常值数组字节序反转
     *
     * @param bytes
     * @param parserDataSpec
     * @return
     */
    private static byte[] exceptionArrayEndianReversal(byte[] bytes, ParserDataSpec parserDataSpec) {
        // 大小端模式-反转字节数组
        if (ParserDataType.STRING.equals(parserDataSpec.getDataType())) {
            if (parserDataSpec.getEndianness() != null && parserDataSpec.getEndianness().equals(Endianness.LE)) {
                // 小端模式-反转字节数组
                bytes = PrimitiveArrayUtil.reverse(bytes);
            }
        } else {
            if (parserDataSpec.getEndianness() == null || parserDataSpec.getEndianness().equals(Endianness.LE)) {
                // 小端模式-反转字节数组
                bytes = PrimitiveArrayUtil.reverse(bytes);
            }
        }
        return bytes;
    }

    /**
     * 查询异常值
     *
     * @param exceptionMap
     * @param dataValue
     * @return
     */
    private static String findExceptionValue(Map<String, String> exceptionMap, String dataValue) {
        for (Map.Entry<String, String> entry : exceptionMap.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(dataValue)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * 设置编码指令头
     */
    public static void setCommandHeader(ParserDataSpec parserDataSpec, DynamicByteBuffer dynamicByteBuffer) {
        Integer channel = parserDataSpec.getChannel();
        Integer type = parserDataSpec.getType();
        if (channel != null) {
            ByteBuffer buffer = ByteBuffer.allocate(1);
            buffer.put(channel.byteValue());
            dynamicByteBuffer.append(buffer.array());
        }
        if (type != null) {
            ByteBuffer buffer = ByteBuffer.allocate(1);
            buffer.put(type.byteValue());
            dynamicByteBuffer.append(buffer.array());
        }
    }

    /**
     * 解析器编码转换-数值
     *
     * @param parsingParamSpec 解析器参数
     */
    public static Number parserEncodeTransformerNumber(ParsingParamSpec parsingParamSpec) {
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
                throw new IllegalArgumentException("parser data parsing failed, parserEncodeTransformerNumber not supported data type:" + parsingParamSpec.getDataType());
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
                throw new IllegalArgumentException("parser data parsing failed, parserEncodeTransformerString not supported data type:" + parsingParamSpec.getDataType());
        }
    }

    /**
     * 设置编码数据值
     */
    @SuppressWarnings({"java:S3776"})
    public static void setEncodeDataValue(DynamicByteBuffer dynamicByteBuffer, ParsingParamSpec parsingParamSpec, ParserDataSpec parserDataSpec) {
        ByteBuffer buffer;
        byte[] bytes;
        if (ParserDataType.STRING.equals(parserDataSpec.getDataType())) {
            int payloadLength = parserDataSpec.getEndByte() - parserDataSpec.getStartByte();
            if (parserDataSpec.getFixedValue() != null || parsingParamSpec == null) {
                if (parsingParamSpec == null) {
                    // 成员属性补 0x00
                    buffer = getByteBufferByFixedValue(payloadLength, 0, parserDataSpec);
                } else {
                    // 固定值
                    buffer = getByteBufferByFixedValue(payloadLength, parserDataSpec.getFixedValue(), parserDataSpec);
                }
            } else {
                // 字符串数据类型
                String value = Objects.requireNonNull(parserEncodeTransformerString(parsingParamSpec));
                // 获取字符串数据编码
                byte[] preByte = handleEncodeStringEncoding(parserDataSpec.getEncoding(), value);
                // 获取指定大小的缓冲区
                ByteBuffer preBuffer = ByteBuffer.allocate(payloadLength);
                preBuffer.put(preByte);
                preBuffer.flip();
                byte[] strByte = preBuffer.array();
                // 获取指定大小的缓冲区-字符串类型
                buffer = getByteBufferByStringSize(strByte, parserDataSpec);
            }
            bytes = buffer.array();
            if (bytes.length != payloadLength) {
                throw new IllegalArgumentException("parser data parsing failed, setEncodeDataValue payloadLength error");
            }
        } else {
            if (parsingParamSpec == null) {
                // 成员属性补 0x00
                parsingParamSpec = ParsingParamSpec.builder()
                        .dataType(ParserDataType.INT)
                        .intValue(0)
                        .build();
            }
            // number数据类型
            Number value = parserDataSpec.getFixedValue() != null ? parserDataSpec.getFixedValue() : Objects.requireNonNull(parserEncodeTransformerNumber(parsingParamSpec));
            switch (parserDataSpec.getDataType()) {
                case BOOL:
                case INT8:
                case UINT8:
                    // 获取指定大小的缓冲区-数值类型
                    buffer = getByteBufferByNumberSize(1, parserDataSpec);
                    buffer.put(value.byteValue());
                    bytes = buffer.array();
                    break;
                case INT16:
                case UINT16:
                    // 获取指定大小的缓冲区-数值类型
                    buffer = getByteBufferByNumberSize(2, parserDataSpec);
                    buffer.putShort(value.shortValue());
                    bytes = buffer.array();
                    break;
                case INT32:
                case UINT32:
                    // 获取指定大小的缓冲区-数值类型
                    buffer = getByteBufferByNumberSize(4, parserDataSpec);
                    buffer.putInt(value.intValue());
                    bytes = buffer.array();
                    break;
                case INT64:
                    // 获取指定大小的缓冲区-数值类型
                    buffer = getByteBufferByNumberSize(8, parserDataSpec);
                    buffer.putLong(value.longValue());
                    bytes = buffer.array();
                    break;
                case FLOAT16:
                    // 获取指定大小的缓冲区-数值类型
                    buffer = getByteBufferByNumberSize(2, parserDataSpec);
                    buffer.putShort(parserDataSpec.getFixedValue() != null ? parserDataSpec.getFixedValue().shortValue() : floatToFloat16(value.floatValue()));
                    bytes = buffer.array();
                    break;
                case FLOAT:
                    // 获取指定大小的缓冲区-数值类型
                    buffer = getByteBufferByNumberSize(4, parserDataSpec);
                    buffer.putFloat(value.floatValue());
                    bytes = buffer.array();
                    break;
                case DOUBLE:
                    // 获取指定大小的缓冲区-数值类型
                    buffer = getByteBufferByNumberSize(8, parserDataSpec);
                    buffer.putDouble(value.doubleValue());
                    bytes = buffer.array();
                    break;
                default:
                    throw new IllegalArgumentException("parser data parsing failed, setEncodeDataValue not supported data type:" + parserDataSpec.getDataType());
            }
        }
        dynamicByteBuffer.append(bytes);
    }


    /**
     * 获取指定大小的缓冲区-固定值类型
     */
    public static ByteBuffer getByteBufferByFixedValue(int payloadLength, Number value, ParserDataSpec parserDataSpec) {
        // 获取指定大小的缓冲区-数值类型
        ByteBuffer buffer = getByteBufferByNumberSize(payloadLength, parserDataSpec);
        switch (payloadLength) {
            case 1:
                buffer.put(value.byteValue());
                break;
            case 2:
                buffer.putShort(value.shortValue());
                break;
            case 4:
                buffer.putInt(value.intValue());
                break;
            case 8:
                buffer.putLong(value.longValue());
                break;
            default:
                // 特殊字节长度处理
                // int 转 byte[]
                byte[] bytes = intToAsciiByteArray(value.intValue());
                if (parserDataSpec.getEndianness() != null && Endianness.LE.equals(parserDataSpec.getEndianness())) {
                    // 小端反转
                    bytes = PrimitiveArrayUtil.reverse(bytes);
                }
                buffer.put(bytes);
        }
        return buffer;
    }


    /**
     * int 转 byte[]
     *
     * @param number
     * @return
     */
    public static byte[] intToAsciiByteArray(int number) {

        // 将 int 数值转换为十六进制字符串
        String hexString = Integer.toHexString(number);

        // 如果十六进制字符串长度为奇数，前面补零
        if (hexString.length() % 2 != 0) {
            hexString = "0" + hexString;
        }

        // 创建一个字节数组，长度为字符串长度的一半
        int length = hexString.length() / 2;
        byte[] byteArray = new byte[length];

        // 将每两个十六进制字符转换为一个字节
        for (int i = 0; i < length; i++) {
            int index = i * 2;
            int j = Integer.parseInt(hexString.substring(index, index + 2), 16);
            byteArray[i] = (byte) j;
        }


        return byteArray;
    }

    /**
     * 获取指定大小的缓冲区-数字类型-默认小端模式
     */
    public static ByteBuffer getByteBufferByNumberSize(int size, ParserDataSpec parserDataSpec) {
        // 分配指定大小的缓冲区 字节大小
        ByteBuffer buffer = ByteBuffer.allocate(size);
        if (parserDataSpec != null && parserDataSpec.getEndianness() != null && Endianness.BE.equals(parserDataSpec.getEndianness())) {
            // 大端模式
            buffer.order(ByteOrder.BIG_ENDIAN);
        } else {
            // 默认小端模式
            buffer.order(ByteOrder.LITTLE_ENDIAN);
        }
        return buffer;
    }

    /**
     * 获取指定大小的缓冲区-字符串类型-默认大端模式
     */
    public static ByteBuffer getByteBufferByStringSize(byte[] bytes, ParserDataSpec parserDataSpec) {
        // 分配指定大小的缓冲区 字节大小
        ByteBuffer buffer;
        if (parserDataSpec.getEndianness() != null && Endianness.LE.equals(parserDataSpec.getEndianness())) {
            // 小端模式-反转字节数组
            bytes = PrimitiveArrayUtil.reverse(bytes);
            buffer = ByteBuffer.wrap(bytes);
        } else {
            buffer = ByteBuffer.wrap(bytes);
            // 默认大端模式
            buffer.order(ByteOrder.BIG_ENDIAN);
        }
        return buffer;
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
                    throw new IllegalArgumentException("parser data parsing failed, parserEncodeTransformerString hex decode error");
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
     * 设置解码正常数据值
     */
    public static void setDecodeDataValue(ByteBuffer buffer, ParsingParamSpec parsingParamSpec, ParserDataSpec parserDataSpec) {
        // 数据类型
        switch (parserDataSpec.getDataType()) {
            case BOOL:
                parsingParamSpec.setDataType(ParserDataType.BOOL);
                parsingParamSpec.setBoolValue(parserDataSpec.getFixedValue() != null ? parserDataSpec.getFixedValue() != 0 : buffer.get() != 0);
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
                // 数字类型字节转化
                parsingParamSpec.setIntValue((int) buffer.getShort());
                break;
            case UINT16:
                parsingParamSpec.setDataType(ParserDataType.INT);
                // 数字类型字节转化
                parsingParamSpec.setIntValue(buffer.getShort() & 0xffff);
                break;
            case INT32:
                parserDataSpec.setDataType(ParserDataType.LONG);
                // 数字类型字节转化
                parsingParamSpec.setLongValue((long) buffer.getInt());
                break;
            case UINT32:
                parsingParamSpec.setDataType(ParserDataType.LONG);
                // 数字类型字节转化
                parsingParamSpec.setLongValue(buffer.getInt() & 0xffffffffL);
                break;
            case INT64:
                parsingParamSpec.setDataType(ParserDataType.LONG);
                // 数字类型字节转化
                parsingParamSpec.setLongValue(buffer.getLong());
                break;
            case FLOAT16:
                parsingParamSpec.setDataType(ParserDataType.FLOAT);
                // 数字类型字节转化
                parsingParamSpec.setFloatValue(float16ToFloat(buffer.getShort()));
                break;
            case FLOAT:
                // 数字类型字节转化
                parsingParamSpec.setFloatValue(buffer.getFloat());
                break;
            case DOUBLE:
                // 数字类型字节转化
                parsingParamSpec.setDoubleValue(buffer.getDouble());
                break;
            case STRING:
                // 获取字节数组
                byte[] bytes = getByteArray(buffer, parserDataSpec);
                if (parserDataSpec.getEndianness() != null && parserDataSpec.getEndianness().equals(Endianness.LE)) {
                    // 小端模式-反转字节数组
                    bytes = PrimitiveArrayUtil.reverse(bytes);
                }
                // 解码处理字符串编码类型
                String str = handleDecodeStringEncoding(bytes, parserDataSpec.getEncoding());
                if (CharSequenceUtil.isNotEmpty(str)) {
                    // 去除空格
                    str = str.trim();
                }
                parsingParamSpec.setStringValue(str);
                break;
            default:
                break;
        }
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
     * 数字日期转换成秒
     *
     * @param data 数字日期
     * @return Long
     */
    public static long parseNumberDate(Number data) {
        val value = data.longValue();
        if (value <= 9999999999L) {
            // 秒
            return value;
        } else {
            // 毫秒
            return TimeUnit.MILLISECONDS.toSeconds(value);
        }
    }
}
