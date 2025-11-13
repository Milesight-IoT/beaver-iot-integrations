package com.milesight.beaveriot.integrations.milesightgateway.model.api;

/**
 * DeviceListItemFields class.
 *
 * Only interested fields included.
 *
 * @author simon
 * @date 2025/2/24
 */
public class DeviceListItemFields {
    private DeviceListItemFields() {}

    public static final String DEV_EUI = "devEUI";
    public static final String NAME = "name";
    public static final String SKIP_F_CNT_CHECK = "skipFCntCheck";
    public static final String APP_KEY = "appKey";
    public static final String F_PORT = "fPort";
    public static final String PAYLOAD_CODEC_ID = "payloadCodecID";
    public static final String PAYLOAD_NAME = "payloadName";
}
