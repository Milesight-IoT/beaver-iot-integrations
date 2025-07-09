package com.milesight.beaveriot.integrations.camthinkaiinference.model.request;

import lombok.Data;

import java.util.List;

/**
 * author: Luxb
 * create: 2025/6/21 10:40
 **/
@Data
public class DeviceUnbindRequest {
    private List<String> deviceIds;
}