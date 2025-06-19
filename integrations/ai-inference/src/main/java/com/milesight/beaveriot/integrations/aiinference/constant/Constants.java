package com.milesight.beaveriot.integrations.aiinference.constant;

import java.util.Set;

/**
 * author: Luxb
 * create: 2025/6/5 8:37
 **/
public class Constants {
    public static final String INTEGRATION_ID = "ai-inference";
    public static final String ATTRIBUTE_KEY_FORMAT = "format";
    public static final String ATTRIBUTE_FORMAT_IMAGE_BASE64 = "IMAGE:BASE64";
    public static final String ATTRIBUTE_FORMAT_IMAGE_URL = "IMAGE:URL";
    public static final Set<String> ATTRIBUTE_FORMAT_IMAGE_SET = Set.of(ATTRIBUTE_FORMAT_IMAGE_BASE64, ATTRIBUTE_FORMAT_IMAGE_URL);
}
