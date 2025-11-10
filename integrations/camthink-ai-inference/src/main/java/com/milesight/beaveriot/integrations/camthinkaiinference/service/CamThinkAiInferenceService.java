package com.milesight.beaveriot.integrations.camthinkaiinference.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.api.ResourceServiceProvider;
import com.milesight.beaveriot.context.enums.ResourceRefType;
import com.milesight.beaveriot.context.integration.enums.AttachTargetType;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.integration.wrapper.AnnotatedEntityWrapper;
import com.milesight.beaveriot.context.model.ResourceRefDTO;
import com.milesight.beaveriot.context.security.TenantContext;
import com.milesight.beaveriot.context.support.SpringContext;
import com.milesight.beaveriot.eventbus.annotations.EventSubscribe;
import com.milesight.beaveriot.eventbus.api.Event;
import com.milesight.beaveriot.eventbus.api.EventResponse;
import com.milesight.beaveriot.integrations.camthinkaiinference.api.client.CamThinkAiInferenceClient;
import com.milesight.beaveriot.integrations.camthinkaiinference.api.enums.ServerErrorCode;
import com.milesight.beaveriot.integrations.camthinkaiinference.api.model.request.CamThinkModelInferRequest;
import com.milesight.beaveriot.integrations.camthinkaiinference.api.model.response.CamThinkModelDetailResponse;
import com.milesight.beaveriot.integrations.camthinkaiinference.api.model.response.CamThinkModelInferResponse;
import com.milesight.beaveriot.integrations.camthinkaiinference.api.model.response.CamThinkModelListResponse;
import com.milesight.beaveriot.integrations.camthinkaiinference.constant.Constants;
import com.milesight.beaveriot.integrations.camthinkaiinference.entity.*;
import com.milesight.beaveriot.integrations.camthinkaiinference.enums.InferStatus;
import com.milesight.beaveriot.integrations.camthinkaiinference.model.InferHistory;
import com.milesight.beaveriot.integrations.camthinkaiinference.model.response.ModelInferResponse;
import com.milesight.beaveriot.integrations.camthinkaiinference.model.response.ModelOutputSchemaResponse;
import com.milesight.beaveriot.integrations.camthinkaiinference.support.DataCenter;
import com.milesight.beaveriot.integrations.camthinkaiinference.support.EntitySupport;
import com.milesight.beaveriot.integrations.camthinkaiinference.support.ImageSupport;
import com.milesight.beaveriot.integrations.camthinkaiinference.support.image.ImageDrawEngine;
import com.milesight.beaveriot.integrations.camthinkaiinference.support.image.action.ImageDrawPathAction;
import com.milesight.beaveriot.integrations.camthinkaiinference.support.image.action.ImageDrawPolygonAction;
import com.milesight.beaveriot.integrations.camthinkaiinference.support.image.action.ImageDrawRectangleAction;
import com.milesight.beaveriot.integrations.camthinkaiinference.support.image.config.ImageDrawConfig;
import com.milesight.beaveriot.scheduler.integration.IntegrationScheduled;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.util.Pair;
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
public class CamThinkAiInferenceService {
    private final DeviceServiceProvider deviceServiceProvider;
    private final EntityServiceProvider entityServiceProvider;
    private final EntityValueServiceProvider entityValueServiceProvider;
    private final ResourceServiceProvider resourceServiceProvider;
    private final CamThinkAiInferenceClient camThinkAiInferenceClient;
    private final ThreadPoolExecutor autoInferThreadPoolExecutor;

    public CamThinkAiInferenceService(DeviceServiceProvider deviceServiceProvider, EntityServiceProvider entityServiceProvider, EntityValueServiceProvider entityValueServiceProvider, ResourceServiceProvider resourceServiceProvider, CamThinkAiInferenceClient camThinkAiInferenceClient) {
        this.deviceServiceProvider = deviceServiceProvider;
        this.entityServiceProvider = entityServiceProvider;
        this.entityValueServiceProvider = entityValueServiceProvider;
        this.resourceServiceProvider = resourceServiceProvider;
        this.camThinkAiInferenceClient = camThinkAiInferenceClient;
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
        AnnotatedEntityWrapper<CamThinkAiInferenceConnectionPropertiesEntities> wrapper = new AnnotatedEntityWrapper<>();
        try {
            CamThinkAiInferenceConnectionPropertiesEntities.CamThinkAiInferenceProperties camThinkAiInferenceProperties = entityValueServiceProvider.findValuesByKey(
                    CamThinkAiInferenceConnectionPropertiesEntities.getKey(CamThinkAiInferenceConnectionPropertiesEntities.Fields.camthinkAiInferenceProperties), CamThinkAiInferenceConnectionPropertiesEntities.CamThinkAiInferenceProperties.class);
            if (!camThinkAiInferenceProperties.isEmpty()) {
                initModels();
                checkAndUpdateSyncModelsScheduled();
            }
        } catch (Exception e) {
            log.error("Error occurs while initializing connection", e);
            wrapper.saveValue(CamThinkAiInferenceConnectionPropertiesEntities::getApiStatus, false).publishSync();
        }
    }

