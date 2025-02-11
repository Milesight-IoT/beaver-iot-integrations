package com.milesight.beaveriot.integration.aws.service;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.constants.IntegrationConstants;
import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.context.integration.model.*;
import com.milesight.beaveriot.context.integration.model.event.ExchangeEvent;
import com.milesight.beaveriot.eventbus.annotations.EventSubscribe;
import com.milesight.beaveriot.eventbus.api.Event;
import com.milesight.beaveriot.integration.aws.constant.AwsIntegrationConstants;
import com.milesight.beaveriot.integration.aws.entity.AwsServiceEntities;
import com.milesight.beaveriot.integration.aws.sdk.service.IIotStrategy;
import com.milesight.beaveriot.integration.aws.util.AwsTslUtils;
import com.milesight.beaveriot.integration.aws.util.WeChatMessageUtils;
import com.milesight.beaveriot.parser.ParserPlugIn;
import com.milesight.beaveriot.parser.model.ParserPayload;
import com.milesight.cloud.sdk.client.model.ThingSpec;
import com.milesight.msc.sdk.error.MscApiException;
import com.milesight.msc.sdk.error.MscSdkException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


@Slf4j
@Service
public class AwsDeviceService {

    @Autowired
    private DeviceServiceProvider deviceServiceProvider;

    @Autowired
    private IIotStrategy iotStrategy;

    @Autowired
    private ParserPlugIn parserPlugIn;

    @Autowired
    private WebhookPushService webhookPushService;

    @SneakyThrows
    @EventSubscribe(payloadKeyExpression = "aws-iot-integration.device.*", eventType = ExchangeEvent.EventType.DOWN)
    public void onDeviceExchangeEvent(ExchangeEvent event) {
        val exchangePayload = event.getPayload();
        val devices = exchangePayload.getExchangeEntities()
                .values()
                .stream()
                .map(Entity::getDeviceKey)
                .distinct()
                .map(deviceServiceProvider::findByKey)
                .filter(Objects::nonNull)
                .toList();
        if (devices.size() != 1) {
            log.warn("Invalid device number: {}", devices.size());
            return;
        }
        val device = devices.get(0);
        // 处理数据
        dataHandle(device, exchangePayload);
    }

    /**
     * 处理数据
     *
     * @param device
     * @param exchangePayload
     */
    @SneakyThrows
    private void dataHandle(Device device, ExchangePayload exchangePayload) {
        val objectMapper = new ObjectMapper();
        val sn = (String) device.getAdditional().get(AwsIntegrationConstants.DeviceAdditionalDataName.DEVICE_SN);
        Map<String, Object> allPayloads = exchangePayload.getAllPayloads();
        Map<String, Object> payloadMap = new HashMap<>();
        allPayloads.forEach((key, value) -> {
            String keyWithoutPrefix = String.format("%s.%s.%s.", AwsIntegrationConstants.INTEGRATION_IDENTIFIER, "device", device.getIdentifier());
            String propertyKey = key.replaceAll(keyWithoutPrefix, "");
            payloadMap.put(propertyKey, value);
        });
        // 将 Map 转换为 JsonNode
        JsonNode jsonNode = objectMapper.valueToTree(payloadMap);
        ParserPayload parserPayload = ParserPayload.builder()
                .sn(sn)
                .jsonData(jsonNode)
                .build();
        parserPlugIn.encode(parserPayload);
        val deviceId = device.getIdentifier();
        try {
            if (CharSequenceUtil.isNotEmpty(deviceId) && CharSequenceUtil.isNotEmpty(parserPayload.getIpsoData())) {
                iotStrategy.sendDataToWirelessDevice(deviceId, parserPayload.getIpsoData());
            }
        } catch (MscApiException e) {
            log.error("Send data to aws error: {}", e.getMessage());
        }
    }

