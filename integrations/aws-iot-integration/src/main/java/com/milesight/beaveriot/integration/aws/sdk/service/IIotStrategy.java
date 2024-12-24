package com.milesight.beaveriot.integration.aws.sdk.service;

import com.milesight.beaveriot.integration.aws.entity.AwsServiceEntities;
import software.amazon.awssdk.services.iotwireless.model.DeleteWirelessDeviceResponse;
import software.amazon.awssdk.services.iotwireless.model.SendDataToWirelessDeviceResponse;
import software.amazon.awssdk.services.iotwireless.model.WirelessDeviceStatistics;

import java.util.List;

/**
 * @author linzy
 */
public interface IIotStrategy {

    /**
     * 创建lora设备
     *
     * @return
     * @throws Exception
     */
    String createLoraDevice(AwsServiceEntities.AddDevice addDevice) throws Exception;

    /**
     * 删除lora设备
     * @param wirelessDeviceId
     * @return
     * @throws Exception
     */
    DeleteWirelessDeviceResponse deleteWirelessDevice(String wirelessDeviceId) throws Exception;

    /**
     * 查询lora设备列表
     * @param serviceProfileId
     * @param destinationName
     * @param multicastGroupId
     * @param fuotaTaskId
     * @return
     * @throws Exception
     */
    List<WirelessDeviceStatistics> listWirelessDevices(String serviceProfileId, String destinationName,
                                                              String multicastGroupId, String fuotaTaskId) throws Exception ;

    /**
     * 发送数据
     * @param wirelessDeviceId
     * @param payload
     * @return
     * @throws Exception
     */
    SendDataToWirelessDeviceResponse sendDataToWirelessDevice(String wirelessDeviceId, String payload) throws Exception ;
}
