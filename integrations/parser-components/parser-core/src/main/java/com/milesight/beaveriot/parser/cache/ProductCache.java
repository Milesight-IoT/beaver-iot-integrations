package com.milesight.beaveriot.parser.cache;

import cn.hutool.core.text.CharSequenceUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.milesight.beaveriot.parser.model.ParserSpec;
import com.milesight.beaveriot.parser.model.ProductDesc;
import com.milesight.beaveriot.parser.model.ProductInformation;
import com.milesight.beaveriot.parser.util.ClassResourceTestUtil;
import com.sun.tools.javac.Main;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author lzy
 */
@Slf4j
@Component
public class ProductCache {

    private ProductCache() {
    }

    /**
     * 产品缓存
     */
    private static final Map<String, ProductDesc> CACHE_PRODUCT_IN_MAP = new HashMap<>();

    public static void addToCache(String key, ProductDesc productDesc) {
        CACHE_PRODUCT_IN_MAP.put(key, productDesc);
    }

    public static void removeToCache(String key) {
        CACHE_PRODUCT_IN_MAP.remove(key);
    }

    public static ProductDesc getFromCache(String key) {
        return CACHE_PRODUCT_IN_MAP.get(key);
    }

    private static final String PRODUCT = "products";

    static {
        // 加载产品描述文件
        productDescLoader();
    }

    /**
     * 加载产品描述文件
     */
    public static void productDescLoader() {
        try {
            // 创建一个ObjectMapper实例
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true);
            // 获取 resources 目录下的路径
            // 获取当前工作目录
            String currentDir = System.getProperty("user.dir");
            val resourceDir = Paths.get(currentDir);
            val productDir = resourceDir.resolve(PRODUCT);

            // 获取productDir所有文件
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(productDir)) {
                for (Path path : directoryStream) {
                    String fileName = path.getFileName().toString();
                    if (fileName.endsWith(".json")) {
                        // 读取文件内容并转换为 JSON 字符串
                        String jsonString = new String(Files.readAllBytes(path));
                        // 字符串转jsonnode
                        val jsonNode = objectMapper.readTree(jsonString);
                        val productInformation = objectMapper.readValue(jsonNode.get("productInformation").toString(), ProductInformation.class);
                        val thingSpecifications = objectMapper.readValue(jsonNode.get("thingSpecifications").toString(), List.class);
                        val parser = jsonNode.get("parser") == null ? null : objectMapper.readValue(jsonNode.get("parser").toString(), ParserSpec.class);
                        ProductDesc productPayload = ProductDesc.builder()
                                .productModel(jsonNode.get("productModel").asText())
                                .manifestVersion(jsonNode.get("manifestVersion").asText())
                                .productInformation(productInformation)
                                .thingSpecifications(thingSpecifications)
                                .parser(parser)
                                .build();
                        String snIdentification = productInformation.getSnIdentification();
                        String snAdditionalBits = productInformation.getSnAdditionalBits();
                        String key = getSnKey(snIdentification, snAdditionalBits);
                        CACHE_PRODUCT_IN_MAP.put(key, productPayload);
                    }
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private static String getSnKey(String snIdentification, String snAdditionalBits) {
        String key = snIdentification;
        if (CharSequenceUtil.isNotEmpty(snAdditionalBits)) {
            key = key + ":" + snAdditionalBits;
        }
        return key;
    }
}
