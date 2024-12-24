package com.milesight.beaveriot.parser.util;

import com.milesight.beaveriot.parser.model.*;
import com.milesight.beaveriot.parser.service.DataHandleStrategy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Configuration;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * @Description: 上下文类，持有策略接口
 */
@Configuration
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DataHandleStrategyContext {

    /**
     * 策略接口
     */
    private DataHandleStrategy dataHandleStrategy;


    /**
     * 处理解码数据
     */
    public void handleDecodersReceipt(List<ParsingParamSpec> list, ByteBuffer buffer, List<ParserDataSpec> parserDataSpecList, List<ParserDecodeResponse> unpackList) {
        if (dataHandleStrategy != null) {
            dataHandleStrategy.handleDecoders(list, buffer, parserDataSpecList, unpackList);
        }
    }

    /**
     * 处理编码数据
     */
    public void handleEncodersReceipt(List<ParsingParamSpec> list, DynamicByteBuffer dynamicByteBuffer, List<ParserDataSpec> parserDataSpecList, ParserEncodeResponse parserEncodeResponse) {
        if (dataHandleStrategy != null) {
            dataHandleStrategy.handleEncoders(list, dynamicByteBuffer, parserDataSpecList, parserEncodeResponse);
        }
    }
}