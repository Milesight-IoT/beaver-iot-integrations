package com.milesight.beaveriot.integrations.aiinference.service;

import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.integration.enums.AttachTargetType;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.context.integration.wrapper.AnnotatedEntityWrapper;
import com.milesight.beaveriot.eventbus.annotations.EventSubscribe;
import com.milesight.beaveriot.eventbus.api.Event;
import com.milesight.beaveriot.eventbus.api.EventResponse;
import com.milesight.beaveriot.integrations.aiinference.api.client.AiInferenceClient;
import com.milesight.beaveriot.integrations.aiinference.api.config.Config;
import com.milesight.beaveriot.integrations.aiinference.api.enums.ServerErrorCode;
import com.milesight.beaveriot.integrations.aiinference.api.model.request.CamThinkModelInferRequest;
import com.milesight.beaveriot.integrations.aiinference.api.model.response.CamThinkModelDetailResponse;
import com.milesight.beaveriot.integrations.aiinference.api.model.response.CamThinkModelInferResponse;
import com.milesight.beaveriot.integrations.aiinference.api.model.response.CamThinkModelListResponse;
import com.milesight.beaveriot.integrations.aiinference.model.response.ModelInferResponse;
import com.milesight.beaveriot.integrations.aiinference.constant.Constants;
import com.milesight.beaveriot.integrations.aiinference.entity.AiInferenceConnectionPropertiesEntities;
import com.milesight.beaveriot.integrations.aiinference.entity.AiInferenceServiceEntities;
import com.milesight.beaveriot.integrations.aiinference.entity.ModelServiceEntityTemplate;
import com.milesight.beaveriot.integrations.aiinference.entity.ModelServiceInputEntityTemplate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * author: Luxb
 * create: 2025/5/30 16:26
 **/
@Slf4j
@Service
public class AiInferenceService {
    private final EntityServiceProvider entityServiceProvider;
    private final EntityValueServiceProvider entityValueServiceProvider;
    private AiInferenceClient aiInferenceClient;

    public AiInferenceService(EntityServiceProvider entityServiceProvider, EntityValueServiceProvider entityValueServiceProvider) {
        this.entityServiceProvider = entityServiceProvider;
        this.entityValueServiceProvider = entityValueServiceProvider;
    }

    public void init() {
        AnnotatedEntityWrapper<AiInferenceConnectionPropertiesEntities> wrapper = new AnnotatedEntityWrapper<>();
        try {
            AiInferenceConnectionPropertiesEntities.AiInferenceProperties aiInferenceProperties = entityValueServiceProvider.findValuesByKey(
                    AiInferenceConnectionPropertiesEntities.getKey(AiInferenceConnectionPropertiesEntities.Fields.aiInferenceProperties), AiInferenceConnectionPropertiesEntities.AiInferenceProperties.class);
            if (!aiInferenceProperties.isEmpty()) {
                initConnection(aiInferenceProperties);
                initModels();
            }
        } catch (Exception e) {
            log.error("Error occurs while initializing connection", e);
            wrapper.saveValue(AiInferenceConnectionPropertiesEntities::getApiStatus, false).publishSync();
        }
    }

    @EventSubscribe(payloadKeyExpression = Constants.INTEGRATION_ID + ".integration.refresh_models")
    public void refreshModels(Event<AiInferenceServiceEntities.RefreshModels> event) {
        initModels();
    }

    @EventSubscribe(payloadKeyExpression = Constants.INTEGRATION_ID + ".integration.model_*")
    public EventResponse infer(Event<AiInferenceServiceEntities.ModelInput> event) {
        AiInferenceServiceEntities.ModelInput modelInput = event.getPayload();
        CamThinkModelInferResponse camThinkModelInferResponse = modelInfer(modelInput);
        return getEventResponse(new ModelInferResponse(camThinkModelInferResponse));
    }

    private static EventResponse getEventResponse(ModelInferResponse modelInferResponse) {
        Map<String, Object> response = JsonUtils.toMap(modelInferResponse);
        EventResponse eventResponse = EventResponse.empty();
        for (Map.Entry<String, Object> entry : response.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            eventResponse.put(key, value);
        }
        return eventResponse;
    }

