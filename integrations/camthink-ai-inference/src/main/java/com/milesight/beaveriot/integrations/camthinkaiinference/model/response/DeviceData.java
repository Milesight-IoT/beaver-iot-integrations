package com.milesight.beaveriot.integrations.camthinkaiinference.model.response;

import lombok.Data;

/**
 * author: Luxb
 * create: 2025/6/20 14:01
 **/
@Data
public class DeviceData {
    private String id;
    private String identifier;
    private String name;
    private String integrationId;
    private String integrationName;
    private boolean isBound;
}
