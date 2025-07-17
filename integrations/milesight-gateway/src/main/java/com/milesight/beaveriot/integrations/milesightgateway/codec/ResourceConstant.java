package com.milesight.beaveriot.integrations.milesightgateway.codec;

/**
 * ResourceConstant class.
 *
 * @author simon
 * @date 2025/3/6
 */
public class ResourceConstant {
    public static final String DEFAULT_DEVICE_CODEC_URI = "https://raw.githubusercontent.com/Milesight-IoT/codec/refs/heads/release/";

    public static final String DEFAULT_DEVICE_CODEC_VERSION = "version.json";

    public static final String DECODER_ENTITY_IDENTIFIER = "#decoder";

    public static final String DECODER_ENTITY_NAME = "Decoder";

    public static final String ENCODER_ENTITY_IDENTIFIER = "#encoder";

    public static final String ENCODER_ENTITY_NAME = "Encoder";

    public static final String CODEC_LEVEL_SEPARATOR = ".";

    public static final String CODEC_ARRAY_ITEM_IDENTIFIER = "_item";

    public static final String ENTITY_LEVEL_SEPARATOR = "#";

    public static final int JSON_REQUEST_CONNECTION_TIMEOUT = 3000;

    public static final int JSON_REQUEST_READ_TIMEOUT = 10000;

    public static String getEntityArraySeparator(int index) {
        return "[" + index + "]";
    }

    private ResourceConstant() {}
}
