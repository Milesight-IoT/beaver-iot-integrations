package com.milesight.beaveriot.parser.model;

import com.milesight.cloud.sdk.client.model.ThingSpec;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;


/**
 * ProductDesc
 *
 * @author linzy
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductDesc implements Serializable {

    /**
     * 产品
     */
    private String productModel;

    private String manifestVersion;

    private ProductInformation productInformation;

    /**
     * 物模型定义
     */
    private List<ThingSpec> thingSpecifications;

    /**
     * 解析器
     */
    private ParserSpec parser;

    /**
     * i18n
     */
    private List<Multilingual> i18n;

}
