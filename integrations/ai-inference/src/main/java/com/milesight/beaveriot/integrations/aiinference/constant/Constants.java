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
    public static final String IDENTIFIER_MODEL_ID = "model_id";
    public static final String IDENTIFIER_MODEL_PREFIX = "model_";
    public static final String IDENTIFIER_MODEL_FORMAT = IDENTIFIER_MODEL_PREFIX + "{0}";
    public static final String IDENTIFIER_MODEL_INFER_INPUTS = "infer_inputs";
    public static final String IDENTIFIER_MODEL_RESULT_IMAGE = "result_image";
    public static final String IDENTIFIER_INFER_HISTORY = "infer_history";
    public static final String IDENTIFIER_BIND_AT = "bind_at";
    public static final String ENTITY_KEY_FORMAT = "{0}.{1}";
    public static final String CHILDREN_ENTITY_KEY_FORMAT = "{0}.{1}.{2}";
}
