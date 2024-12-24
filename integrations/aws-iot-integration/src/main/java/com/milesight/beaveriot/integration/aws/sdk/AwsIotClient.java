package com.milesight.beaveriot.integration.aws.sdk;


import com.milesight.beaveriot.integration.aws.sdk.config.AwsIotProperties;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DescribeEndpointRequest;
import software.amazon.awssdk.services.iot.model.DescribeEndpointResponse;
import software.amazon.awssdk.services.iotwireless.IotWirelessClient;
import software.amazon.awssdk.services.iotwireless.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author linzy
 */
@Slf4j
public class AwsIotClient {

    AwsIotProperties awsIotProperties;
    IotClient iotClient;
    IotWirelessClient iotWirelessClient;
    String mqttDataAtsEndpoint;

    public AwsIotClient(AwsIotProperties awsIotProperties) throws Exception {
        this.awsIotProperties = awsIotProperties;
        iotClient = IotClient.builder()
                .region(Region.of(awsIotProperties.getRegion()))
                .credentialsProvider(() -> AwsBasicCredentials.create(awsIotProperties.getAccessKey(), awsIotProperties.getSecretKey()))
                .build();
        iotWirelessClient = IotWirelessClient.builder()
                .region(Region.of(awsIotProperties.getRegion()))
                .credentialsProvider(() -> AwsBasicCredentials.create(awsIotProperties.getAccessKey(), awsIotProperties.getSecretKey()))
                .build();
        String dataAtsEndpointType = "iot:Data-ATS";
        mqttDataAtsEndpoint = describeEndpoint(dataAtsEndpointType).endpointAddress();
    }

    public DescribeEndpointResponse describeEndpoint(String endpointType) {
        DescribeEndpointRequest describeEndpointRequest = DescribeEndpointRequest.builder()
                .endpointType(endpointType)
                .build();
        return iotClient.describeEndpoint(describeEndpointRequest);
    }

    /**
     * 创建 LoRaWAN 设备
     * @param sn
     * @param devEui
     * @param devAppKey
     * @param deviceProfileId
     * @param serviceProfileId
     * @param destinationName
     * @return
     */
    public CreateWirelessDeviceResponse createWirelessDevice(String sn, String devEui, String devAppKey, String deviceProfileId, String serviceProfileId, String destinationName) {
        OtaaV1_0_x otaaV1_0_x = OtaaV1_0_x.builder()
                .appKey(devAppKey)
                .appEui(awsIotProperties.getAppEui())
                .genAppKey(devAppKey)
                .build();
        FPorts fPorts = FPorts.builder()
                .multicast(200)
                .fuota(201)
                .clockSync(202)
                .build();
        LoRaWANDevice loRaWANDevice = LoRaWANDevice.builder()
                .devEui(devEui)
                .otaaV1_0_x(otaaV1_0_x)
                .deviceProfileId(deviceProfileId)
                .serviceProfileId(serviceProfileId)
                .fPorts(fPorts)
                .build();
        CreateWirelessDeviceRequest createWirelessDeviceRequest = CreateWirelessDeviceRequest.builder()
                .loRaWAN(loRaWANDevice)
                .destinationName(destinationName)
                .type(WirelessDeviceType.LO_RA_WAN)
                .name(sn)
                .build();
        return iotWirelessClient.createWirelessDevice(createWirelessDeviceRequest);
    }

    public DeleteWirelessDeviceResponse deleteWirelessDevice(String wirelessDeviceId) {
        DeleteWirelessDeviceRequest deleteWirelessDeviceRequest = DeleteWirelessDeviceRequest.builder()
                .id(wirelessDeviceId)
                .build();
        return iotWirelessClient.deleteWirelessDevice(deleteWirelessDeviceRequest);
    }

    public GetWirelessDeviceResponse getWirelessDevice(String wirelessDeviceId) {
        GetWirelessDeviceRequest getWirelessDeviceRequest = GetWirelessDeviceRequest.builder()
                .identifier(wirelessDeviceId)
                .identifierType(WirelessDeviceIdType.WIRELESS_DEVICE_ID)
                .build();
        return iotWirelessClient.getWirelessDevice(getWirelessDeviceRequest);
    }

    public List<WirelessDeviceStatistics> listWirelessDevices(String serviceProfileId, String destinationName,
                                                              String multicastGroupId, String fuotaTaskId) {
        ListWirelessDevicesResponse listWirelessDevicesResponse = listWirelessDevices(serviceProfileId, destinationName, multicastGroupId, fuotaTaskId, null);
        String nextToken = listWirelessDevicesResponse.nextToken();
        if(StringUtils.isBlank(nextToken)){
            return listWirelessDevicesResponse.wirelessDeviceList();
        }else{
            List<WirelessDeviceStatistics> wirelessDeviceStatisticsList = new ArrayList<>();
            wirelessDeviceStatisticsList.addAll(listWirelessDevicesResponse.wirelessDeviceList());
            listWirelessDevices(serviceProfileId, destinationName, multicastGroupId, fuotaTaskId, nextToken,wirelessDeviceStatisticsList);
            return wirelessDeviceStatisticsList;
        }
    }

    private void listWirelessDevices(String serviceProfileId, String destinationName,
                                                              String multicastGroupId, String fuotaTaskId, String nextToken,List<WirelessDeviceStatistics> wirelessDeviceStatisticsList) {
        ListWirelessDevicesResponse nextListWirelessDevicesResponse = listWirelessDevices(serviceProfileId, destinationName, multicastGroupId, fuotaTaskId, nextToken);
        String continueNextToken = nextListWirelessDevicesResponse.nextToken();
        if(StringUtils.isBlank(continueNextToken)){
            wirelessDeviceStatisticsList.addAll(nextListWirelessDevicesResponse.wirelessDeviceList());
        }else{
            wirelessDeviceStatisticsList.addAll(nextListWirelessDevicesResponse.wirelessDeviceList());
            listWirelessDevices(serviceProfileId, destinationName, multicastGroupId, fuotaTaskId, continueNextToken,wirelessDeviceStatisticsList);
        }
    }

    private ListWirelessDevicesResponse listWirelessDevices(String serviceProfileId, String destinationName,
                                                              String multicastGroupId, String fuotaTaskId, String nextToken) {
        ListWirelessDevicesRequest listWirelessDevicesRequest = ListWirelessDevicesRequest.builder()
                .wirelessDeviceType(WirelessDeviceType.LO_RA_WAN)
                .serviceProfileId(serviceProfileId)
                .destinationName(destinationName)
                .multicastGroupId(multicastGroupId)
                .fuotaTaskId(fuotaTaskId)
                .maxResults(250)
                .nextToken(nextToken)
                .build();
        return iotWirelessClient.listWirelessDevices(listWirelessDevicesRequest);
    }

    public SendDataToWirelessDeviceResponse sendDataToWirelessDevice(String wirelessDeviceId, String payload) {
        LoRaWANSendDataToDevice loRaWANSendDataToDevice = LoRaWANSendDataToDevice.builder()
                .fPort(85)
                .build();
        WirelessMetadata wirelessMetadata = WirelessMetadata.builder()
                .loRaWAN(loRaWANSendDataToDevice)
                .build();
        SendDataToWirelessDeviceRequest sendDataToWirelessDeviceRequest = SendDataToWirelessDeviceRequest.builder()
                .id(wirelessDeviceId)
                .transmitMode(1)
                .wirelessMetadata(wirelessMetadata)
                .payloadData(payload)
                .build();
        return iotWirelessClient.sendDataToWirelessDevice(sendDataToWirelessDeviceRequest);
    }


}
