package com.milesight.beaveriot.integrations.milesightgateway.legacy;

import com.milesight.beaveriot.integrations.milesightgateway.util.Constants;

/**
 * LegacyConstants class.
 * <p><em>Intend to be removed at V2.0.</em></p>
 *
 *
 * @author simon
 * @date 2025/9/22
 */
public class LegacyConstants {

    public static String OLD_GATEWAY_STATUS_ENTITY_IDENTIFIER = "status";

    public static String NEW_GATEWAY_STATUS_ENTITY_IDENTIFIER = "@status";

    public static final String DEVICE_MODEL_DATA_IDENTIFIER = "device-model-data";

    public static final String DEVICE_MODEL_DATA_KEY = Constants.INTEGRATION_ID + ".integration." + DEVICE_MODEL_DATA_IDENTIFIER;

    public static final String SYNC_DEVICE_CODEC_IDENTIFIER = "sync-device-codec";

    public static final String SYNC_DEVICE_CODEC_KEY = Constants.INTEGRATION_ID + ".integration." + SYNC_DEVICE_CODEC_IDENTIFIER;

    public static final String MODEL_REPO_URL_IDENTIFIER = "model-repo-url";

    public static final String MODEL_REPO_URL_KEY = Constants.INTEGRATION_ID + ".integration." + MODEL_REPO_URL_IDENTIFIER;

    private LegacyConstants() {}
}
