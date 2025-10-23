package com.milesight.beaveriot.integrations.milesightgateway.model.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * AddDeviceRequest class.
 *
 * @author simon
 * @date 2025/3/5
 */
@Data
public class AddDeviceRequest {
    String name;

    String description;

    String devEUI;

    String profileID;

    @JsonProperty("fPort")
    Long fPort;

    Boolean skipFCntCheck;

    String appKey;

    Boolean isDefaultAppKey;

    String applicationID;
}
