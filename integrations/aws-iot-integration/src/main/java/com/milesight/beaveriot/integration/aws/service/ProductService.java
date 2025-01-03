package com.milesight.beaveriot.integration.aws.service;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.page.Sorts;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.context.integration.model.EntityBuilder;
import com.milesight.beaveriot.entity.po.EntityPO;
import com.milesight.beaveriot.entity.repository.EntityRepository;
import com.milesight.beaveriot.integration.aws.constant.AwsIntegrationConstants;
import com.milesight.beaveriot.integration.aws.model.ProductResponseData;
import com.milesight.beaveriot.integration.aws.model.SearchProductRequest;
import com.milesight.beaveriot.integration.aws.model.parser.ProductRequest;
import com.milesight.beaveriot.parser.cache.ProductCache;
import com.milesight.beaveriot.parser.model.ProductDesc;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.milesight.beaveriot.parser.constants.CommonConstants.SN_ADDITIONAL_BITS;
import static com.milesight.beaveriot.parser.constants.CommonConstants.SN_IDENTIFICATION;

@Service
@Slf4j
public class ProductService {

    @Lazy
    @Autowired
    private EntityRepository entityRepository;

    @Lazy
    @Autowired
    private EntityServiceProvider entityServiceProvider;

    private static final String PRODUCT = "products";

    private static final String MODEL = "model";


    public Boolean createProduct(ProductRequest productRequest) {
        try {
            // 示例 JSON 字符串，其中包含转义字符
            String jsonString = productRequest.getName();
            // 获取产品信息
            ProductDesc productPayload = saveProductDesc(jsonString);
            if (productPayload == null) {
                return false;
            }
            String model = productPayload.getProductModel();
            val productInformation = productPayload.getProductInformation();
            val snIdentification = productInformation.getSnIdentification();
            val snAdditionalBits = productInformation.getSnAdditionalBits();
            Map<String, Object> attributesMap = new HashMap<>();
            attributesMap.put(MODEL, model);
            attributesMap.put(SN_IDENTIFICATION, snIdentification);
            attributesMap.put(SN_ADDITIONAL_BITS, snAdditionalBits);
            val integrationId = AwsIntegrationConstants.getKey(PRODUCT);
            val productEntity = new EntityBuilder(integrationId)
                    .identifier(model)
                    .valueType(EntityValueType.BINARY)
                    .attributes(attributesMap)
                    .property(model, AccessMod.RW)
                    .build();
            entityServiceProvider.save(productEntity);
            String key = getSnKey(snIdentification, snAdditionalBits);
            // 添加缓存
            ProductCache.addToCache(key, productPayload);
        } catch (Exception e) {
            log.error("Received webhook data error: {}", e.getMessage());
        }
        return true;
    }

    private static String getSnKey(String snIdentification, String snAdditionalBits) {
        String key = snIdentification;
        if (CharSequenceUtil.isNotEmpty(snAdditionalBits)) {
            key = key + ":" + snAdditionalBits;
        }
        return key;
    }

    private static ProductDesc saveProductDesc(String jsonString) throws JsonProcessingException {
        // 创建一个ObjectMapper实例
        ObjectMapper objectMapper = new ObjectMapper();
        // 将 JSON 字符串解析为 JsonNode
        JsonNode jsonNode = objectMapper.readTree(jsonString);
        objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        ProductDesc productPayload = objectMapper.readValue(jsonNode.asText(), ProductDesc.class);
        // productPayload对象转成json文件，并存resource下
        try {
            // 获取当前工作目录
            String currentDir = System.getProperty("user.dir");
            File resourceDir = Paths.get(currentDir).toFile();
            // 创建 product 文件夹
            File productDir = new File(resourceDir, PRODUCT);
            if (!productDir.exists()) {
                boolean created = productDir.mkdirs();
                if (!created) {
                    throw new IOException("Failed to create directory: " + productDir.getAbsolutePath());
                }
            }
            // 创建 JSON 文件
            File jsonFile = new File(productDir, String.format("%s.json", productPayload.getProductModel()));
            // 将对象写入 JSON 文件
            objectMapper.writeValue(jsonFile, productPayload);
            log.info("JSON file created: " + jsonFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("Error creating JSON file: {}", e.getMessage());
        }
        return productPayload;
    }

    public Page<ProductResponseData> searchProduct(SearchProductRequest searchProductRequest) {
        if (searchProductRequest.getSort().getOrders().isEmpty()) {
            searchProductRequest.sort(new Sorts().desc(EntityPO.Fields.createdAt));
        }
        val integrationId = AwsIntegrationConstants.getKey(PRODUCT);
        return entityRepository
                .findAll(f -> f.eq(EntityPO.Fields.attachTargetId, integrationId), searchProductRequest.toPageable())
                .map(this::convertPOToResponseData);
    }

    private ProductResponseData convertPOToResponseData(EntityPO entityPO) {
        ProductResponseData deviceResponseData = new ProductResponseData();
        deviceResponseData.setId(entityPO.getId().toString());
        deviceResponseData.setKey(entityPO.getKey());
        deviceResponseData.setName(entityPO.getName());
        deviceResponseData.setIntegration(entityPO.getAttachTargetId());
        deviceResponseData.setAdditionalData(entityPO.getValueAttribute());
        deviceResponseData.setCreatedAt(entityPO.getCreatedAt());
        deviceResponseData.setUpdatedAt(entityPO.getUpdatedAt());

        deviceResponseData.setDeletable(true);
        return deviceResponseData;
    }

    public void batchDeleteProducts(List<String> productIdList) {
        if (productIdList.isEmpty()) {
            throw new ServiceException(ErrorCode.METHOD_NOT_ALLOWED, "productIdList cannot be empty");
        }
        AtomicReference<Boolean> isFileDeleted = new AtomicReference<>(true);
        List<Long> productIds = productIdList.stream().map(Long::parseLong).collect(Collectors.toList());
        val products = entityRepository.findAllById(productIds);
        // 循环删除resource下的product下的model名称的json文件
        products.forEach(product -> {
            val valueAttributeMap = product.getValueAttribute();
            val model = valueAttributeMap.get(MODEL).toString();
            val snIdentification = valueAttributeMap.get(SN_IDENTIFICATION).toString();
            val snAdditionalBits = valueAttributeMap.get(SN_ADDITIONAL_BITS).toString();
            String key = getSnKey(snIdentification, snAdditionalBits);
            try {
                // 获取当前工作目录
                String currentDir = System.getProperty("user.dir");
                val resourceDir = Paths.get(currentDir);
                val productDir = resourceDir.resolve(PRODUCT);
                val jsonFile = productDir.resolve(model + ".json");
                Files.deleteIfExists(jsonFile);
                // 删除缓存
                ProductCache.removeToCache(key);
            } catch (IOException e) {
                log.error("Error deleting JSON file: {}", e.getMessage());
                isFileDeleted.set(false);
            }
        });
        if (Boolean.FALSE.equals(isFileDeleted.get())) {
            throw new ServiceException("Failed to delete product files", "Failed to delete product files");
        }
        entityRepository.deleteAllByIdInBatch(productIds);
    }

    public ProductResponseData findById(String model) {
        val product = entityRepository.findById(Long.parseLong(model)).get();
        return convertPOToResponseData(product);
    }
}
