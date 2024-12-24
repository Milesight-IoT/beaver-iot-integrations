package com.milesight.beaveriot.vms.service;

import cn.hutool.core.lang.Assert;
import com.milesight.beaveriot.common.entity.IntegrationDeviceBuilder;
import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.api.ExchangeFlowExecutor;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.context.integration.model.EntityBuilder;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.integration.model.event.ExchangeEvent;
import com.milesight.beaveriot.eventbus.annotations.EventSubscribe;
import com.milesight.beaveriot.eventbus.api.Event;
import com.milesight.beaveriot.eventbus.api.EventResponse;
import com.milesight.beaveriot.vms.api.HuggingfaceApi;
import com.milesight.beaveriot.vms.api.VmsApi;
import com.milesight.beaveriot.vms.constants.VmsConstants;
import com.milesight.beaveriot.vms.entity.VmsDeviceEntities;
import com.milesight.beaveriot.vms.entity.VmsIntegrationEntities;
import com.milesight.beaveriot.vms.model.VmsDeviceListResponse;
import com.milesight.beaveriot.vms.model.VmsStatusListResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @Author yuanh
 * @Description
 * @Package com.milesight.beaveriot.vms.service
 * @Date 2024/11/22 14:10
 */
@Service
@Slf4j
public class VmsService {
    @Autowired
    private DeviceServiceProvider deviceServiceProvider;
    @Autowired
    private ExchangeFlowExecutor exchangeFlowExecutor;

    @EventSubscribe(payloadKeyExpression = VmsConstants.INTEGRATION_ID + ".integration." + VmsConstants.Entity.ADD_DEVICE + ".*", eventType = ExchangeEvent.EventType.DOWN)
    public void onAddDevice(Event<VmsIntegrationEntities.AddDevice> event) {
        String deviceName = event.getPayload().getContext("device_name", "VMS Device");
        VmsIntegrationEntities.AddDevice payload = event.getPayload();
        String deviceId = payload.getDeviceId();
        addDevice(deviceName, deviceId);
    }

    @EventSubscribe(payloadKeyExpression = VmsConstants.INTEGRATION_ID + ".integration." + VmsConstants.Entity.DELETE_DEVICE, eventType = ExchangeEvent.EventType.DOWN)
    public void onDeleteDevice(Event<ExchangePayload> event) {
        Device device = (Device) event.getPayload().getContext("device");
        deviceServiceProvider.deleteById(device.getId());
    }

    @SneakyThrows
    @EventSubscribe(payloadKeyExpression = VmsConstants.INTEGRATION_ID + ".integration." + VmsConstants.Entity.BENCHMARK, eventType = ExchangeEvent.EventType.DOWN)
    public void doBenchmark(Event<VmsIntegrationEntities> event) {
        String detectStatusKey = VmsConstants.INTEGRATION_ID + ".integration." + VmsConstants.Entity.DETECT_STATUS;
        try {
            exchangeFlowExecutor.syncExchangeDown(new ExchangePayload(Map.of(detectStatusKey, VmsIntegrationEntities.DetectStatus.DETECTING.ordinal())));
            syncDeviceList();
        } catch (Exception e) {
            log.error("[Benchmark Error] " + e);
            throw new RuntimeException(e);
        } finally {
            // mark benchmark done
            ExchangePayload donePayload = new ExchangePayload();
            donePayload.put(detectStatusKey, VmsIntegrationEntities.DetectStatus.STANDBY.ordinal());
            exchangeFlowExecutor.syncExchangeUp(donePayload);
        }
    }

    @SneakyThrows
    @EventSubscribe(payloadKeyExpression = VmsConstants.INTEGRATION_ID + ".device.*." + VmsConstants.DeviceEntity.SYNC, eventType = ExchangeEvent.EventType.DOWN)
    public void sync(ExchangeEvent event) {
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
        String devId = device.getIdentifier();
        String session = VmsApi.getSession();
        Map<String, VmsStatusListResponse.StatusInfo> statusInfoMap = VmsApi.getStatusInfoMap(session);
        Boolean online = Optional.ofNullable(statusInfoMap.get(devId))
                .map(VmsStatusListResponse.StatusInfo::getOnline)
                .orElse(false);
        syncDeviceEntities(device, online);
    }

