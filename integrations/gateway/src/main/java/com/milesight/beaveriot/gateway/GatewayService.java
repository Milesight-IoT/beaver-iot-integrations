package com.milesight.beaveriot.gateway;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.context.integration.model.*;
import com.milesight.beaveriot.context.integration.model.event.ExchangeEvent;
import com.milesight.beaveriot.eventbus.annotations.EventSubscribe;
import com.milesight.beaveriot.eventbus.api.Event;
import com.milesight.beaveriot.gateway.handle.DeviceDataHandle;
import com.milesight.beaveriot.gateway.handle.MqttDataHandle;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Slf4j
public class GatewayService {
    @Autowired
    private DeviceServiceProvider deviceServiceProvider;

    @Autowired
    private DeviceDataHandle deviceDataHandle;

    @Autowired
    private MqttDataHandle mqttDataHandle;

    @SneakyThrows
    @EventSubscribe(payloadKeyExpression = GatewayConstants.INTEGRATION_ID + ".device.*", eventType = ExchangeEvent.EventType.DOWN)
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
        Map<String, Object> content = new HashMap<>();
        content.put("devEUI", device.getIdentifier());
        content.put("data", Map.of("reboot", true));
        mqttDataHandle.mqttPublist(JSONUtil.toJsonStr(content));
    }

    @Transactional
    @EventSubscribe(payloadKeyExpression = GatewayConstants.INTEGRATION_ID + ".integration.add_device.*", eventType = ExchangeEvent.EventType.DOWN)
    public void onAddDevice(Event<GatewayIntegrationEntities.AddDevice> event) throws Exception {
        GatewayIntegrationEntities.AddDevice payload = event.getPayload();
        String deviceName = payload.getContext("device_name", "Device Name");
        Map<String, Object> additional = new HashMap<>();
        additional.put(GatewayIntegrationEntities.AddDevice.Fields.devEUI, payload.getDevEUI());
        additional.put(GatewayIntegrationEntities.AddDevice.Fields.applicationName, GatewayIntegrationEntities.DefaultApplicationName.JYX_TEST.name());
        additional.put(GatewayIntegrationEntities.AddDevice.Fields.devAddr, payload.getDevAddr());
        additional.put(GatewayIntegrationEntities.AddDevice.Fields.payloadCodecID, payload.getPayloadCodecID());

        Device device = new DeviceBuilder(GatewayConstants.INTEGRATION_ID)
                .name(deviceName)
                .identifier(payload.getDevEUI().toUpperCase())
                .additional(additional)
                .entity(()->{
                    return new EntityBuilder(GatewayConstants.INTEGRATION_ID)
                            .identifier("reboot")
                            .service("Reboot")
                            .valueType(EntityValueType.BOOLEAN)
                            .attributes(new HashMap<>())
                            .build();
                })
                .build();

        deviceServiceProvider.save(device);
        deviceDataHandle.createDevice(additional);
    }

    @Transactional
    @EventSubscribe(payloadKeyExpression = GatewayConstants.INTEGRATION_ID + ".integration.gateway.*", eventType = ExchangeEvent.EventType.DOWN)
    public void onGatewayIdUpdated(Event<GatewayIntegrationEntities.Gateway> event) throws Exception {
        // 调用获取codec接口
        GatewayIntegrationEntities.Gateway payload = event.getPayload();
        if (StrUtil.isBlank(payload.getGatewayIP()) || StrUtil.isBlank(payload.getUsername()) || StrUtil.isBlank(payload.getPassword())) {
            return;
        }
        // 拉取所有payloadCodecContent
        mqttDataHandle.getAllPayloadCodecContent();
    }


    @Transactional
    @EventSubscribe(payloadKeyExpression = GatewayConstants.INTEGRATION_ID + ".integration.delete_device", eventType = ExchangeEvent.EventType.DOWN)
    public void onDeleteDevice(Event<ExchangePayload> event) throws Exception {
        Device device = (Device) event.getPayload().getContext("device");
        deviceServiceProvider.deleteById(device.getId());
        if (MapUtil.isNotEmpty(device.getAdditional())) {
            String devEUI = device.getAdditional().get(GatewayIntegrationEntities.AddDevice.Fields.devEUI).toString();
            deviceDataHandle.deleteDevice(devEUI);
        }
    }

    @EventSubscribe(payloadKeyExpression = GatewayConstants.INTEGRATION_ID + ".integration.benchmark", eventType = ExchangeEvent.EventType.DOWN)
    @Async
    public void benchmark(Event<GatewayIntegrationEntities> event) throws Exception {
        // 拉取所有payloadCodecContent
        mqttDataHandle.getAllPayloadCodecContent();
        // 拉取设备数据
        pullDeviceData();
    }

    /**
     * Pull device data from the database and create devices
     */
    public void pullDeviceData() throws Exception {
        JSONArray jsonArray = deviceDataHandle.queryDevice();
        if (null == jsonArray || jsonArray.isEmpty()) {
            return;
        }
        List<String> successDevEuis = new ArrayList<>();
        for (Object obj : jsonArray) {
            try {
                JSONObject entries = JSONUtil.parseObj(obj);
                Object devEUIObj = entries.get(GatewayIntegrationEntities.AddDevice.Fields.devEUI);
                if (null == devEUIObj) {
                    continue;
                }
                Device byKey = deviceServiceProvider.findByKey(GatewayConstants.INTEGRATION_ID + ".device." + devEUIObj);
                if (null != byKey) {
                    log.debug("device already exists: " + byKey.getIdentifier());
                    continue;
                }
                Device device = new DeviceBuilder(GatewayConstants.INTEGRATION_ID)
                        .name(entries.getOrDefault("name", "Device Name").toString())
                        .identifier(devEUIObj.toString().toUpperCase())
                        .additional(BeanUtil.beanToMap(obj))
                        .entity(()->{
                            return new EntityBuilder(GatewayConstants.INTEGRATION_ID)
                                    .identifier("reboot")
                                    .service("Reboot")
                                    .valueType(EntityValueType.BOOLEAN)
                                    .attributes(new HashMap<>())
                                    .build();
                        })
                        .build();
                successDevEuis.add(devEUIObj.toString());
                deviceServiceProvider.save(device);
            } catch (Exception e) {
                log.error("benchmark obj:{} error: {}", JSONUtil.toJsonStr(obj), e.getMessage());
            }
        }
        Map<String, String> config = deviceDataHandle.getConfig();
        String webhookUrl = config.get(GatewayIntegrationEntities.Gateway.Fields.webhookUrl);
        if (!successDevEuis.isEmpty()) {
            deviceDataHandle.pushRobotData(webhookUrl, "同步网关设备成功", "成功devEUI:" + String.join(",", successDevEuis), null);
        } else {
            deviceDataHandle.pushRobotData(webhookUrl,"同步网关设备失败", "无可同步设备", null);
        }
    }

}
