package com.milesight.beaveriot.integrations.milesightgateway.model.api;

import lombok.Data;

/**
 * DeviceDef class.
 *
 * @author simon
 * @date 2025/2/14
 */
@Data
public class DeviceCodec {
    private String id;

    private String name;

    private String description;

    private String templateID;

    private String devEUIPrefix;

    private String encoderScript;

    private String decoderScript;

    private Boolean testEnabled;

    private Integer fPort;

    private String codecJson;
}
