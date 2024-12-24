package com.milesight.beaveriot.integration.aws.service;

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
import com.milesight.beaveriot.integration.aws.model.parser.PluginResponseData;
import com.milesight.beaveriot.integration.aws.model.parser.SearchPluginRequest;
import com.milesight.beaveriot.integration.aws.model.parser.PluginRequest;
import com.milesight.beaveriot.parser.cache.PluginCache;
import com.milesight.beaveriot.parser.plugin.PlugIn;
import com.sun.jdi.NativeMethodException;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.milesight.beaveriot.parser.constants.CommonConstants.PLUGINS_DIR;
import static com.milesight.beaveriot.parser.constants.CommonConstants.PLUGIN_KEY;

@Service
@Slf4j
public class PluginService {

    @Lazy
    @Autowired
    private EntityRepository entityRepository;

    @Lazy
    @Autowired
    private EntityServiceProvider entityServiceProvider;


    public Boolean createPlugin(PluginRequest pluginRequest) {
        try {
            // 示例 JSON 字符串，其中包含转义字符
            String name = pluginRequest.getName();
            String jsonString = pluginRequest.getText();
            //保存插件
            String url = savePlugin(name, jsonString);
            if (url == null) {
                return false;
            }
            Map<String, Object> attributesMap = new HashMap<>();
            attributesMap.put("name", name);
            attributesMap.put("url", url);
            val integrationId = AwsIntegrationConstants.getKey(PLUGINS_DIR);
            val pluginEntity = new EntityBuilder(integrationId)
                    .identifier(PLUGIN_KEY)
                    .valueType(EntityValueType.BINARY)
                    .attributes(attributesMap)
                    .property(name, AccessMod.RW)
                    .build();
            entityServiceProvider.save(pluginEntity);
            PluginCache.addToCacheUrl(PLUGIN_KEY, url);
        } catch (Exception e) {
            log.error("Received webhook data error: {}", e.getMessage());
        }
        return true;
    }

    private static String savePlugin(String fileName, String fileContent) throws JsonProcessingException {
        // 获取当前工作目录
        String currentDir = System.getProperty("user.dir");
        File resourceDir = Paths.get(currentDir).toFile();
        // 创建 plugin 文件夹
        File pluginDir = new File(resourceDir, PLUGINS_DIR);
        if (!pluginDir.exists()) {
            boolean created = pluginDir.mkdirs();
            if (!created) {
                log.error("Failed to create directory: " + pluginDir.getAbsolutePath());
            }
        } else {
            // 获取 plugin 文件夹下的所有文件，清空
            if (pluginDir.listFiles() != null) {
                for (File file : Objects.requireNonNull(pluginDir.listFiles())) {
                    if (file.isFile()) {
                        file.delete();
                    }
                }
            }
        }
        // 将文件内容写入到文件
        File file = new File(pluginDir, String.format("%s.jar", fileName));
        // 请确保upload-dir目录存在或自行创建
        try (FileOutputStream fos = new FileOutputStream(file)) {
            byte[] data = Base64.getDecoder().decode(fileContent);
            FileCopyUtils.copy(data, fos);
            return file.getAbsolutePath();
        } catch (IOException e) {
            log.error("Error creating JSON file: {}", e.getMessage());
        }
        return null;
    }

    public Page<PluginResponseData> searchPlugin(SearchPluginRequest searchPluginRequest) {
        if (searchPluginRequest.getSort().getOrders().isEmpty()) {
            searchPluginRequest.sort(new Sorts().desc(EntityPO.Fields.createdAt));
        }
        val integrationId = AwsIntegrationConstants.getKey(PLUGINS_DIR);
        return entityRepository
                .findAll(f -> f.eq(EntityPO.Fields.attachTargetId, integrationId), searchPluginRequest.toPageable())
                .map(this::convertPOToResponseData);
    }

    private PluginResponseData convertPOToResponseData(EntityPO entityPO) {
        PluginResponseData deviceResponseData = new PluginResponseData();
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

    public void batchDeletePlugins(List<String> pluginIdList) {
        if (pluginIdList.isEmpty()) {
            throw new ServiceException(ErrorCode.METHOD_NOT_ALLOWED, "pluginIdList cannot be empty");
        }
        AtomicReference<Boolean> isFileDeleted = new AtomicReference<>(true);
        List<Long> pluginIds = pluginIdList.stream().map(Long::parseLong).collect(Collectors.toList());
        val plugins = entityRepository.findAllById(pluginIds);
        // 循环删除resource下的plugin下的model名称的json文件
        plugins.forEach(plugin -> {
            val valueAttributeMap = plugin.getValueAttribute();
            val name = valueAttributeMap.get("name").toString();
             try {
                 // 获取当前工作目录
                 String currentDir = System.getProperty("user.dir");
                 val resourceDir = Paths.get(currentDir);
                 val pluginDir = resourceDir.resolve(PLUGINS_DIR);
                 val jsonFile = pluginDir.resolve(name + ".jar");
                 Files.deleteIfExists(jsonFile);
                 // 删除缓存
                 PluginCache.removeToCache(PLUGIN_KEY);
             } catch (IOException e) {
                 log.error("Error deleting JSON file: {}", e.getMessage());
                 isFileDeleted.set(false);
             }
        });
        if (Boolean.FALSE.equals(isFileDeleted.get())){
            throw new ServiceException("Failed to delete plugin files", "Failed to delete plugin files");
        }
        entityRepository.deleteAllByIdInBatch(pluginIds);
    }

}