    @SneakyThrows
    @EventSubscribe(payloadKeyExpression = "aws-iot-integration.integration.add_device.*", eventType = ExchangeEvent.EventType.DOWN)
    public void onAddDevice(Event<AwsServiceEntities.AddDevice> event) {
        val deviceName = event.getPayload().getContext("device_name", "Device Name");
        AwsServiceEntities.AddDevice device = event.getPayload();
        String sn = device.getSn();
        val product = parserPlugIn.getProductBySn(sn);
        if (product == null) {
            log.error("Product not found");
            throw new MscSdkException("Product not found");
        }
        // aws 添加设备
        val deviceId = iotStrategy.createLoraDevice(device);
        // 物模型
        val thingSpecs = product.getThingSpecifications();
        if (thingSpecs == null || thingSpecs.size() == 0) {
            log.error("Product thing spec not found");
            throw new MscSdkException("Product thing spec not found");
        }
        // 使用 Jackson 进行反序列化
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true);
        ThingSpec thingSpec = objectMapper.convertValue(thingSpecs.get(0), ThingSpec.class);
        addLocalDevice(deviceId, deviceName, sn, device, thingSpec);
    }

    public Device addLocalDevice(String identifier, String deviceName, String sn, AwsServiceEntities.AddDevice addDevice, ThingSpec thingSpec) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("device_name", deviceName);
        if (addDevice == null) {
            map.put(AwsServiceEntities.AddDevice.Fields.sn, sn);
        } else {
            map.put(AwsServiceEntities.AddDevice.Fields.sn, sn);
            map.put(AwsServiceEntities.AddDevice.Fields.devEui, addDevice.getDevEui());
            map.put(AwsServiceEntities.AddDevice.Fields.devAppKey, addDevice.getDevAppKey());
            map.put(AwsServiceEntities.AddDevice.Fields.rfRegion, addDevice.getRfRegion());
            map.put(AwsServiceEntities.AddDevice.Fields.loraClass, addDevice.getLoraClass());
        }

        val integrationId = AwsIntegrationConstants.INTEGRATION_IDENTIFIER;
        val deviceKey = IntegrationConstants.formatIntegrationDeviceKey(integrationId, identifier);
        val entities = AwsTslUtils.thingSpecificationToEntities(integrationId, deviceKey, thingSpec);
        addAdditionalEntities(integrationId, deviceKey, entities);

        val device = new DeviceBuilder(integrationId)
                .name(deviceName)
                .identifier(identifier)
                .additional(map)
                .entities(entities)
                .build();
        deviceServiceProvider.save(device);
        try {
            webhookPushService.webhookPush(WeChatMessageUtils.createAddDeviceMessage(device.getName(), sn));
            webhookPushService.alarmPush(WeChatMessageUtils.createAddDeviceAlarmMessage(device.getName(), sn));
        } catch (Exception e) {
            log.error("Webhook push error", e);
        }
        return device;
    }

    public Device updateLocalDevice(String identifier, String deviceId, ThingSpec thingSpec) {
        val integrationId = AwsIntegrationConstants.INTEGRATION_IDENTIFIER;
        val deviceKey = IntegrationConstants.formatIntegrationDeviceKey(integrationId, identifier);
        val entities = AwsTslUtils.thingSpecificationToEntities(integrationId, deviceKey, thingSpec);
        // 添加额外属性
        addAdditionalEntities(integrationId, deviceKey, entities);
        val device = deviceServiceProvider.findByIdentifier(identifier, integrationId);
        device.setAdditional(Map.of(AwsIntegrationConstants.DeviceAdditionalDataName.DEVICE_SN, deviceId));
        device.setEntities(entities);
        deviceServiceProvider.save(device);
        return device;
    }

    private static void addAdditionalEntities(String integrationId, String deviceKey, List<Entity> entities) {
        entities.add(new EntityBuilder(integrationId, deviceKey)
                .identifier(AwsIntegrationConstants.InternalPropertyIdentifier.LAST_SYNC_TIME)
                .property(AwsIntegrationConstants.InternalPropertyIdentifier.LAST_SYNC_TIME, AccessMod.R)
                .valueType(EntityValueType.LONG)
                .attributes(Map.of("internal", true))
                .build());
    }

    @SneakyThrows
    @EventSubscribe(payloadKeyExpression = "aws-iot-integration.integration.delete_device", eventType = ExchangeEvent.EventType.DOWN)
    public void onDeleteDevice(Event<AwsServiceEntities.DeleteDevice> event) {
        val device = deviceServiceProvider.findByIdentifier(
                ((Device) event.getPayload().getContext("device")).getIdentifier(), AwsIntegrationConstants.INTEGRATION_IDENTIFIER);
        val additionalData = device.getAdditional();
        if (additionalData == null) {
            return;
        }
        val sn = additionalData.get(AwsIntegrationConstants.DeviceAdditionalDataName.DEVICE_SN);
        if (sn == null) {
            return;
        }
        val deviceId = device.getIdentifier();
        try {
            iotStrategy.deleteWirelessDevice(deviceId);
        } catch (MscApiException e) {
            if (!"device_not_found".equals(e.getErrorResponse().getErrCode())) {
                throw e;
            } else {
                log.warn("Device '{}' ({}) not found in MSC", device.getIdentifier(), sn);
            }
        }
        deviceServiceProvider.deleteById(device.getId());
        try {
            webhookPushService.webhookPush(WeChatMessageUtils.createDeleteDeviceMessage(device.getName(), sn.toString()));
            webhookPushService.alarmPush(WeChatMessageUtils.createDeleteDeviceAlarmMessage(device.getName(), sn.toString()));
        } catch (Exception ex) {
            log.warn("Device '{}' ({}) not found in MSC", device.getIdentifier(), sn);
        }
    }

}
