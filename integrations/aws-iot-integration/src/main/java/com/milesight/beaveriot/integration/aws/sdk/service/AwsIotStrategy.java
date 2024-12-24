package com.milesight.beaveriot.integration.aws.sdk.service;

import cn.hutool.core.util.StrUtil;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.integration.aws.entity.AwsConnectionPropertiesEntities;
import com.milesight.beaveriot.integration.aws.entity.AwsServiceEntities;
import com.milesight.beaveriot.integration.aws.sdk.AwsIotClient;
import com.milesight.beaveriot.integration.aws.sdk.config.AwsIotProperties;
import com.milesight.beaveriot.integration.aws.sdk.config.DestinationProperties;
import com.milesight.beaveriot.integration.aws.sdk.enums.LoraClass;
import com.milesight.beaveriot.integration.aws.sdk.enums.RfRegion;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.iotwireless.model.CreateWirelessDeviceResponse;
import software.amazon.awssdk.services.iotwireless.model.DeleteWirelessDeviceResponse;
import software.amazon.awssdk.services.iotwireless.model.SendDataToWirelessDeviceResponse;
import software.amazon.awssdk.services.iotwireless.model.WirelessDeviceStatistics;

import java.util.List;

import static com.milesight.beaveriot.integration.aws.sdk.enums.LoraClass.*;
import static com.milesight.beaveriot.integration.aws.sdk.enums.RfRegion.*;

/**
 * @author linzy
 */
@Component
//@EnableConfigurationProperties(AwsIotProperties.class)
//@ConditionalOnProperty(prefix = "sdk.aws", name = "enabled", havingValue = "true")
@Slf4j
public class AwsIotStrategy implements IIotStrategy, SmartInitializingSingleton {

    private AwsIotClient awsIotClient;
    @Autowired
    private DestinationProperties destinationProperties;
    @Autowired
    private EntityValueServiceProvider entityValueServiceProvider;

    private static final RfRegion[] RF_REGION = {CN470, EU868, IN865, RU864, US915, AS923_1, AS923_2, AS923_3, AS923_4, KR920};

    private static final LoraClass[] LORA_CLASS = {A, B, C};

    private static final String DEV_APP_KEY = "5572404c696e6b4c6f52613230313823";
    private static final String DEVICE_PROFILE_ID = "78400e87-99f2-4085-b07d-a740d29e0a66";
    private static final String SERVICE_PROFILE_ID = "aead180d-e875-4a44-9486-1aeeb49e1f6a";

    private static String ACCESS_KEY = "";

    private static String SECRET_KEY = "";

    public AwsIotStrategy() throws Exception {

    }

    @Override
    public void afterSingletonsInstantiated() {
        awsIot();
    }

    private void awsIot() {
        val accessJson = entityValueServiceProvider.findValueByKey(AwsConnectionPropertiesEntities.getKey("openapi.access_key"));
        val secretJson = entityValueServiceProvider.findValueByKey(AwsConnectionPropertiesEntities.getKey("openapi.secret_key"));
        AwsIotProperties awsIotProperties = new AwsIotProperties();
        awsIotProperties.setAccessKey(accessJson.asText());
        awsIotProperties.setSecretKey(secretJson.asText());
        if (StrUtil.isEmpty(awsIotProperties.getAccessKey()) || StrUtil.isEmpty(awsIotProperties.getSecretKey())) {
            return;
        }
        if (ACCESS_KEY.equals(awsIotProperties.getAccessKey()) || SECRET_KEY.equals(awsIotProperties.getSecretKey())) {
            return;
        }
        try {
            ACCESS_KEY = awsIotProperties.getAccessKey();
            SECRET_KEY = awsIotProperties.getSecretKey();
            awsIotClient = new AwsIotClient(awsIotProperties);
        } catch (Exception e) {
            log.error("aws iot client init error: {}", e.getMessage());
        }
    }

    @Override
    public String createLoraDevice(AwsServiceEntities.AddDevice addDevice) throws Exception {
        awsIot();
        String sn = addDevice.getSn();
        String devEui = addDevice.getDevEui();
        String devAppKey = addDevice.getDevAppKey() == null ? DEV_APP_KEY : addDevice.getDevAppKey();
        // 读取配置文件信息
//        RfRegion rfRegion = addDevice.getRfRegion() !=null ? RF_REGION[Integer.parseInt(addDevice.getRfRegion())] : RF_REGION[1];
//        LoraClass loraClass = addDevice.getLoraClass() !=null ? LORA_CLASS[Integer.parseInt(addDevice.getLoraClass())] : LORA_CLASS[0];
//        String deviceProfileId = deviceProfileProperties.getDeviceProfileId(rfRegion, loraClass);
//        String serviceProfileId = serviceProfileProperties.getActiveServiceProfileId();
        String destinationName = destinationProperties.getName();
        CreateWirelessDeviceResponse createWirelessDeviceResponse = awsIotClient.createWirelessDevice(sn, devEui, devAppKey, DEVICE_PROFILE_ID, SERVICE_PROFILE_ID, destinationName);
        return createWirelessDeviceResponse.id();
    }

    @Override
    public DeleteWirelessDeviceResponse deleteWirelessDevice(String wirelessDeviceId) throws Exception {
        awsIot();
        DeleteWirelessDeviceResponse deleteWirelessDeviceResponse = awsIotClient.deleteWirelessDevice(wirelessDeviceId);
        return deleteWirelessDeviceResponse;
    }

    @Override
    public List<WirelessDeviceStatistics> listWirelessDevices(String serviceProfileId, String destinationName,
                                                              String multicastGroupId, String fuotaTaskId) throws Exception {
        awsIot();
        return awsIotClient.listWirelessDevices(serviceProfileId, destinationName, multicastGroupId, fuotaTaskId);
    }

    @Override
    public SendDataToWirelessDeviceResponse sendDataToWirelessDevice(String wirelessDeviceId, String payload) throws Exception {
        awsIot();
        return awsIotClient.sendDataToWirelessDevice(wirelessDeviceId, payload);
    }
}
