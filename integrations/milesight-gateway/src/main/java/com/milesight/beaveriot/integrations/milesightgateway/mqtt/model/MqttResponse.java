package com.milesight.beaveriot.integrations.milesightgateway.mqtt.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * MqttResponse class.
 *
 * @author simon
 * @date 2025/2/14
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MqttResponse<T> extends MqttRawResponse {
    private T successBody;

    private MqttRequestError errorBody;
}
