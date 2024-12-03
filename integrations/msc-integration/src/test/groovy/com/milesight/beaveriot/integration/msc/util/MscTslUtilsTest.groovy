package com.milesight.beaveriot.integration.msc.util

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.milesight.beaveriot.base.utils.JsonUtils
import com.milesight.beaveriot.context.integration.enums.EntityType
import com.milesight.beaveriot.context.integration.enums.EntityValueType
import com.milesight.cloud.sdk.client.model.*
import spock.lang.Specification

class MscTslUtilsTest extends Specification {


    def "given thing spec when calling thingSpecificationToEntities then should return property entities"() {
        given:
        def properties = [
                new TslPropertySpec()
                        .id("data")
                        .name("Data")
                        .accessMode(TslPropertySpec.AccessModeEnum.RW)
                        .dataSpec(new TslDataSpec()
                                .dataType(TslDataSpec.DataTypeEnum.STRUCT)),
                new TslPropertySpec()
                        .id("data.long_value")
                        .name("Long Value")
                        .accessMode(TslPropertySpec.AccessModeEnum.RW)
                        .dataSpec(new TslDataSpec()
                                .parentId("data")
                                .dataType(TslDataSpec.DataTypeEnum.LONG)
                                .fractionDigits(2)
                                .validator(new TslDataValidatorSpec()
                                        .min(BigDecimal.valueOf(9))
                                        .max(BigDecimal.valueOf(23)))),
                new TslPropertySpec()
                        .id("data.enum_value")
                        .name("Enum Value")
                        .accessMode(TslPropertySpec.AccessModeEnum.W)
                        .dataSpec(new TslDataSpec()
                                .parentId("data")
                                .dataType(TslDataSpec.DataTypeEnum.ENUM)
                                .mappings([
                                        new TslKeyValuePair().key("a").value("1"),
                                        new TslKeyValuePair().key("b").value("2"),
                                ])),
                new TslPropertySpec()
                        .id("data.struct_value")
                        .name("Struct Value")
                        .accessMode(TslPropertySpec.AccessModeEnum.R)
                        .dataSpec(new TslDataSpec()
                                .parentId("data")
                                .dataType(TslDataSpec.DataTypeEnum.STRUCT)),
                new TslPropertySpec()
                        .id("data.struct_value.string_value")
                        .name("String Value")
                        .accessMode(TslPropertySpec.AccessModeEnum.R)
                        .dataSpec(new TslDataSpec()
                                .parentId("data.struct_value")
                                .dataType(TslDataSpec.DataTypeEnum.STRING)
                                .validator(new TslDataValidatorSpec()
                                        .minSize(5)
                                        .maxSize(15))),
                new TslPropertySpec()
                        .id("data.array_value")
                        .name("Data Array Value")
                        .accessMode(TslPropertySpec.AccessModeEnum.R)
                        .dataSpec(new TslDataSpec()
                                .parentId("data")
                                .dataType(TslDataSpec.DataTypeEnum.ARRAY)
                                .elementDataType(TslDataSpec.ElementDataTypeEnum.STRUCT)
                                .validator(new TslDataValidatorSpec()
                                        .minSize(1)
                                        .maxSize(2))),
                new TslPropertySpec()
                        .id("data.array_value._item")
                        .name("Data Array Element")
                        .accessMode(TslPropertySpec.AccessModeEnum.R)
                        .dataSpec(new TslDataSpec()
                                .parentId("data.array_value")
                                .dataType(TslDataSpec.DataTypeEnum.STRUCT)),
                new TslPropertySpec()
                        .id("data.array_value._item.int_value")
                        .name("Data Array Element Value")
                        .accessMode(TslPropertySpec.AccessModeEnum.R)
                        .dataSpec(new TslDataSpec()
                                .parentId("data.array_value._item")
                                .dataType(TslDataSpec.DataTypeEnum.INT)),
                new TslPropertySpec()
                        .id("array_value")
                        .name("Array Value")
                        .accessMode(TslPropertySpec.AccessModeEnum.R)
                        .dataSpec(new TslDataSpec()
                                .dataType(TslDataSpec.DataTypeEnum.ARRAY)
                                .elementDataType(TslDataSpec.ElementDataTypeEnum.STRUCT)),
                new TslPropertySpec()
                        .id("array_value._item")
                        .name("Array Element")
                        .accessMode(TslPropertySpec.AccessModeEnum.R)
                        .dataSpec(new TslDataSpec()
                                .parentId("array_value")
                                .dataType(TslDataSpec.DataTypeEnum.STRUCT)),
                new TslPropertySpec()
                        .id("array_value._item.int_value1")
                        .name("Array Element Value 1")
                        .accessMode(TslPropertySpec.AccessModeEnum.R)
                        .dataSpec(new TslDataSpec()
                                .parentId("array_value._item")
                                .dataType(TslDataSpec.DataTypeEnum.INT)),
                new TslPropertySpec()
                        .id("array_value._item.int_value2")
                        .name("Array Element Value 2")
                        .accessMode(TslPropertySpec.AccessModeEnum.R)
                        .dataSpec(new TslDataSpec()
                                .parentId("array_value._item")
                                .dataType(TslDataSpec.DataTypeEnum.INT)),
        ]
        def thingSpec = new ThingSpec()
                .properties(properties)

        when:
        def result = MscTslUtils.thingSpecificationToEntities("", "", thingSpec)

        then:
        result.size() == 5
        def dataEntity = result.get(4)
        dataEntity.identifier == "data"
        dataEntity.name == "Data"
        dataEntity.type == EntityType.PROPERTY
        dataEntity.valueType == EntityValueType.OBJECT

        def children = dataEntity.children
        children.size() == 8

        def arrayValueFirstElementEntity = children.get(0)
        arrayValueFirstElementEntity.identifier == "array_value[0]"
        arrayValueFirstElementEntity.type == EntityType.PROPERTY
        arrayValueFirstElementEntity.valueType == EntityValueType.OBJECT
        arrayValueFirstElementEntity.children.size() == 0

        def arrayValueSecondElementIntValueEntity = children.get(3)
        arrayValueSecondElementIntValueEntity.identifier == "array_value[1]@int_value"
        arrayValueSecondElementIntValueEntity.type == EntityType.PROPERTY
        arrayValueSecondElementIntValueEntity.valueType == EntityValueType.LONG

        def enumValueEntity = children.get(4)
        enumValueEntity.identifier == "enum_value"
        enumValueEntity.type == EntityType.PROPERTY
        enumValueEntity.valueType == EntityValueType.STRING
        enumValueEntity.attributes["enum"]["a"] == "1"
        enumValueEntity.attributes["enum"]["b"] == "2"

        def longValueEntity = children.get(5)
        longValueEntity.identifier == "long_value"
        longValueEntity.type == EntityType.PROPERTY
        longValueEntity.valueType == EntityValueType.LONG
        longValueEntity.attributes["fraction_digits"] == 2
        longValueEntity.attributes["min"] == 9
        longValueEntity.attributes["max"] == 23

        def structValueEntity = children.get(6)
        structValueEntity.identifier == "struct_value"
        structValueEntity.type == EntityType.PROPERTY
        structValueEntity.valueType == EntityValueType.OBJECT
        structValueEntity.children.size() == 0
        structValueEntity.attributes.isEmpty()

        def stringValueEntity = children.get(7)
        stringValueEntity.identifier == "struct_value@string_value"
        stringValueEntity.type == EntityType.PROPERTY
        stringValueEntity.valueType == EntityValueType.STRING
        stringValueEntity.attributes["min_length"] == 5
        stringValueEntity.attributes["max_length"] == 15

        def arrayEntity = result.get(0)
        arrayEntity.identifier == "array_value[0]"
        arrayEntity.name == "Array Element"
        arrayEntity.type == EntityType.PROPERTY
        arrayEntity.valueType == EntityValueType.OBJECT

        def arrayChildren = arrayEntity.children
        arrayChildren.size() == 2
    }

