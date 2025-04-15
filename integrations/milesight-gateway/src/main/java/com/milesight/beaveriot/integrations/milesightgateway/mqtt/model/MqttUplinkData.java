package com.milesight.beaveriot.integrations.milesightgateway.mqtt.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * MqttUplinkData class.
 *
 * @author simon
 * @date 2025/3/20
 */
@Data
public class MqttUplinkData {
    private String applicationID;

    private String applicationName;

    private String data;

    private String devEUI;

    private String deviceName;

    @JsonAlias("fCnt")
    private Integer fCnt;

    @JsonAlias("fPort")
    private Integer fPort;

    private String time;

    // ignore: rxInfo / txInfo
}
