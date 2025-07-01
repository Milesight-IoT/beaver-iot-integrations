package com.milesight.beaveriot.integrations.aiinference.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.integration.enums.AttachTargetType;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.integration.wrapper.AnnotatedEntityWrapper;
import com.milesight.beaveriot.context.support.SpringContext;
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
import com.milesight.beaveriot.integrations.aiinference.constant.Constants;
import com.milesight.beaveriot.integrations.aiinference.entity.AiInferenceConnectionPropertiesEntities;
import com.milesight.beaveriot.integrations.aiinference.entity.AiInferenceServiceEntities;
import com.milesight.beaveriot.integrations.aiinference.entity.ModelServiceEntityTemplate;
import com.milesight.beaveriot.integrations.aiinference.entity.ModelServiceInputEntityTemplate;
import com.milesight.beaveriot.integrations.aiinference.enums.InferStatus;
import com.milesight.beaveriot.integrations.aiinference.model.InferHistory;
import com.milesight.beaveriot.integrations.aiinference.model.response.ModelInferResponse;
import com.milesight.beaveriot.integrations.aiinference.model.response.ModelOutputSchemaResponse;
import com.milesight.beaveriot.integrations.aiinference.support.DataCenter;
import com.milesight.beaveriot.integrations.aiinference.support.EntitySupport;
import com.milesight.beaveriot.integrations.aiinference.support.image.ImageDrawEngine;
import com.milesight.beaveriot.integrations.aiinference.support.image.action.ImageDrawPathAction;
import com.milesight.beaveriot.integrations.aiinference.support.image.action.ImageDrawPolygonAction;
import com.milesight.beaveriot.integrations.aiinference.support.image.action.ImageDrawRectangleAction;
import com.milesight.beaveriot.integrations.aiinference.support.image.config.ImageDrawConfig;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * author: Luxb
 * create: 2025/5/30 16:26
 **/
@Slf4j
@Service
public class AiInferenceService {
    private final DeviceServiceProvider deviceServiceProvider;
    private final EntityServiceProvider entityServiceProvider;
    private final EntityValueServiceProvider entityValueServiceProvider;
    private AiInferenceClient aiInferenceClient;
    private final ThreadPoolExecutor autoInferThreadPoolExecutor;

    public AiInferenceService(DeviceServiceProvider deviceServiceProvider, EntityServiceProvider entityServiceProvider, EntityValueServiceProvider entityValueServiceProvider) {
        this.deviceServiceProvider = deviceServiceProvider;
        this.entityServiceProvider = entityServiceProvider;
        this.entityValueServiceProvider = entityValueServiceProvider;
        this.autoInferThreadPoolExecutor = buildAutoInferThreadPoolExecutor();
    }

    private ThreadPoolExecutor buildAutoInferThreadPoolExecutor() {
        int corePoolSize = Runtime.getRuntime().availableProcessors() * 2;
        int maxPoolSize = corePoolSize * 2;
        long keepAliveTime = 60L;
        TimeUnit unit = TimeUnit.SECONDS;
        BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(1000);
        RejectedExecutionHandler handler = new ThreadPoolExecutor.CallerRunsPolicy();

        return new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepAliveTime,
                unit,
                workQueue,
                handler
        );
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

    public void destroy() {
        closeAutoInferThreadPoolExecutorGracefully();
    }

