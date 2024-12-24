package com.milesight.beaveriot.parser.service;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.milesight.beaveriot.parser.enums.ParserDataType;
import com.milesight.beaveriot.parser.model.DeviceData;
import com.milesight.beaveriot.parser.model.ParserPayload;
import com.milesight.beaveriot.parser.model.ParsingParamSpec;
import com.milesight.cloud.sdk.client.model.ThingSpec;
import com.milesight.cloud.sdk.client.model.TslDataSpec;
import com.milesight.cloud.sdk.client.model.TslServiceSpec;
import lombok.val;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static com.milesight.beaveriot.parser.util.ParserUtil.*;
import static com.milesight.beaveriot.parser.util.ThingUtil.*;

/**
 * @Description: tsl处理
 */
public class ThingHandle {

    /**
     * 解码处理
     *
     * @return 是否解析完成
     */
    public static void decoderData(ParserPayload parserPayload) {
        parserPayload.getParserDecodeResponseList().forEach(parserDecodeResponse -> {
            val parserData = parserDecodeResponse.getPayload();
            List<ThingSpec> thingSpecifications = parserPayload.getProductDesc().getThingSpecifications();
            // 使用 Jackson 进行反序列化
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true);
            ThingSpec thingSpec = objectMapper.convertValue(thingSpecifications.get(0), ThingSpec.class);
            val parserListMap = groupingByParserIdPrefix(parserData);
            List<DeviceData> deviceDataList = new ArrayList<>();
            parserListMap.keySet().forEach(parserIdPrefix -> {
                val parserList = parserListMap.get(parserIdPrefix);
                String parentId = null;
                DeviceData deviceData = thingDecoderHandle(thingSpec, parserList, parentId);
                deviceDataList.add(deviceData);
            });
            parserPayload.setThingData(deviceDataList);
        });
    }

    /**
     * 编码处理
     *
     * @return 是否解析完成
     */
    public static void encoderData(ParserPayload parserPayload) {
        List<ParsingParamSpec> parserList = new ArrayList<>();
        List<ThingSpec> thingSpecifications = parserPayload.getProductDesc().getThingSpecifications();
        // 使用 Jackson 进行反序列化
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true);
        ThingSpec thingSpec = objectMapper.convertValue(thingSpecifications.get(0), ThingSpec.class);
        Map<String, Object> resultMap = new HashMap<>();
        val jsonData = parserPayload.getJsonData();
        encoderTslData(parserList, thingSpec, resultMap, jsonData);
        // parserList根据path字段去重
        parserList = parserList.stream().collect(Collectors.toMap(ParsingParamSpec::getPath, parsingParamSpec -> parsingParamSpec)).entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toList());
        parserPayload.setParserDownLinkData(parserList);
    }

    private static void encoderTslData(List<ParsingParamSpec> parserList, ThingSpec thingSpec, Map<String, Object> resultMap, JsonNode jsonData) {
        jsonNodeToMap(resultMap, jsonData);
        resultMap.keySet().forEach(key -> {
            val splits = key.split("\\.");
            val keyPrefix = splits[0];
            val value = resultMap.get(key);
            // PROPERTY
            val properties = thingSpec.getProperties();
            val propertiesMap = groupingByTslPropertyPrefix(properties);
            if (propertiesMap.get(keyPrefix) != null) {
                if (splits.length > 1) {
                    val tslPropertySpecs = propertiesMap.get(keyPrefix).stream()
                            .filter(tslPropertySpec -> tslPropertySpec.getId().equals(keyPrefix)).collect(Collectors.toList()).get(0);
                    ParsingParamSpec parentParam = ParsingParamSpec.builder()
                            .parserId(keyPrefix)
                            .path(keyPrefix)
                            .dataType(ParserDataType.valueOf(tslPropertySpecs.getDataSpec().getDataType().name()))
                            .build();
                    parserList.add(parentParam);
                }
                val tslPropertySpecs = propertiesMap.get(key).get(0);
                ParsingParamSpec parsingParamSpec = ParsingParamSpec.builder()
                        .parserId(tslPropertySpecs.getId())
                        .path(tslPropertySpecs.getId())
                        .build();
                setThingDecodeDataValue(value, tslPropertySpecs.getDataSpec(), parsingParamSpec);
                parserList.add(parsingParamSpec);
            }
            // SERVICE
            val services = thingSpec.getServices();
            Map<String, List<TslServiceSpec>> servicesMap = groupingByTslServicePrefix(services);
            if (servicesMap.get(keyPrefix) != null) {
                ParsingParamSpec parentParam = ParsingParamSpec.builder()
                        .parserId(keyPrefix)
                        .path(keyPrefix)
                        .dataType(ParserDataType.STRUCT)
                        .build();
                parserList.add(parentParam);
                if (splits.length > 1) {
                    val serviceSpecs = servicesMap.get(keyPrefix).get(0);
                    val inputs = serviceSpecs.getInputs();
                    val tslParamSpecMap = tslParamSpecToMap(inputs);
                    val tslParamSpecs = tslParamSpecMap.get(key);
                    ParsingParamSpec parsingParamSpec = ParsingParamSpec.builder()
                            .parserId(key)
                            .path(key)
                            .build();
                    setThingDecodeDataValue(value, tslParamSpecs != null ? tslParamSpecs.getDataSpec() : null, parsingParamSpec);
                    parserList.add(parsingParamSpec);
                }
            }
        });
    }

    public static void setThingDecodeDataValue(Object object, TslDataSpec parserDataSpec, ParsingParamSpec parsingParamSpec) {
        if (parserDataSpec == null || TslDataSpec.DataTypeEnum.STRING.equals(parserDataSpec.getDataType())) {
            parsingParamSpec.setDataType(ParserDataType.STRING);
            parsingParamSpec.setStringValue(object.toString());
            return;
        }
        // number数据类型
        Number value = Objects.requireNonNull(thingEncodeTransformerNumber(object));
        // 数据类型
        switch (parserDataSpec.getDataType()) {
            case BOOL:
                parsingParamSpec.setDataType(ParserDataType.BOOL);
                parsingParamSpec.setBoolValue(value.intValue() == 0 ? false : true);
                break;
            case INT:
                parsingParamSpec.setDataType(ParserDataType.INT);
                parsingParamSpec.setIntValue(value.intValue());
                break;
            case LONG:
                parsingParamSpec.setDataType(ParserDataType.LONG);
                // 数字类型字节转化
                parsingParamSpec.setLongValue(value.longValue());
                break;
            case FLOAT:
                parsingParamSpec.setDataType(ParserDataType.FLOAT);
                // 数字类型字节转化
                parsingParamSpec.setFloatValue(value.floatValue());
                break;
            case DOUBLE:
                parsingParamSpec.setDataType(ParserDataType.DOUBLE);
                // 数字类型字节转化
                parsingParamSpec.setDoubleValue(value.doubleValue());
                break;
            default:
                break;
        }
    }

    private static Number thingEncodeTransformerNumber(Object object) {
        // object转数字类型
        if (object instanceof String) {
            try {
                return new BigDecimal((String) object);
            } catch (Exception e) {
                return null;
            }
        } else if (object instanceof Boolean) {
            return (Boolean) object ? 1 : 0;
        }
        return object instanceof Number ? (Number) object : null;

    }

    private static void jsonNodeToMap(Map<String, Object> map, JsonNode node) {
        // 遍历 JsonNode 的所有字段
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String key = field.getKey();
            JsonNode valueNode = field.getValue();

            // 将 JsonNode 转换为适当的 Java 对象
            Object value;
            if (valueNode.isTextual()) {
                value = valueNode.asText();
            } else if (valueNode.isInt()) {
                value = valueNode.asInt();
            } else if (valueNode.isDouble()) {
                value = valueNode.asDouble();
            } else if (valueNode.isBoolean()) {
                value = valueNode.asBoolean();
            } else if (valueNode.isObject()) {
                value = valueNode.toString(); // 或者递归调用 nodesToMap(valueNode)
            } else if (valueNode.isArray()) {
                value = valueNode.toString(); // 或者处理成 List
            } else {
                value = valueNode.toString();
            }

            // 将键值对放入 Map
            map.put(key, value);
        }
    }
}