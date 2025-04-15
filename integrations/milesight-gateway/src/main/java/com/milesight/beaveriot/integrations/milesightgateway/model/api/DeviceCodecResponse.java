package com.milesight.beaveriot.integrations.milesightgateway.model.api;

import lombok.Data;

import java.util.List;

/**
 * DeviceDefResponse class.
 *
 * @author simon
 * @date 2025/2/14
 */
@Data
public class DeviceCodecResponse {
    private Integer totalCount;

    private List<DeviceCodec> result;
}