    private CamThinkModelInferResponse modelInfer(AiInferenceServiceEntities.ModelInput modelInput) {
        String modelId = null;
        CamThinkModelInferRequest camThinkModelInferRequest = new CamThinkModelInferRequest();
        Map<String, Object> inputs = camThinkModelInferRequest.getInputs();
        for (String key : modelInput.keySet()) {
            Object value = modelInput.get(key);
            if (modelId == null) {
                modelId = ModelServiceEntityTemplate.getModelIdFromKey(key);
            }
            String modelInputName = ModelServiceInputEntityTemplate.getModelInputNameFromKey(key);
            Entity modelInputEntity = entityServiceProvider.findByKey(key);
            if (EntityValueType.LONG.equals(modelInputEntity.getValueType())) {
                inputs.put(modelInputName, Long.parseLong(value.toString()));
            } else if (EntityValueType.BOOLEAN.equals(modelInputEntity.getValueType())) {
                inputs.put(modelInputName, Boolean.parseBoolean(value.toString()));
            } else if (EntityValueType.DOUBLE.equals(modelInputEntity.getValueType())) {
                inputs.put(modelInputName, Double.parseDouble(value.toString()));
            } else {
                inputs.put(modelInputName, value.toString());
            }
        }
        return aiInferenceClient.modelInfer(modelId, camThinkModelInferRequest);
    }

    @EventSubscribe(payloadKeyExpression = Constants.INTEGRATION_ID + ".integration.ai_inference_properties.*")
    public void onAiInferencePropertiesUpdate(Event<AiInferenceConnectionPropertiesEntities.AiInferenceProperties> event) {
        if (isConfigChanged(event)) {
            AiInferenceConnectionPropertiesEntities.AiInferenceProperties aiInferenceProperties = event.getPayload();
            initConnection(aiInferenceProperties);
            initModels();
        }
    }

    private boolean isConfigChanged(Event<AiInferenceConnectionPropertiesEntities.AiInferenceProperties> event) {
        // check if required fields are set
        AiInferenceConnectionPropertiesEntities.AiInferenceProperties aiInferenceProperties = event.getPayload();
        if (aiInferenceProperties.getBaseUrl() == null) {
            return false;
        }
        if (aiInferenceProperties.getToken() == null) {
            return false;
        }
        if (aiInferenceClient == null || aiInferenceClient.getConfig() == null) {
            return true;
        }
        AnnotatedEntityWrapper<AiInferenceConnectionPropertiesEntities> wrapper = new AnnotatedEntityWrapper<>();
        boolean apiStatus = (Boolean) wrapper.getValue(AiInferenceConnectionPropertiesEntities::getApiStatus).orElse(false);
        return !apiStatus ||
                !StringUtils.equals(aiInferenceClient.getConfig().getBaseUrl(), aiInferenceProperties.getBaseUrl()) ||
                !StringUtils.equals(aiInferenceClient.getConfig().getToken(), aiInferenceProperties.getToken());
    }

    private boolean testConnection() {
        boolean isConnection = false;
        try {
            if (aiInferenceClient != null && aiInferenceClient.getConfig() != null) {
                isConnection = aiInferenceClient.testConnection();
            }
        } catch (Exception e) {
            log.error("Error occurs while testing connection", e);
        }
        AnnotatedEntityWrapper<AiInferenceConnectionPropertiesEntities> wrapper = new AnnotatedEntityWrapper<>();
        wrapper.saveValue(AiInferenceConnectionPropertiesEntities::getApiStatus, isConnection).publishSync();
        if (!isConnection) {
            throw ServiceException.with(ServerErrorCode.SERVER_NOT_REACHABLE.getErrorCode(), ServerErrorCode.SERVER_NOT_REACHABLE.getErrorMessage()).build();
        }
        return isConnection;
    }