    private void closeAutoInferThreadPoolExecutorGracefully() {
        autoInferThreadPoolExecutor.shutdown();
        try {
            if (!autoInferThreadPoolExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                autoInferThreadPoolExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            autoInferThreadPoolExecutor.shutdownNow();
        }
    }

    @SuppressWarnings("unused")
    @EventSubscribe(payloadKeyExpression = Constants.INTEGRATION_ID + ".integration.ai_inference_properties.*")
    public void onAiInferencePropertiesUpdate(Event<AiInferenceConnectionPropertiesEntities.AiInferenceProperties> event) {
        if (isConfigChanged(event)) {
            AiInferenceConnectionPropertiesEntities.AiInferenceProperties aiInferenceProperties = event.getPayload();
            initConnection(aiInferenceProperties);
            initModels();
        }
    }

    @SuppressWarnings("unused")
    @EventSubscribe(payloadKeyExpression = Constants.INTEGRATION_ID + ".integration.refresh_models")
    public void refreshModels(Event<AiInferenceServiceEntities.RefreshModels> event) {
        initModels();
    }

    @SuppressWarnings("unused")
    @EventSubscribe(payloadKeyExpression = Constants.INTEGRATION_ID + ".integration.model_*")
    public EventResponse infer(Event<AiInferenceServiceEntities.ModelInput> event) {
        AiInferenceServiceEntities.ModelInput modelInput = event.getPayload();
        CamThinkModelInferResponse camThinkModelInferResponse = modelInfer(modelInput);
        return getEventResponse(new ModelInferResponse(camThinkModelInferResponse));
    }

    @SuppressWarnings("unused")
    @EventSubscribe(payloadKeyExpression = "*.device.*")
    public void detectImageAutoInfer(Event<ExchangePayload> event) {
        ExchangePayload exchangePayload = event.getPayload();
        exchangePayload.forEach((key, value) -> {
            Device device = getDeviceByImageEntityKey(key);
            if (device != null) {
                AiInferenceService service = self();
                autoInferThreadPoolExecutor.execute(() -> service.autoInfer(device, key, value.toString()));
            }
        });
    }

    public AiInferenceService self() {
        return SpringContext.getBean(AiInferenceService.class);
    }

    private Device getDeviceByImageEntityKey(String imageEntityKey) {
        Long deviceId = DataCenter.getDeviceIdByImageEntityKey(imageEntityKey);
        if (deviceId == null) {
            return null;
        }
        Device device = deviceServiceProvider.findById(deviceId);
        if (device == null) {
            DataCenter.removeDeviceFromImageEntityMap(deviceId);
        }
        return device;
    }

    @Timed(value = "ai_inference_auto_infer")
    @Counted(value = "ai_inference_auto_infer")
    public void autoInfer(Device device, String imageEntityKey, String imageEntityValue) {
        try {
            long uplinkAt = System.currentTimeMillis();
            String deviceKey = device.getKey();
            String modelId = (String) entityValueServiceProvider.findValueByKey(EntitySupport.getDeviceEntityKey(deviceKey, Constants.IDENTIFIER_MODEL_ID));
            if (StringUtils.isEmpty(modelId)) {
                return;
            }

            String modelIdentifier = MessageFormat.format(Constants.IDENTIFIER_MODEL_FORMAT, modelId);
            String modelInferInputsKey = EntitySupport.getDeviceEntityChildrenKey(deviceKey, modelIdentifier, Constants.IDENTIFIER_MODEL_INFER_INPUTS);
            String inferInputsValue = (String) entityValueServiceProvider.findValueByKey(modelInferInputsKey);
            if (StringUtils.isEmpty(inferInputsValue)) {
                return;
            }

            CamThinkModelInferRequest camThinkModelInferRequest = new CamThinkModelInferRequest();
            Map<String, Object> inferInputs = JsonUtils.toMap(inferInputsValue);
            inferInputs.put(CamThinkModelInferRequest.INPUT_IMAGE_FIELD, imageEntityValue);
            camThinkModelInferRequest.setInputs(inferInputs);

            CamThinkModelInferResponse camThinkModelInferResponse = null;
            try {
                camThinkModelInferResponse = aiInferenceClient.modelInfer(modelId, camThinkModelInferRequest);
            } catch (Exception e) {
                log.error("modelInfer error deviceId:{}, imageEntityKey:{}, error:", device.getId(), imageEntityKey, e);
            }
            InferStatus inferStatus = InferStatus.OK;
            if (camThinkModelInferResponse == null || !camThinkModelInferResponse.isSuccess()) {
                inferStatus = InferStatus.FAILED;
            }
            long inferAt = System.currentTimeMillis();

            Map<String, String> modelMap = getModelMap();

            ExchangePayload exchangePayload = new ExchangePayload();

            InferHistory inferHistory = new InferHistory();
            inferHistory.setModelName(modelMap.get(modelId));
            inferHistory.setOriginImage(imageEntityValue);
            inferHistory.setInferStatus(inferStatus.getValue());
            inferHistory.setUplinkAt(uplinkAt);
            inferHistory.setInferAt(inferAt);

            String inferHistoryEntityKey = EntitySupport.getDeviceEntityKey(device.getKey(), Constants.IDENTIFIER_INFER_HISTORY);
            if (InferStatus.OK.equals(inferStatus)) {
                String resultImageBase64 = drawResultImage(imageEntityValue, camThinkModelInferResponse);
                inferHistory.setResultImage(resultImageBase64);
                String resultImageEntityKey = EntitySupport.getDeviceEntityChildrenKey(deviceKey, modelIdentifier, Constants.IDENTIFIER_MODEL_RESULT_IMAGE);
                if (entityServiceProvider.findByKey(resultImageEntityKey) != null) {
                    exchangePayload.put(resultImageEntityKey, resultImageBase64);
                }

                if (camThinkModelInferResponse.getData() != null && camThinkModelInferResponse.getData().getOutputs() != null) {
                    String outputsData = JsonUtils.toJSON(camThinkModelInferResponse.getData().getOutputs());
                    inferHistory.setInferOutputsData(outputsData);

                    for (String filed : camThinkModelInferResponse.getData().getOutputs().keySet()) {
                        String value;
                        if (CamThinkModelInferResponse.ModelInferData.FIELD_DATA.equals(filed)) {
                            value = JsonUtils.toJSON(camThinkModelInferResponse.getData().getOutputs().get(filed));
                        } else {
                            value = camThinkModelInferResponse.getData().getOutputs().get(filed).toString();
                        }
                        String outputFiledEntityKey = EntitySupport.getDeviceEntityChildrenKey(deviceKey, modelIdentifier, filed);
                        if (entityServiceProvider.findByKey(outputFiledEntityKey) != null) {
                            exchangePayload.put(outputFiledEntityKey, value);
                        }
                    }
                }
            }

            if (entityServiceProvider.findByKey(inferHistoryEntityKey) != null) {
                exchangePayload.put(inferHistoryEntityKey, JsonUtils.toJSON(inferHistory));
            }

            if (!exchangePayload.isEmpty()) {
                entityValueServiceProvider.saveValuesAndPublishSync(exchangePayload);
            }
        } catch (Exception e) {
            log.error("autoInfer error deviceId:{}, imageEntityKey:{}, error:", device.getId(), imageEntityKey, e);
        }
    }

    public Map<String, String> getModelMap() {
        List<Entity> entities = entityServiceProvider.findByTargetId(AttachTargetType.INTEGRATION, Constants.INTEGRATION_ID);
        return entities.stream().filter(entity -> entity.getIdentifier().startsWith(Constants.IDENTIFIER_MODEL_PREFIX)).collect(Collectors.toMap(
                entity -> {
                    String identifier = entity.getIdentifier();
                    return identifier.substring(Constants.IDENTIFIER_MODEL_PREFIX.length());
                },
                Entity::getName
        ));
    }

    private String drawResultImage(String imageBase64, CamThinkModelInferResponse camThinkModelInferResponse) throws Exception {
        if (camThinkModelInferResponse.getData() == null) {
            return "";
        }

        if (camThinkModelInferResponse.getData().getOutputs() == null) {
            return "";
        }

        if (camThinkModelInferResponse.getData().getOutputs().get(CamThinkModelInferResponse.ModelInferData.FIELD_DATA) == null) {
            return "";
        }
        String dataJson = JsonUtils.toJSON(camThinkModelInferResponse.getData().getOutputs().get(CamThinkModelInferResponse.ModelInferData.FIELD_DATA));
        List<CamThinkModelInferResponse.ModelInferData.OutputData> data = JsonUtils.fromJSON(dataJson, new TypeReference<>() {});
        if(CollectionUtils.isEmpty(data)) {
            return "";
        }

        CamThinkModelInferResponse.ModelInferData.OutputData outputData = data.get(0);
        if (CollectionUtils.isEmpty(outputData.getDetections())) {
            return "";
        }

        ImageDrawEngine engine = new ImageDrawEngine(ImageDrawConfig.getDefault());
        engine.loadImageFromBase64(imageBase64);

        for (CamThinkModelInferResponse.ModelInferData.OutputData.Detection detection : outputData.getDetections()) {
            List<Integer> box = detection.getBox();
            if (!CollectionUtils.isEmpty(box) && box.size() == CamThinkModelInferResponse.BOX_SIZE) {
                String tag = getTag(detection.getCls(), detection.getConf());
                ImageDrawRectangleAction imageDrawRectangleAction = new ImageDrawRectangleAction(
                        box.get(CamThinkModelInferResponse.BOX_X_INDEX),
                        box.get(CamThinkModelInferResponse.BOX_Y_INDEX),
                        box.get(CamThinkModelInferResponse.BOX_WIDTH_INDEX),
                        box.get(CamThinkModelInferResponse.BOX_HEIGHT_INDEX),
                        tag);
                engine.addAction(imageDrawRectangleAction);
            }

            List<List<Integer>> masks = detection.getMasks();
            if (!CollectionUtils.isEmpty(masks)) {
                ImageDrawPolygonAction imageDrawPolygonAction = new ImageDrawPolygonAction();
                for (List<Integer> mask : masks) {
                    if (!CollectionUtils.isEmpty(mask) && mask.size() == CamThinkModelInferResponse.MASK_SIZE) {
                        imageDrawPolygonAction.addPoint(
                                mask.get(CamThinkModelInferResponse.MASK_POINT_X_INDEX), mask.get(CamThinkModelInferResponse.MASK_POINT_Y_INDEX));
                    }
                }
                engine.addAction(imageDrawPolygonAction);
            }

            List<List<Double>> points = detection.getPoints();
            if (!CollectionUtils.isEmpty(points)) {
                List<List<Integer>> skeleton = detection.getSkeleton();
                ImageDrawPathAction imageDrawPathAction = buildImageDrawPathAction(points, skeleton);

                engine.addAction(imageDrawPathAction);
            }
        }

        return engine.draw().outputImageBase64();
    }

    private ImageDrawPathAction buildImageDrawPathAction(List<List<Double>> points, List<List<Integer>> skeleton) {
        ImageDrawPathAction imageDrawPathAction = new ImageDrawPathAction();
        for (List<Double> point : points) {
            if (!CollectionUtils.isEmpty(point) && point.size() == CamThinkModelInferResponse.POINT_SIZE) {
                imageDrawPathAction.addPoint(
                        point.get(CamThinkModelInferResponse.POINT_X_INDEX).intValue(),
                        point.get(CamThinkModelInferResponse.POINT_Y_INDEX).intValue(),
                        point.get(CamThinkModelInferResponse.POINT_ID_INDEX).intValue());
            }
        }

        if (!CollectionUtils.isEmpty(skeleton)) {
            for (List<Integer> line : skeleton) {
                if (!CollectionUtils.isEmpty(line) && line.size() == CamThinkModelInferResponse.LINE_SIZE) {
                    imageDrawPathAction.addLine(line.get(CamThinkModelInferResponse.LINE_START_POINT_ID_INDEX), line.get(CamThinkModelInferResponse.LINE_END_POINT_ID_INDEX));
                }
            }
        }

        return imageDrawPathAction;
    }

    private String getTag(String cls, Double conf) {
        if (StringUtils.isEmpty(cls)) {
            return "";
        }
        if (conf == null) {
            return "";
        }
        return String.format("%s:%.2f", cls, conf);
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
        return true;
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
                String modelKey = ModelServiceEntityTemplate.getModelKey(modelData.getId());
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
                        .modelId(modelData.getId())
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

    public ModelOutputSchemaResponse fetchModelDetail(String modelId) {
        ModelOutputSchemaResponse modelOutputSchemaResponse = new ModelOutputSchemaResponse();
        CamThinkModelDetailResponse camThinkModelDetailResponse;
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

            modelOutputSchemaResponse.setOutputSchema(camThinkModelDetailResponse.getData().getOutputSchema());

            String modelKey = ModelServiceEntityTemplate.getModelKey(modelId);
            Entity modelServiceEntity = entityServiceProvider.findByKey(modelKey);
            String modelServiceIdentifier = modelServiceEntity.getIdentifier();
            modelServiceEntity.getChildren().clear();

            List<Entity> inputEntities = new ArrayList<>();
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
                inputEntities.add(modelServiceInputEntity);
                modelServiceEntity.getChildren().add(modelServiceInputEntity);
            }
            entityServiceProvider.save(modelServiceEntity);
            modelOutputSchemaResponse.setInputEntities(inputEntities);
        }
        return modelOutputSchemaResponse;
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