    def "given ObjectNode embed ArrayNode when convertJsonNodeToExchangePayload then get valid ExchangePayload"() {
        given:
        def jsonNode = JsonUtils.toJsonNode([
                "string" : "value",
                "struct": ["struct": ["string": "value"]],
                "array" : [1, [2], ["int": 3], [null, ["struct": ["string": "value"]]]],
        ])

        when:
        def result = MscTslUtils.convertJsonNodeToExchangePayload("prefix", jsonNode)

        then:
        result["prefix.string"].textValue() == "value"
        result["prefix.struct.struct@string"].textValue() == "value"
        result["prefix.array[0]"].intValue() == 1
        result["prefix.array[1][0]"].intValue() == 2
        result["prefix.array[2].int"].intValue() == 3
        result["prefix.array[3][1].struct@string"].textValue() == "value"
    }

    def "given ExchangePayload when convertExchangePayloadToGroupedJsonNode then get grouped JsonNode"() {
        given:
        def exchangePayload = [:] as Map<String, Object>
        exchangePayload["prefix.string"] = "value"
        exchangePayload["prefix.struct.struct@string"] = "value"
        exchangePayload["prefix.array[0]"] = 1
        exchangePayload["prefix.array[1][0]"] = 2
        exchangePayload["prefix.array[2].int"] = 3
        exchangePayload["prefix.array[3][1].struct@string"] = "value"

        when:
        def result = MscTslUtils.convertExchangePayloadToGroupedJsonNode(JsonUtils.objectMapper, "prefix", exchangePayload)

        then:
        result["string"] instanceof TextNode
        result["string"].textValue() == "value"
        result["struct"] instanceof ObjectNode
        result["struct"]["struct"]["string"].textValue() == "value"
        result["array"] instanceof ArrayNode
        result["array"].size() == 4
        result["array"][0].intValue() == 1
        result["array"][1] instanceof ArrayNode
        result["array"][1].size() == 1
        result["array"][1][0].intValue() == 2
        result["array"][2] instanceof ObjectNode
        result["array"][2].get("int").intValue() == 3
        result["array"][3] instanceof ArrayNode
        result["array"][3].size() == 2
        result["array"][3][1] instanceof ObjectNode
        result["array"][3][1]["struct"]["string"].textValue() == "value"
    }

}
