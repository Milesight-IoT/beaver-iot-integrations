package com.milesight.beaveriot.integrations.milesightgateway.mqtt.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * MqttDownlinkData class.
 *
 * @author simon
 * @date 2025/3/21
 */
@Data
public class MqttDownlinkData {
    private String devEUI;

    private Boolean confirmed = false;

    @JsonAlias("fPort")
    private Integer fPort;

    // Base64 encoded
    private String data;
}
