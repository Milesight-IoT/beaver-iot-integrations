package com.milesight.beaveriot.integrations.milesightgateway.codec.model;

import lombok.Data;

import java.util.List;

/**
 * DeviceResourceInfo class.
 *
 * @author simon
 * @date 2025/2/27
 */
@Data
public class DeviceResourceInfo {
    String id;

    String name;

    String description;

    String catalog;

    /**
     * Series number prefix
     */
    String sn;

    /**
     * EUI prefix
     */
    String deveui;

    List<String> deviceProfile;

    String codec;

    String decoderScript;

    String encoderScript;
}
