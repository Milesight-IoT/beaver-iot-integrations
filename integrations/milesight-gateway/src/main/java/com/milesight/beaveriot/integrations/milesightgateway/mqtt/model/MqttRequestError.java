package com.milesight.beaveriot.integrations.milesightgateway.mqtt.model;

import lombok.Data;

/**
 * MqttRequestError class.
 *
 * @author simon
 * @date 2025/2/14
 */
@Data
public class MqttRequestError {
    Integer code;

    String error;
}
