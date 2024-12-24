package com.milesight.beaveriot.vms.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.json.JSONUtil;
import com.milesight.beaveriot.vms.api.HuggingfaceApi;
import com.milesight.beaveriot.vms.model.ImageObjectDetectionRequest;
import com.milesight.beaveriot.vms.model.ImageObjectDetectionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @Author yuanh
 * @Description
 * @Package com.milesight.beaveriot.vms.service
 * @Date 2024/11/27 16:31
 */
@Service
@Slf4j
public class VmsImageService {

    /**
     * 图片物品分析
     *
     * @param request
     * @return
     */
    public List<ImageObjectDetectionResponse> objectDetection(ImageObjectDetectionRequest request) {
        String image = request.getImage();
        if (image == null) {
            return Collections.emptyList();
        }
        String body = HuggingfaceApi.objectDetection(image);
        List<ImageObjectDetectionResponse> imageAnalyzeResponses = CollUtil.newArrayList();
        try {
            imageAnalyzeResponses = JSONUtil.toList(body, ImageObjectDetectionResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse response body: " + body);
        }
        return initImageAnalyzeResponse(imageAnalyzeResponses);
    }

    private List<ImageObjectDetectionResponse> initImageAnalyzeResponse(List<ImageObjectDetectionResponse> imageAnalyzeResponses) {
        String imageLabels = HuggingfaceApi.IMAGE_LABELS;
        String imageScoreStr = HuggingfaceApi.IMAGE_SCORE;
        if (CharSequenceUtil.isNotBlank(imageScoreStr)) {
            float imageScore = Float.parseFloat(imageScoreStr);
            imageAnalyzeResponses = imageAnalyzeResponses.stream()
                    .filter(response -> response.getScore() > imageScore)
                    .toList();
        }
        if (CharSequenceUtil.isBlank(imageLabels)) {
            return imageAnalyzeResponses;
        }
        //
        List<String> imageLabelList = Arrays.asList(imageLabels.split(","));
        // 过滤合法的标签
        imageAnalyzeResponses = imageAnalyzeResponses.stream()
                .filter(response -> imageLabelList.contains(response.getLabel()))
                .toList();
        return imageAnalyzeResponses;
    }
}
