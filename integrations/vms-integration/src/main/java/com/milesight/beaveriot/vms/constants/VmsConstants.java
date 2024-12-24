package com.milesight.beaveriot.vms.constants;

import lombok.experimental.UtilityClass;

/**
 * @Author yuanh
 * @Description
 * @Package com.milesight.beaveriot.integration.constants
 * @Date 2024/11/19 17:28
 */
@UtilityClass
public class VmsConstants {

    public static final String INTEGRATION_ID = "vms-integration";

    @UtilityClass
    public static class Entity {
        public static final String DETECT_STATUS = "detect_status";
        public static final String DEVICE_ID = "device_id";
        public static final String BENCHMARK = "benchmark";
        public static final String ADD_DEVICE = "add_device";
        public static final String DELETE_DEVICE = "delete_device";
        public static final String VMS_INFO = "vms_info";
        public static final String VMS_URL = "vms_url";
        public static final String USERNAME = "username";
        public static final String PASSWORD = "password";
        public static final String AI_URL = "ai_url";
        public static final String AI_ACCESS_TOKEN = "ai_access_token";
        public static final String OBJECT_DETECTION_MODEL = "object_detection_model";
        public static final String IMAGE_LABELS = "image_labels";
        public static final String IMAGE_SCORE = "image_score";
    }

    @UtilityClass
    public static class DeviceEntity {
        public static final String ONLINE = "online";
        public static final String GET_HLS_URL = "get_hls_url";
        public static final String SYNC = "sync";
        public static final String GET_HLS_VOD_URL = "get_hls_vod_url";
        public static final String START_TIME = "start_time";
    }
}
