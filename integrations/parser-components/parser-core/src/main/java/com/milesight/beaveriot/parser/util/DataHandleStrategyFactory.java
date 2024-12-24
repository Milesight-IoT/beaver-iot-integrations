package com.milesight.beaveriot.parser.util;

import com.milesight.beaveriot.context.support.SpringContext;
import com.milesight.beaveriot.parser.annotaion.DataStruct;
import com.milesight.beaveriot.parser.enums.ParserDataType;
import com.milesight.beaveriot.parser.service.DataHandleStrategy;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 数据处理策略工厂
 *
 * @author lzy
 */
public class DataHandleStrategyFactory {
    private DataHandleStrategyFactory() {
    }

    /**
     * 缓存数据处理策略
     */
    private static final Map<String, DataHandleStrategy> DATA_HANDLE_BEANS = new ConcurrentHashMap<>();

    public static DataHandleStrategy getReceiptHandleStrategy(ParserDataType dataStructureType) {
        String key = dataStructureType.toString();
        if (DATA_HANDLE_BEANS.containsKey(key)) {
            return DATA_HANDLE_BEANS.get(key);
        }

        Map<String, DataHandleStrategy> beans = SpringContext.getBeansOfType(DataHandleStrategy.class);

        Optional<DataHandleStrategy> strategyOptional = beans.values()
                .stream()
                .filter(strategy -> {
                    DataStruct dataStruct = strategy.getClass().getAnnotation(DataStruct.class);
                    return dataStruct.value().equals(dataStructureType);
                }).findAny();
        if (strategyOptional.isPresent()) {
            DATA_HANDLE_BEANS.put(key, strategyOptional.get());
            return strategyOptional.get();
        }
        // 理论上不会为空
        return null;
    }
}