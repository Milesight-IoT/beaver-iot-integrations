package com.milesight.beaveriot.parser;

import com.milesight.beaveriot.parser.enums.DeviceType;
import com.milesight.beaveriot.parser.model.ParserPayload;
import com.milesight.beaveriot.parser.model.ProductDesc;
import com.milesight.beaveriot.parser.service.ParserHandle;
import com.milesight.beaveriot.parser.service.ThingHandle;
import com.milesight.cloud.sdk.client.model.ThingSpec;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.milesight.beaveriot.parser.enums.DeviceType.SUB_DEVICE;
import static com.milesight.beaveriot.parser.util.ParserUtil.getProductDesc;

/**
 * 解析器实现类
 *
 * @author linzy
 */
@Service
@Slf4j
public class ParserPluginService implements ParserPlugIn {


    @Override
    public ProductDesc getProductBySn(String sn) {
        return getProductDesc(sn);
    }

    @Override
    public List<ThingSpec> getThingSpecBySn(String sn) {
        ProductDesc productDesc = getProductDesc(sn);
        if (productDesc == null) {
            return Collections.emptyList();
        }
        return productDesc.getThingSpecifications();
    }

    @Override
    public ParserPayload decode(ParserPayload parserPayload) {
        ProductDesc productDesc = getProductDesc(parserPayload.getSn());
        if (productDesc == null) {
            log.error("sn:{} productDesc is null", parserPayload.getSn());
            return parserPayload;
        }
        parserPayload.setProductDesc(productDesc);
        parserPayload.setDeviceType(DeviceType.valueOf(productDesc.getProductInformation().getDeviceType()));
        if (SUB_DEVICE.equals(parserPayload.getDeviceType())) {
            try {
                // 子设备解析器数据处理
                ParserHandle.decoderData(parserPayload);
                parserPayload.getParserDecodeResponseList().forEach(parserDecodeResponse -> {
                    val parserData = parserDecodeResponse.getPayload();
                    List<String> exceptions = new ArrayList<>();
                    parserData.forEach(parserDataSpec -> {
                        if (parserDataSpec.getExceptionValue() != null) {
                            exceptions.add(parserDataSpec.getExceptionValue());
                        }
                    });
                    // 添加异常值
                    parserPayload.setExceptions(exceptions);
                    // 物模型数据处理
                    ThingHandle.decoderData(parserPayload);
                });
            } catch (Exception e) {
                log.error("parser data parsing failed, sn:{}", parserPayload.getSn(), e);
            }
        }
        return parserPayload;
    }


    @SuppressWarnings({"java:S1602"})
    @Override
    public ParserPayload encode(ParserPayload parserPayload) {
        ProductDesc productDesc = getProductDesc(parserPayload.getSn());
        if (productDesc == null) {
            return parserPayload;
        }
        parserPayload.setProductDesc(productDesc);
        parserPayload.setDeviceType(DeviceType.valueOf(productDesc.getProductInformation().getDeviceType()));
        if (SUB_DEVICE.equals(parserPayload.getDeviceType())) {
            try {
                // 物模型数据处理
                ThingHandle.encoderData(parserPayload);
                // 子设备解析器数据处理
                ParserHandle.encoderData(parserPayload);
            } catch (Exception e) {
                log.error("parser data parsing failed, sn:{}", parserPayload.getSn(), e);
            }
        }
        return parserPayload;
    }

}
