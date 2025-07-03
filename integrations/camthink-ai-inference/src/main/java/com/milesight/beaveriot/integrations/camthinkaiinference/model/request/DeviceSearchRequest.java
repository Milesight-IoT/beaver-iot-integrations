package com.milesight.beaveriot.integrations.camthinkaiinference.model.request;

import lombok.Data;

/**
 * author: Luxb
 * create: 2025/6/18 16:04
 **/
@Data
public class DeviceSearchRequest {
    private String name;
    private Boolean isBound;
}
