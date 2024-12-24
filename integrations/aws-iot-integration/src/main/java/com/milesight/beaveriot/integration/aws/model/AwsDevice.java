package com.milesight.beaveriot.integration.aws.model;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;


@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AwsDevice {

    private String deviceId;

    private String sn;

    private String devEui;

    private String devAppKey;

    private String rfRegion;

    private String loraClass;
}