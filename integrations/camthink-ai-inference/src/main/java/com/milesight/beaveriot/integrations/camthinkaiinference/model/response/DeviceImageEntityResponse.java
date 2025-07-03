package com.milesight.beaveriot.integrations.camthinkaiinference.model.response;

import lombok.Data;

import java.util.List;

/**
 * author: Luxb
 * create: 2025/6/18 16:00
 **/
@Data
public class DeviceImageEntityResponse {
    private List<ImageEntityData> content;

    private DeviceImageEntityResponse(List<ImageEntityData> imageEntityDataList) {
        this.content = imageEntityDataList;
    }

    public static DeviceImageEntityResponse build(List<ImageEntityData> imageEntityDataList) {
        return new DeviceImageEntityResponse(imageEntityDataList);
    }

    @Data
    public static class ImageEntityData {
        private String id;
        private String key;
        private String name;
        private String format;
        private String value;
    }
}
