package com.milesight.beaveriot.vms.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.milesight.beaveriot.common.util.MilesightHttpUtil;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.support.SpringContext;
import com.milesight.beaveriot.vms.constants.VmsConstants;
import com.milesight.beaveriot.vms.entity.VmsIntegrationEntities;
import com.milesight.beaveriot.vms.enums.ObjectDetectionModel;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.Map;

/**
 * @Author yuanh
 * @Description
 * @Package com.milesight.beaveriot.vms.api
 * @Date 2024/11/26 16:45
 */
@UtilityClass
public class HuggingfaceApi {
    private static String URL = "https://api-inference.huggingface.co";
    private static String ACCESS_TOKEN = "";
    public static String IMAGE_LABELS = "";
    public static String IMAGE_SCORE = "0.8";
    private static String OBJECT_DETECTION_MODEL = "/models/facebook/detr-resnet-50";
//      /models/facebook/detr-resnet-50-dc5
//    /models/facebook/detr-resnet-101
//    private static String OBJECT_DETECTION_URL = "/models/hustvl/yolos-small";

    public static void init() {
        EntityValueServiceProvider entityValueServiceProvider = SpringContext.getBean(EntityValueServiceProvider.class);
        String vmsInfoId = VmsConstants.INTEGRATION_ID + ".integration." + VmsConstants.Entity.VMS_INFO + ".";
        String urlId = vmsInfoId + VmsConstants.Entity.AI_URL;
        String tokenId = vmsInfoId + VmsConstants.Entity.AI_ACCESS_TOKEN;
        String labelsId = vmsInfoId + VmsConstants.Entity.IMAGE_LABELS;
        String scoreId = vmsInfoId + VmsConstants.Entity.IMAGE_SCORE;
        String objectDetectionModelId = vmsInfoId + VmsConstants.Entity.OBJECT_DETECTION_MODEL;
        Map<String, JsonNode> jsonNodeMap = entityValueServiceProvider.findValuesByKeys(List.of(urlId, tokenId, labelsId, scoreId,objectDetectionModelId));
        jsonNodeMap.forEach((key, value) -> {
            if (value == null || value.isNull()){
                return;
            }
            String textValue = value.textValue();
            if (key.equals(urlId)){
                URL = textValue;
            }
            if (key.equals(tokenId)){
                ACCESS_TOKEN = textValue;
            }
            if (key.equals(labelsId)){
                IMAGE_LABELS = textValue;
            }
            if (key.equals(scoreId)){
                IMAGE_SCORE = textValue;
            }
            if (key.equals(objectDetectionModelId)){
                OBJECT_DETECTION_MODEL = ObjectDetectionModel.getUrl(textValue);
            }
        });
    }

    public static synchronized void setInfo(VmsIntegrationEntities.VmsInfo vmsInfo) {
        URL = vmsInfo.getAiUrl();
        ACCESS_TOKEN = vmsInfo.getAiAccessToken();
        IMAGE_LABELS = vmsInfo.getImageLabels();
        IMAGE_SCORE = vmsInfo.getImageScore();
        OBJECT_DETECTION_MODEL = ObjectDetectionModel.getUrl(vmsInfo.getObjectDetectionModel());
    }

    public static String objectDetection(String base64Image) {
        Map<String, String> headers = Map.of("Authorization", "Bearer " + ACCESS_TOKEN);
        Map<String, Object> requestObj = Map.of("inputs", base64Image);
        return MilesightHttpUtil.postBody(URL + "/models/" + OBJECT_DETECTION_MODEL, 5000, requestObj, headers);
    }
}
