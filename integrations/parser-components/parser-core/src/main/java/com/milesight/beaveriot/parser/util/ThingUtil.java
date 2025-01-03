package com.milesight.beaveriot.parser.util;


import cn.hutool.core.util.NumberUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.milesight.beaveriot.parser.model.DeviceData;
import com.milesight.beaveriot.parser.model.ParsingParamSpec;
import com.milesight.cloud.sdk.client.model.*;
import lombok.val;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.milesight.beaveriot.parser.util.ParserUtil.*;

/**
 * Thing工具类
 */
public class ThingUtil {

    public static DeviceData thingDecoderHandle(ThingSpec thingSpec, List<ParsingParamSpec> parserList, String parentId) {
        val current = parserList.stream().filter(
                parserDataSpec -> Objects.equals(parserDataSpec.getParentId(), parentId)
        ).toList();
        val properties = thingSpec.getProperties();
        val propertiesMap = groupingByTslPropertyPrefix(properties);
        final val obj = parserList.get(0);
        List<DeviceData> deviceDataArrayList = new ArrayList<>();
        if (propertiesMap.get(obj.getParserId()) != null) {
            String type = "PROPERTY";
            final val tslPropertySpecs = propertiesMap.get(obj.getParserId());
            parserList.forEach(parsingParamSpec -> {
                deviceDataArrayList.add(getDeviceDataByProperty(parsingParamSpec, tslPropertySpecs, type));
            });
        }
        val events = thingSpec.getEvents();
        val eventsMap = groupingByTslEventPrefix(events);
        if (eventsMap.get(obj.getParserId()) != null) {
            String type =  "EVENT";
            parserList.forEach(parsingParamSpec -> {
                deviceDataArrayList.add(getDeviceDataByEvent(parserList, events, type));
            });
        }
        return deviceDataArrayList.get(0);
    }

    private static DeviceData getDeviceDataByEvent(List<ParsingParamSpec> parserList, List<TslEventSpec> events, String type) {
        Map<String, Object> valueMap = new HashMap<>();
        parserList.forEach(parsingParamSpec -> {
            val path = parsingParamSpec.getPath();
            val tslEventSpec = events.stream().filter(
                    event -> Objects.equals(event.getId(), path)
            ).findFirst().orElse(null);
            switch (parsingParamSpec.getDataType()) {
                case INT:
                    valueMap.put(path, parsingParamSpec.getIntValue().toString());
                    break;
                case LONG:
                    valueMap.put(path, parsingParamSpec.getLongValue().toString());
                    break;
                case FLOAT:
                    valueMap.put(path, parsingParamSpec.getFloatValue().toString());
                    break;
                case DOUBLE:
                    valueMap.put(path, parsingParamSpec.getDoubleValue().toString());
                    break;
                case BOOL:
                    valueMap.put(path, parsingParamSpec.getBoolValue().toString());
                    break;
                case STRING:
                    valueMap.put(path, parsingParamSpec.getStringValue());
                    break;
                default:
                    break;
            }
        });
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            // 将 Person 对象转换为 JsonNode
            JsonNode personNode = objectMapper.valueToTree(valueMap);
            return DeviceData.builder()
                    .type(type)
                    .tslId(parserList.get(0).getParserId())
                    .payload(personNode)
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static DeviceData getDeviceDataByProperty(ParsingParamSpec parsingParamSpec, List<TslPropertySpec> list, String type) {
        Map<String, Object> valueMap = new HashMap<>();
        val path = parsingParamSpec.getPath();
        final val tslPropertySpec = list.stream().filter(
                event -> Objects.equals(event.getId(), path)
        ).findFirst().orElse(null);
        val dataSpec = tslPropertySpec.getDataSpec();
        if (TslDataSpec.DataTypeEnum.STRING.equals(dataSpec.getDataType())) {
            switch (parsingParamSpec.getDataType()) {
                case INT:
                    valueMap.put(path, parsingParamSpec.getIntValue().toString());
                    break;
                case LONG:
                    valueMap.put(path, parsingParamSpec.getLongValue().toString());
                    break;
                case FLOAT:
                    valueMap.put(path, parsingParamSpec.getFloatValue().toString());
                    break;
                case DOUBLE:
                    valueMap.put(path, parsingParamSpec.getDoubleValue().toString());
                    break;
                case BOOL:
                    valueMap.put(path, parsingParamSpec.getBoolValue().toString());
                    break;
                case STRING:
                    valueMap.put(path, parsingParamSpec.getStringValue());
                    break;
                default:
                    break;
            }
        } else {
            // number数据类型
            Number number = parserEncodeTransformerNumber(parsingParamSpec);
            BigDecimal value = null;
            if (number instanceof BigDecimal) {
                value =  (BigDecimal) number;
            } else if (number instanceof Integer || number instanceof Long) {
                value = BigDecimal.valueOf(number.longValue());
            } else if (number instanceof Float || number instanceof Double) {
                value = BigDecimal.valueOf(number.doubleValue());
            }
            value = decodeConvertNumber(value, dataSpec);
            switch (dataSpec.getDataType()) {
                case INT:
                    valueMap.put(path, value.intValue());
                    break;
                case LONG:
                    valueMap.put(path, value.longValue());
                    break;
                case FLOAT:
                    valueMap.put(path, value.floatValue());
                    break;
                case DOUBLE:
                    valueMap.put(path, value.doubleValue());
                    break;
                case BOOL:
                    valueMap.put(path, parsingParamSpec.getBoolValue());
                    break;
                case ENUM:
                    final val tslKeyValuePair = dataSpec.getMappings().get(value.intValue());
                    valueMap.put(path, tslKeyValuePair);
                    break;
                case STRING:
                    valueMap.put(path, parsingParamSpec.getStringValue());
                    break;
                default:
                    break;
            }
        }

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            // 将 Person 对象转换为 JsonNode
            JsonNode personNode = objectMapper.valueToTree(valueMap);
            return DeviceData.builder()
                    .type(type)
                    .payload(personNode)
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static BigDecimal decodeConvertNumber(BigDecimal value, TslDataSpec dataSpec) {
        if (dataSpec == null) {
            return value;
        }
        if (dataSpec.getCoefficient() != null) {
            value = NumberUtil.mul(value, dataSpec.getCoefficient());
        }
        if (dataSpec.getFractionDigits() != null) {
            value = BigDecimal.valueOf(value.doubleValue())
                    .setScale(Math.toIntExact(dataSpec.getFractionDigits()), RoundingMode.HALF_UP);
        }
        return value;
    }

    public static BigDecimal encodeConvertNumber(BigDecimal value, TslDataSpec dataSpec) {
        if (dataSpec == null) {
            return value;
        }
        if (dataSpec.getCoefficient() != null) {
            value = NumberUtil.div(value, dataSpec.getCoefficient());
        }
        if (dataSpec.getFractionDigits() != null) {
            value = BigDecimal.valueOf(value.doubleValue())
                    .setScale(Math.toIntExact(dataSpec.getFractionDigits()), RoundingMode.HALF_UP);
        }
        return value;
    }


    /**
     * ParserParamSpec根据parserId前缀分组
     */
    public static Map<String, List<TslServiceSpec>> groupingByTslServicePrefix(List<TslServiceSpec> list) {
        return list.stream()
                .collect(Collectors.groupingBy(paramEntity -> extractFirstNode(paramEntity.getId()),
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    /**
     * list根据id转map
     */
    public static Map<String, TslParamSpec> tslParamSpecToMap(List<TslParamSpec> list) {
        // list根据id转map
        return list.stream()
                .collect(Collectors.toMap(TslParamSpec::getId, Function.identity()));
    }
}
