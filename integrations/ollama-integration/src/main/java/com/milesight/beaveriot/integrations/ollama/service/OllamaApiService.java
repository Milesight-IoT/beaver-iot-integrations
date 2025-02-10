package com.milesight.beaveriot.integrations.ollama.service;

import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.integration.entity.annotation.Attribute;
import com.milesight.beaveriot.context.integration.model.AttributeBuilder;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.context.integration.wrapper.AnnotatedEntityWrapper;
import com.milesight.beaveriot.eventbus.annotations.EventSubscribe;
import com.milesight.beaveriot.eventbus.api.Event;
import com.milesight.beaveriot.eventbus.api.EventResponse;
import com.milesight.beaveriot.integrations.ollama.api.OllamaClient;
import com.milesight.beaveriot.integrations.ollama.api.config.Config;
import com.milesight.beaveriot.integrations.ollama.api.model.GenerateCompletionRequest;
import com.milesight.beaveriot.integrations.ollama.api.model.GenerateCompletionResponse;
import com.milesight.beaveriot.integrations.ollama.api.model.TagsResponse;
import com.milesight.beaveriot.integrations.ollama.constant.OllamaIntegrationConstants;
import com.milesight.beaveriot.integrations.ollama.entity.OllamaConnectionPropertiesEntities;
import com.milesight.beaveriot.integrations.ollama.entity.OllamaServiceEntities;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;


@Slf4j
@Component
public class OllamaApiService {


    @Autowired
    private EntityValueServiceProvider entityValueServiceProvider;
    @Autowired
    private EntityServiceProvider entityServiceProvider;

    private OllamaClient ollamaClient;

    private static final String MODEL_ATTRIBUTE_NAME = OllamaIntegrationConstants.INTEGRATION_IDENTIFIER + ".integration.generate_completion.model";

    public void init() {
        try {
            OllamaConnectionPropertiesEntities.OllamaProperties ollamaProperties = entityValueServiceProvider.findValuesByKey(
                    OllamaConnectionPropertiesEntities.getKey(OllamaConnectionPropertiesEntities.Fields.ollamaProperties), OllamaConnectionPropertiesEntities.OllamaProperties.class);
            if (!ollamaProperties.isEmpty()) {
                initConnection(ollamaProperties);
                initModels();
            }
        } catch (Exception e) {
            log.error("Error occurs while initializing connection", e);
            AnnotatedEntityWrapper<OllamaConnectionPropertiesEntities> wrapper = new AnnotatedEntityWrapper<>();
            wrapper.saveValues(
                    Map.of(OllamaConnectionPropertiesEntities::getApiStatus, Boolean.FALSE, OllamaConnectionPropertiesEntities::getModels, "")
            ).publishSync();
            saveModelAttributes(new LinkedHashMap<>());
        }
    }

    @EventSubscribe(payloadKeyExpression = OllamaIntegrationConstants.INTEGRATION_IDENTIFIER + ".integration.ollama_properties.*")
    public void onOllamaPropertiesUpdate(Event<OllamaConnectionPropertiesEntities.OllamaProperties> event) {
        if (isConfigChanged(event)) {
            OllamaConnectionPropertiesEntities.OllamaProperties ollamaProperties = event.getPayload();
            initConnection(ollamaProperties);
            initModels();
        }
    }


    private boolean isConfigChanged(Event<OllamaConnectionPropertiesEntities.OllamaProperties> event) {
        // check if required fields are set
        OllamaConnectionPropertiesEntities.OllamaProperties ollamaProperties = event.getPayload();
        if (ollamaProperties.getBaseUrl() == null) {
            return false;
        }
        if (ollamaClient == null || ollamaClient.getConfig() == null) {
            return true;
        }
        return !StringUtils.equals(ollamaClient.getConfig().getBaseUrl(), ollamaProperties.getBaseUrl());
    }

    @SneakyThrows
    @EventSubscribe(payloadKeyExpression = OllamaIntegrationConstants.INTEGRATION_IDENTIFIER + ".integration.test_connection")
    public void testConnection(Event<OllamaServiceEntities> event) {
        initModels();
    }

    @SneakyThrows
    @EventSubscribe(payloadKeyExpression = OllamaIntegrationConstants.INTEGRATION_IDENTIFIER + ".integration.generate_completion.*")
    public EventResponse onGenerateCompletion(Event<OllamaServiceEntities.GenerateCompletion> event) {
        OllamaServiceEntities.GenerateCompletion payload = event.getPayload();
        GenerateCompletionRequest request = new GenerateCompletionRequest().converterPayload(payload);
        GenerateCompletionResponse generateCompletionResponse = ollamaClient.postGenerateCompletion(request);
        return getEventResponse(generateCompletionResponse);
    }

    private static EventResponse getEventResponse(GenerateCompletionResponse generateCompletionResponse) {
        Map<String, Object> response = JsonUtils.toMap(generateCompletionResponse);
        EventResponse eventResponse = EventResponse.empty();
        for (Map.Entry<String, Object> entry : response.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            eventResponse.put(key, value);
        }
        return eventResponse;
    }

    private void initConnection(OllamaConnectionPropertiesEntities.OllamaProperties ollamaProperties) {
        Config config = Config.builder()
                .baseUrl(ollamaProperties.getBaseUrl())
                .build();
        ollamaClient = OllamaClient.builder()
                .config(config)
                .build();
    }

    private void initModels() {
        String modelsAsString = "";
        Map<String, String> modelsEnum = new LinkedHashMap<>();
        try {
            if (testConnection()) {
                TagsResponse tags = ollamaClient.getTags();
                List<String> modelsAsList = tags.getModelsAsList();
                if (modelsAsList != null && !modelsAsList.isEmpty()) {
                    modelsAsString = StringUtils.join(modelsAsList, ",");
                    modelsAsList.forEach(model -> modelsEnum.put(model, model));
                }
            }
        } catch (Exception e) {
            log.error("Error occurs while getting models", e);
        }
        AnnotatedEntityWrapper<OllamaConnectionPropertiesEntities> wrapper = new AnnotatedEntityWrapper<>();
        wrapper.saveValue(OllamaConnectionPropertiesEntities::getModels, modelsAsString).publishSync();
        saveModelAttributes(modelsEnum);
    }

    private boolean testConnection() {
        boolean isConnection = Boolean.FALSE;
        try {
            if (ollamaClient != null && ollamaClient.getConfig() != null) {
                isConnection = ollamaClient.test();
            }
        } catch (Exception e) {
            log.error("Error occurs while testing connection", e);
        }
        AnnotatedEntityWrapper<OllamaConnectionPropertiesEntities> wrapper = new AnnotatedEntityWrapper<>();
        wrapper.saveValue(OllamaConnectionPropertiesEntities::getApiStatus, isConnection).publishSync();
        return isConnection;
    }

    private void saveModelAttributes(Map<String, String> modelsEnum) {
        Entity entity = entityServiceProvider.findByKey(MODEL_ATTRIBUTE_NAME);
        Map<String, Object> attributes = entity.getAttributes();
        if (attributes == null){
            attributes = new LinkedHashMap<>();
        }
        attributes.put(AttributeBuilder.ATTRIBUTE_ENUM, modelsEnum);
        entity.setAttributes(attributes);
        entityServiceProvider.save(entity);
    }

}