    private void initModels() {
        if (testConnection()) {
            CamThinkModelListResponse camThinkModelListResponse = aiInferenceClient.getModels();
            if (camThinkModelListResponse == null) {
                return;
            }

            if (CollectionUtils.isEmpty(camThinkModelListResponse.getData())) {
                return;
            }

            Set<String> newModelKeys = new HashSet<>();
            for (CamThinkModelListResponse.ModelData modelData : camThinkModelListResponse.getData()) {
                String modelKey = ModelServiceEntityTemplate.getModelKey(modelData.getModelId());
                newModelKeys.add(modelKey);
            }

            Set<String> toDeleteModelKeys = new HashSet<>();
            List<Entity> existEntities = entityServiceProvider.findByTargetId(AttachTargetType.INTEGRATION, Constants.INTEGRATION_ID);
            if (!CollectionUtils.isEmpty(existEntities)) {
                existEntities.stream().filter(
                        existEntity -> existEntity.getKey().startsWith(ModelServiceEntityTemplate.getModelPrefixKey()) &&
                                existEntity.getParentKey() == null &&
                                !newModelKeys.contains(existEntity.getKey())
                ).forEach(existEntity -> toDeleteModelKeys.add(existEntity.getKey()));
            }

            if (!toDeleteModelKeys.isEmpty()) {
                toDeleteModelKeys.forEach(entityServiceProvider::deleteByKey);
            }

            for (CamThinkModelListResponse.ModelData modelData : camThinkModelListResponse.getData()) {
                ModelServiceEntityTemplate modelServiceEntityTemplate = ModelServiceEntityTemplate.builder()
                        .modelId(modelData.getModelId())
                        .name(modelData.getName())
                        .version(modelData.getVersion())
                        .description(modelData.getDescription())
                        .engineType(modelData.getEngineType())
                        .build();
                Entity modelServiceEntity = modelServiceEntityTemplate.toEntity();
                entityServiceProvider.save(modelServiceEntity);
            }
        }
    }

    public CamThinkModelDetailResponse fetchModelDetail(String modelId) {
        CamThinkModelDetailResponse camThinkModelDetailResponse = null;
        if (testConnection()) {
            camThinkModelDetailResponse = aiInferenceClient.getModelDetail(modelId);
            if (camThinkModelDetailResponse == null) {
                return null;
            }
            if (camThinkModelDetailResponse.getData() == null) {
                return null;
            }
            if (CollectionUtils.isEmpty(camThinkModelDetailResponse.getData().getInputSchema())) {
                return null;
            }
            if (CollectionUtils.isEmpty(camThinkModelDetailResponse.getData().getOutputSchema())) {
                return null;
            }
            String modelKey = ModelServiceEntityTemplate.getModelKey(modelId);
            Entity modelServiceEntity = entityServiceProvider.findByKey(modelKey);
            String modelServiceIdentifier = modelServiceEntity.getIdentifier();
            modelServiceEntity.getChildren().clear();
            for (CamThinkModelDetailResponse.InputSchema inputSchema : camThinkModelDetailResponse.getData().getInputSchema()) {
                ModelServiceInputEntityTemplate modelServiceInputEntityTemplate = ModelServiceInputEntityTemplate.builder()
                        .parentIdentifier(modelServiceIdentifier)
                        .name(inputSchema.getName())
                        .type(inputSchema.getType())
                        .description(inputSchema.getDescription())
                        .required(inputSchema.isRequired())
                        .format(inputSchema.getFormat())
                        .defaultValue(inputSchema.getDefaultValue())
                        .minimum(inputSchema.getMinimum())
                        .maximum(inputSchema.getMaximum())
                        .build();
                Entity modelServiceInputEntity = modelServiceInputEntityTemplate.toEntity();
                modelServiceEntity.getChildren().add(modelServiceInputEntity);
            }
            entityServiceProvider.save(modelServiceEntity);
        }
        return camThinkModelDetailResponse;
    }

    public void initConnection(AiInferenceConnectionPropertiesEntities.AiInferenceProperties aiInferenceProperties) {
        Config config = Config.builder()
                .baseUrl(aiInferenceProperties.getBaseUrl())
                .token(aiInferenceProperties.getToken())
                .build();
        aiInferenceClient = AiInferenceClient.builder()
                .config(config)
                .build();
        aiInferenceClient.init();
    }
}