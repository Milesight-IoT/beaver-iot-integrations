package com.milesight.beaveriot.parser.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * AWS Lora消息载荷
 *
 * @author linzy
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LnsAwsPayload {

    /**
     * sn
     */
    private String sn;

    /**
     * 数据链路类型
     */
    private String dataLinkType;

    /**
     * 三方设备id(设备不上传sn，由平台根据三方设备id查询)
     */
    @JsonProperty("WirelessDeviceId")
    private String wirelessDeviceId;

    /**
     * 消息数据（base64位）
     */
    @JsonProperty("PayloadData")
    private String payloadData;

    /**
     * wirelessMetadata
     */
    @JsonProperty("WirelessMetadata")
    private JsonNode wirelessMetadata;

}