    private void checkAndUpdateSyncModelsScheduled() {
        AnnotatedEntityWrapper<CamThinkAiInferenceConnectionPropertiesEntities> wrapper = new AnnotatedEntityWrapper<>();
        boolean apiStatus = (Boolean) wrapper.getValue(CamThinkAiInferenceConnectionPropertiesEntities::getApiStatus).orElse(false);
        updateSyncModelsScheduled(apiStatus);
    }

    private void updateSyncModelsScheduled(boolean enabled) {
        AnnotatedEntityWrapper<CamThinkAiInferenceIntegrationEntities.SyncModels> wrapper = new AnnotatedEntityWrapper<>();
        wrapper.saveValue(CamThinkAiInferenceIntegrationEntities.SyncModels::getPeriod, Constants.SYNC_MODELS_PERIOD_SECONDS);
        wrapper.saveValue(CamThinkAiInferenceIntegrationEntities.SyncModels::getEnabled, enabled);
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
    @EventSubscribe(payloadKeyExpression = Constants.INTEGRATION_ID + ".integration.camthink_ai_inference_properties.*")
    public void onAiInferencePropertiesUpdate(Event<CamThinkAiInferenceConnectionPropertiesEntities.CamThinkAiInferenceProperties> event) {
        if (isConfigChanged(event)) {
            initModels();
            checkAndUpdateSyncModelsScheduled();
        }
    }

    @SuppressWarnings("unused")
    @IntegrationScheduled(
            name = "camthink-ai-inference.sync_models",
            fixedRateEntity = "camthink-ai-inference.integration.sync_models.period",
            enabledEntity = "camthink-ai-inference.integration.sync_models.enabled"
    )
    public void syncModels() {
        AnnotatedEntityWrapper<CamThinkAiInferenceConnectionPropertiesEntities> wrapper = new AnnotatedEntityWrapper<>();
        try {
            CamThinkAiInferenceConnectionPropertiesEntities.CamThinkAiInferenceProperties camThinkAiInferenceProperties = entityValueServiceProvider.findValuesByKey(
                    CamThinkAiInferenceConnectionPropertiesEntities.getKey(CamThinkAiInferenceConnectionPropertiesEntities.Fields.camthinkAiInferenceProperties), CamThinkAiInferenceConnectionPropertiesEntities.CamThinkAiInferenceProperties.class);
            if (!camThinkAiInferenceProperties.isEmpty()) {
                initModels();
            }
        } catch (Exception e) {
            log.error("Error occurs while sync models", e);
            wrapper.saveValue(CamThinkAiInferenceConnectionPropertiesEntities::getApiStatus, false).publishSync();
        }
    }

    @SuppressWarnings("unused")
    @EventSubscribe(payloadKeyExpression = Constants.INTEGRATION_ID + ".integration.refresh_models")
    public void refreshModels(Event<CamThinkAiInferenceServiceEntities.RefreshModels> event) {
        initModels();
    }

    @SuppressWarnings("unused")
    @EventSubscribe(payloadKeyExpression = Constants.INTEGRATION_ID + ".integration.draw_result_image.*")
    public EventResponse drawResultImage(Event<CamThinkAiInferenceServiceEntities.DrawResultImage> event) {
        try {
            String imageBase64 = event.getPayload().getImageBase64();
            String camThinkModelInferResponseJson = event.getPayload().getCamthinkInferResponseJson();
            CamThinkModelInferResponse camThinkModelInferResponse = JsonUtils.fromJSON(camThinkModelInferResponseJson, CamThinkModelInferResponse.class);
            String resultImageBase64 = drawResultImage(imageBase64, camThinkModelInferResponse);
            return getEventResponse(Map.of("result_image_base64", resultImageBase64));
        } catch (Exception e) {
            log.error("drawResultImage error:", e);
            if (e instanceof ServiceException) {
                throw (ServiceException) e;
            } else {
                throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), e.getMessage()).build();
            }
        }
    }

    @SuppressWarnings("unused")
    @EventSubscribe(payloadKeyExpression = Constants.INTEGRATION_ID + ".integration.model_*")
    public EventResponse infer(Event<CamThinkAiInferenceServiceEntities.ModelInput> event) {
        CamThinkAiInferenceServiceEntities.ModelInput modelInput = event.getPayload();
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
                CamThinkAiInferenceService service = self();
                autoInferThreadPoolExecutor.execute(() -> service.autoInfer(device, key, value.toString()));
            }
        });
    }

    public CamThinkAiInferenceService self() {
        return SpringContext.getBean(CamThinkAiInferenceService.class);
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
                camThinkModelInferResponse = camThinkAiInferenceClient.modelInfer(modelId, camThinkModelInferRequest);
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

            String imageBase64;
            if (ImageSupport.isUrl(imageEntityValue)) {
                ImageSupport.ImageResult imageResult = ImageSupport.getImageBase64FromUrl(imageEntityValue);
                imageBase64 = imageResult.getImageBase64();
            } else {
                imageBase64 = imageEntityValue;
            }
            ImageSupport.ImageData originImageData = ImageSupport.parseFromImageBase64(imageBase64);
            String originImageFileName = getImageFileName(device.getId(), "origin_image", originImageData.getImageSuffix());
            String originImageResourceUrl = resourceServiceProvider.putTempResource(originImageFileName, originImageData.getContentType(), originImageData.getData());

            InferHistory inferHistory = new InferHistory();
            inferHistory.setModelName(modelMap.get(modelId));
            inferHistory.setOriginImage(originImageResourceUrl);
            inferHistory.setInferStatus(inferStatus.getValue());
            inferHistory.setUplinkAt(uplinkAt);
            inferHistory.setInferAt(inferAt);

            String resultImageResourceUrl = "";
            String inferHistoryEntityKey = EntitySupport.getDeviceEntityKey(device.getKey(), Constants.IDENTIFIER_INFER_HISTORY);
            String resultImageEntityKey = EntitySupport.getDeviceEntityChildrenKey(deviceKey, modelIdentifier, Constants.IDENTIFIER_MODEL_RESULT_IMAGE);
            if (InferStatus.OK.equals(inferStatus)) {
                String resultImageBase64 = drawResultImage(imageBase64, camThinkModelInferResponse);
                ImageSupport.ImageData resultImageData = ImageSupport.parseFromImageBase64(resultImageBase64);
                String resultImageFileName = getImageFileName(device.getId(), "result_image", resultImageData.getImageSuffix());
                resultImageResourceUrl = resourceServiceProvider.putTempResource(resultImageFileName, resultImageData.getContentType(), resultImageData.getData());

                inferHistory.setResultImage(resultImageResourceUrl);
                if (entityServiceProvider.findByKey(resultImageEntityKey) != null) {
                    exchangePayload.put(resultImageEntityKey, resultImageResourceUrl);
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
                Map<String, Pair<Long, Long>> entityKeyLatestIdAndHistoryIds = entityValueServiceProvider.saveValues(exchangePayload);
                linkResource(entityKeyLatestIdAndHistoryIds, inferHistoryEntityKey, List.of(originImageResourceUrl, resultImageResourceUrl));
                linkResource(entityKeyLatestIdAndHistoryIds, resultImageEntityKey, List.of(resultImageResourceUrl));
            }
        } catch (Exception e) {
            log.error("autoInfer error deviceId:{}, imageEntityKey:{}, error:", device.getId(), imageEntityKey, e);
        }
    }

    private void linkResource(Map<String, Pair<Long, Long>> entityKeyLatestIdAndHistoryIds, String entityKey, List<String> resourceUrls) {
        if (CollectionUtils.isEmpty(entityKeyLatestIdAndHistoryIds)) {
            return;
        }

        Pair<Long, Long> idPair = entityKeyLatestIdAndHistoryIds.get(entityKey);
        if (idPair == null) {
            return;
        }

        Long entityLatestId = idPair.getFirst();
        ResourceRefDTO entityLatestIdResourceRef = ResourceRefDTO.of(String.valueOf(entityLatestId), ResourceRefType.ENTITY_LATEST.name());
        resourceServiceProvider.unlinkRef(entityLatestIdResourceRef);

        Long entityHistoryId = idPair.getSecond();
        ResourceRefDTO entityHistoryIdResourceRef = ResourceRefDTO.of(String.valueOf(entityHistoryId), ResourceRefType.ENTITY_HISTORY.name());
        resourceUrls.stream().filter(resourceUrl -> !StringUtils.isEmpty(resourceUrl)).forEach(resourceUrl -> {
            resourceServiceProvider.linkByUrl(resourceUrl, entityLatestIdResourceRef);
            resourceServiceProvider.linkByUrl(resourceUrl, entityHistoryIdResourceRef);
        });
    }

    private String getImageFileName(Long deviceId, String prefix, String suffix) {
        return deviceId + "_" + prefix + "_" + System.currentTimeMillis() + "." + suffix;
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
            return imageBase64;
        }

        if (camThinkModelInferResponse.getData().getOutputs() == null) {
            return imageBase64;
        }

        if (camThinkModelInferResponse.getData().getOutputs().get(CamThinkModelInferResponse.ModelInferData.FIELD_DATA) == null) {
            return imageBase64;
        }
        String dataJson = JsonUtils.toJSON(camThinkModelInferResponse.getData().getOutputs().get(CamThinkModelInferResponse.ModelInferData.FIELD_DATA));
        List<CamThinkModelInferResponse.ModelInferData.OutputData> data = JsonUtils.fromJSON(dataJson, new TypeReference<>() {});
        if(CollectionUtils.isEmpty(data)) {
            return imageBase64;
        }

        CamThinkModelInferResponse.ModelInferData.OutputData outputData = data.get(0);
        if (CollectionUtils.isEmpty(outputData.getDetections())) {
            return imageBase64;
        }

        if (StringUtils.isEmpty(imageBase64)) {
            return imageBase64;
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
        return String.format("%s (%.2f)", cls, conf);
    }

    private static EventResponse getEventResponse(Object responseObj) {
        Map<String, Object> response = JsonUtils.toMap(responseObj);
        EventResponse eventResponse = EventResponse.empty();
        for (Map.Entry<String, Object> entry : response.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            eventResponse.put(key, value);
        }
        return eventResponse;
    }

    private CamThinkModelInferResponse modelInfer(CamThinkAiInferenceServiceEntities.ModelInput modelInput) {
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
        return camThinkAiInferenceClient.modelInfer(modelId, camThinkModelInferRequest);
    }

    private boolean isConfigChanged(Event<CamThinkAiInferenceConnectionPropertiesEntities.CamThinkAiInferenceProperties> event) {
        // check if required fields are set
        CamThinkAiInferenceConnectionPropertiesEntities.CamThinkAiInferenceProperties camThinkAiInferenceProperties = event.getPayload();
        return camThinkAiInferenceProperties.getBaseUrl() != null && camThinkAiInferenceProperties.getToken() != null;
    }

    private boolean testConnection() {
        boolean isConnection = false;
        try {
            if (camThinkAiInferenceClient != null && camThinkAiInferenceClient.getConfig() != null) {
                isConnection = camThinkAiInferenceClient.testConnection();
            }
        } catch (Exception e) {
            log.error("Error occurs while testing connection", e);
        }
        AnnotatedEntityWrapper<CamThinkAiInferenceConnectionPropertiesEntities> wrapper = new AnnotatedEntityWrapper<>();
        wrapper.saveValue(CamThinkAiInferenceConnectionPropertiesEntities::getApiStatus, isConnection).publishSync();
        if (!isConnection) {
            throw ServiceException.with(ServerErrorCode.SERVER_NOT_REACHABLE.getErrorCode(), ServerErrorCode.SERVER_NOT_REACHABLE.getErrorMessage()).build();
        }
        return true;
    }

    private void initModels() {
        if (testConnection()) {
            long start = System.currentTimeMillis();
            CamThinkModelListResponse camThinkModelListResponse = camThinkAiInferenceClient.getModels();
            if (camThinkModelListResponse == null) {
                throwServiceNotReachableException();
                return;
            }

            if (camThinkModelListResponse.getData() == null) {
                throwServiceNotReachableException();
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

            List<CompletableFuture<Entity>> futures = new ArrayList<>();
            for (CamThinkModelListResponse.ModelData modelData : camThinkModelListResponse.getData()) {
                ModelServiceEntityTemplate modelServiceEntityTemplate = ModelServiceEntityTemplate.builder()
                        .modelId(modelData.getId())
                        .name(modelData.getName())
                        .description(modelData.getRemark())
                        .engineType(modelData.getEngineType())
                        .build();
                Entity modelServiceEntity = modelServiceEntityTemplate.toEntity();
                futures.add(fetchAndSetModelInputEntities(modelServiceEntity, modelData.getId()));
            }
            CompletableFuture<Void> allFuture = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .orTimeout(10, TimeUnit.SECONDS)
                    .thenRun(() -> {
                        try {
                            futures.stream().map(CompletableFuture::join).forEach(entityServiceProvider::save);
                        } catch (Exception e) {
                            log.error("Error occurs while saving model entities", e);
                        }
                    })
                    .whenComplete((v, e) -> {
                        long duration = System.currentTimeMillis() - start;
                        if (e != null) {
                            log.error("initModels failed after {} ms", duration, e);
                        } else {
                            log.info("initModels succeeded in {} ms", duration);
                        }
                    })
                    .exceptionally(e -> {
                        if (e instanceof TimeoutException) {
                            log.error("Timeout while waiting for some fetching model detail tasks", e);
                            futures.forEach(f -> f.cancel(true));
                        } else {
                            log.error("Error occurs while waiting for all futures to complete: fetching model details and setting model input entities", e);
                        }
                        return null;
                    });

            try {
                allFuture.join();
            } catch (Exception e) {
                log.error("Error occurs while waiting for all futures to complete: fetching model details and setting model input entities", e);
            }
        }
    }

    private void throwServiceNotReachableException() {
        AnnotatedEntityWrapper<CamThinkAiInferenceConnectionPropertiesEntities> wrapper = new AnnotatedEntityWrapper<>();
        wrapper.saveValue(CamThinkAiInferenceConnectionPropertiesEntities::getApiStatus, false).publishSync();
        throw ServiceException.with(ServerErrorCode.SERVER_NOT_REACHABLE.getErrorCode(), ServerErrorCode.SERVER_NOT_REACHABLE.getErrorMessage()).build();
    }

    private CompletableFuture<Entity> fetchAndSetModelInputEntities(Entity modelServiceEntity, String modelId) {
        String tenantId = TenantContext.getTenantId();
        return CompletableFuture.supplyAsync(() -> {
            TenantContext.setTenantId(tenantId);
            CamThinkModelDetailResponse camThinkModelDetailResponse = camThinkAiInferenceClient.getModelDetail(modelId);
            if (camThinkModelDetailResponse == null) {
                return modelServiceEntity;
            }
            if (camThinkModelDetailResponse.getData() == null) {
                return modelServiceEntity;
            }
            if (CollectionUtils.isEmpty(camThinkModelDetailResponse.getData().getInputSchema())) {
                return modelServiceEntity;
            }

            List<Entity> inputEntities = new ArrayList<>();
            for (CamThinkModelDetailResponse.InputSchema inputSchema : camThinkModelDetailResponse.getData().getInputSchema()) {
                ModelServiceInputEntityTemplate modelServiceInputEntityTemplate = ModelServiceInputEntityTemplate.builder()
                        .parentIdentifier(modelServiceEntity.getIdentifier())
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
            }

            if (!CollectionUtils.isEmpty(inputEntities)) {
                modelServiceEntity.setChildren(inputEntities);
            }
            return modelServiceEntity;
        }).exceptionally(ex -> {
            log.error("Error occurs while fetching model detail and setting model input entities with modelId: {}", modelId, ex);
            return modelServiceEntity;
        });
    }

    public ModelOutputSchemaResponse fetchModelDetail(String modelId) {
        ModelOutputSchemaResponse modelOutputSchemaResponse = new ModelOutputSchemaResponse();
        CamThinkModelDetailResponse camThinkModelDetailResponse;
        if (testConnection()) {
            camThinkModelDetailResponse = camThinkAiInferenceClient.getModelDetail(modelId);
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
            modelOutputSchemaResponse.setInputEntities(modelServiceEntity.getChildren());
        }
        return modelOutputSchemaResponse;
    }
}