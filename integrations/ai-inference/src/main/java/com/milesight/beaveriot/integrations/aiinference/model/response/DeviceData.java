package com.milesight.beaveriot.integrations.aiinference.model.response;

import lombok.Data;

/**
 * author: Luxb
 * create: 2025/6/20 14:01
 **/
@Data
public class DeviceData {
    private String id;
    private String name;
    private String integrationId;
    private String integrationName;

    public DeviceData(String id, String name, String integrationId, String integrationName) {
        this.id = id;
        this.name = name;
        this.integrationId = integrationId;
        this.integrationName = integrationName;
    }
}