    @SneakyThrows
    @EventSubscribe(payloadKeyExpression = VmsConstants.INTEGRATION_ID + ".device.*." + VmsConstants.DeviceEntity.GET_HLS_URL + "*", eventType = ExchangeEvent.EventType.DOWN)
    public EventResponse getHlsUrl(ExchangeEvent event) {
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
            return new EventResponse();
        }
        val device = devices.get(0);
        String devId = device.getIdentifier();
        String session = VmsApi.getSession();
        String hlsUrl = VmsApi.getHlsUrl(session, devId);
        return EventResponse.of("hls_url", hlsUrl);
    }

    @SneakyThrows
    @EventSubscribe(payloadKeyExpression = VmsConstants.INTEGRATION_ID + ".device.*." + VmsConstants.DeviceEntity.GET_HLS_VOD_URL + "*", eventType = ExchangeEvent.EventType.DOWN)
    public EventResponse getHlsVodUrl(ExchangeEvent event) {
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
            return new EventResponse();
        }
        val device = devices.get(0);
        String devId = device.getIdentifier();
        Object o = exchangePayload.get(VmsConstants.INTEGRATION_ID + ".device." + devId + "." + VmsConstants.DeviceEntity.GET_HLS_VOD_URL + "." + VmsConstants.DeviceEntity.START_TIME);
        Long startTime = o == null ? null : Long.parseLong(o.toString());
        String hlsVodList = VmsApi.getHlsVodList(devId, startTime);
        return EventResponse.of("hls_vod_url", hlsVodList);
    }

    @EventSubscribe(payloadKeyExpression = VmsConstants.INTEGRATION_ID + ".integration." + VmsConstants.Entity.VMS_INFO + ".*", eventType = ExchangeEvent.EventType.DOWN)
    public void setApiInfo(Event<VmsIntegrationEntities.VmsInfo> event) {
        VmsApi.setVmsInfo(event.getPayload());
        HuggingfaceApi.setInfo(event.getPayload());
    }

    /**
     * 同步设备列表
     */
    public void syncDeviceList() {
        String session = VmsApi.getSession();
        List<VmsDeviceListResponse.DeviceInfo> deviceInfos = VmsApi.getDeviceInfoList(session);
        for (VmsDeviceListResponse.DeviceInfo deviceInfo : deviceInfos) {
            String identifier = deviceInfo.getDevId();
            String name = deviceInfo.getAddr();
            addDevice(name, identifier);
        }
        syncDevicesEntities();
    }

    /**
     * 批量同步设备实体
     */
    public void syncDevicesEntities() {
        List<Device> devices = deviceServiceProvider.findAll(VmsConstants.INTEGRATION_ID);
        if (devices.isEmpty()) {
            return;
        }
        String session = VmsApi.getSession();
        Map<String, VmsStatusListResponse.StatusInfo> statusInfoMap = VmsApi.getStatusInfoMap(session);
        for (Device device : devices) {
            String devId = device.getIdentifier();
            Boolean online = Optional.ofNullable(statusInfoMap.get(devId))
                    .map(VmsStatusListResponse.StatusInfo::getOnline)
                    .orElse(false);
            syncDeviceEntities(device, online);
        }
    }


    /**
     * 同步设备实体
     */
    public void syncDeviceEntities(Device device, Boolean online) {
        String devId = device.getIdentifier();
        Assert.notBlank(devId, "device identifier is null!");
        ExchangePayload weatherPayload = new ExchangePayload();
        device.getEntities().forEach(entity -> {
            if (entity.getIdentifier().equals(VmsConstants.DeviceEntity.ONLINE)) {
                weatherPayload.put(entity.getKey(), online);
            }
        });
        exchangeFlowExecutor.asyncExchangeDown(weatherPayload);
    }


    private void addDevice(String name, String identifier) {
        Entity vodStartTime = new EntityBuilder(VmsConstants.INTEGRATION_ID)
                .identifier(VmsConstants.DeviceEntity.START_TIME)
                .service(VmsConstants.DeviceEntity.START_TIME)
                .valueType(EntityValueType.STRING)
                .build();
        Entity getHlsVodUrl = new EntityBuilder(VmsConstants.INTEGRATION_ID)
                .identifier(VmsConstants.DeviceEntity.GET_HLS_VOD_URL)
                .service(VmsConstants.DeviceEntity.GET_HLS_VOD_URL)
                .valueType(EntityValueType.BOOLEAN)
                .children(vodStartTime)
                .build();
        Device device = new IntegrationDeviceBuilder(VmsConstants.INTEGRATION_ID)
                .name(name)
                .identifier(identifier)
                .build(VmsDeviceEntities.class);
        List<Entity> entities = device.getEntities();
        entities.add(getHlsVodUrl);
        device.setEntities(entities);
        deviceServiceProvider.save(device);
    }
}
