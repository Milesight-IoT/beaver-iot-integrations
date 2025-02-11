package com.milesight.beaveriot.integration.aws.service;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.integration.aws.model.ProductResponseData;
import com.milesight.beaveriot.integration.aws.model.parser.ParserRequest;
import com.milesight.beaveriot.parser.ParserPlugIn;
import com.milesight.beaveriot.parser.model.ParserPayload;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.milesight.beaveriot.parser.constants.CommonConstants.SN_ADDITIONAL_BITS;
import static com.milesight.beaveriot.parser.constants.CommonConstants.SN_IDENTIFICATION;
import static com.milesight.beaveriot.parser.enums.DeviceType.SUB_DEVICE;

@Service
@Slf4j
public class ParserService {

    @Autowired
    private ProductService productService;

    @Autowired
    private ParserPlugIn parserPlugIn;
    private static final String SN_SUFFIX = "00000000";
    private static final String DECODE = "decode";
    private static final String ENCODE = "encode";


    public String parser(ParserRequest parserRequest) {
        val model = parserRequest.getModel();
        ProductResponseData product = productService.findById(model);
        if (product == null) {
            return null;
        }
        val snIdentification = product.getAdditionalData().get(SN_IDENTIFICATION);
        val snAdditionalBits = product.getAdditionalData().get(SN_ADDITIONAL_BITS);
        ParserPayload parserPayload = ParserPayload.builder()
                .sn(String.format("%s%s%s", snIdentification, SN_SUFFIX, snAdditionalBits))
                .build();
        val type = parserRequest.getType();
        if (DECODE.equals(type)) {
            return decodeHandle(parserRequest, parserPayload);
        } else if (ENCODE.equals(type)) {
            return encodeHandle(parserRequest, parserPayload);
        }
        return null;
    }

    @Nullable
    private String decodeHandle(ParserRequest parserRequest, ParserPayload parserPayload) {
        // 初始化解码数据
        val payload = Base64.encode(HexUtil.decodeHex(parserRequest.getInput()));
        parserPayload.setIpsoData(payload);
        try {
            parserPlugIn.decode(parserPayload);
            List<JsonNode> nodes = new ArrayList<>();
            if (SUB_DEVICE.equals(parserPayload.getDeviceType())) {
                parserPayload.getThingData().forEach(deviceData -> {
                    nodes.add(deviceData.getPayload());
                });
                // 合并 JsonNode
                JsonNode mergedNode = mergeJsonNodes(nodes);
                return JsonUtils.toPrettyJSON(mergedNode.toString());
            } else {
                return parserRequest.getInput();
            }
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    private String encodeHandle(ParserRequest parserRequest, ParserPayload parserPayload) {
        val payload = parserRequest.getInput();
        val objectMapper = new ObjectMapper();
        JsonNode jsonNode = null;
        try {
            jsonNode = objectMapper.readTree(payload);
            parserPayload.setJsonData(jsonNode);
            parserPlugIn.encode(parserPayload);
            if (SUB_DEVICE.equals(parserPayload.getDeviceType())) {
                val ipsoData = parserPayload.getIpsoData();
                // base64 转16进制字符串
                return StrUtil.isEmpty(ipsoData) ? null : HexUtil.encodeHexStr(Base64.decode(ipsoData));
            } else {
                return parserRequest.getInput();
            }
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public static JsonNode mergeJsonNodes(List<JsonNode> nodes) {
        ObjectMapper mapper = new ObjectMapper();
        return mergeNodes(mapper, nodes);
    }

    public static ObjectNode mergeNodes(ObjectMapper mapper, List<JsonNode> nodes) {
        ObjectNode mergedNode = mapper.createObjectNode();

        for (JsonNode node : nodes) {
            node.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                JsonNode value = entry.getValue();

                if (key.contains(".")) {
                    String[] parts = key.split("\\.");
                    mergeNestedObject(mergedNode, parts, value);
                } else {
                    mergedNode.set(key, value);
                }
            });
        }

        return mergedNode;
    }

    private static void mergeNestedObject(ObjectNode parent, String[] parts, JsonNode value) {
        ObjectNode currentNode = parent;

        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            if (!currentNode.has(part) || !currentNode.get(part).isObject()) {
                currentNode.set(part, currentNode.objectNode());
            }
            currentNode = (ObjectNode) currentNode.get(part);
        }

        currentNode.set(parts[parts.length - 1], value);
    }
}
