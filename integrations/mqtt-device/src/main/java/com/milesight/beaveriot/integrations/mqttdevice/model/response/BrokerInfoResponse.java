package com.milesight.beaveriot.integrations.mqttdevice.model.response;

import lombok.Builder;
import lombok.Data;

/**
 * author: Luxb
 * create: 2025/6/13 11:10
 **/
@Builder
@Data
public class BrokerInfoResponse {
    private String server;
    private Integer port;
    private String username;
    private String password;
    private String topicPrefix;
}
