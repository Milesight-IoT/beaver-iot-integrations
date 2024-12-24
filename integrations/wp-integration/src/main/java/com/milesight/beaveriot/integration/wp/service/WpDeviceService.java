package com.milesight.beaveriot.integration.wp.service;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.constants.IntegrationConstants;
import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.context.integration.model.*;
import com.milesight.beaveriot.context.integration.model.event.ExchangeEvent;
import com.milesight.beaveriot.eventbus.annotations.EventSubscribe;
import com.milesight.beaveriot.eventbus.api.Event;
import com.milesight.beaveriot.integration.wp.constant.WpIntegrationConstants;
import com.milesight.beaveriot.integration.wp.entity.WpIntegrationEntities;
import com.milesight.beaveriot.integration.wp.model.WpMeeting;
import com.milesight.beaveriot.integration.wp.model.WpMeetingRequest;
import com.milesight.beaveriot.integration.wp.model.WpResponsePayload;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;


@Service
@Slf4j
public class WpDeviceService {

    @Autowired
    private DeviceServiceProvider deviceServiceProvider;

    @Autowired
    private WpMeetingRoomService wpMeetingRoomService;

    @SneakyThrows
    @EventSubscribe(payloadKeyExpression = "wp-integration.device.*", eventType = ExchangeEvent.EventType.DOWN)
    public void onDeviceExchangeEvent(ExchangeEvent event) {
        val exchangePayload = event.getPayload();
    }

    @SneakyThrows
    @EventSubscribe(payloadKeyExpression = "wp-integration.integration.sync_device", eventType = ExchangeEvent.EventType.DOWN)
    public void onSyncDevice(Event<WpIntegrationEntities.SyncDevice> event) {
        try {

            val wpResponsePayload = wpMeetingRoomService.allConferenceRoom();
            if (wpResponsePayload == null) {
                return;
            }
            String integrationId = WpIntegrationConstants.INTEGRATION_IDENTIFIER;
            val data = wpResponsePayload.getData();
            ArrayNode arrayNode = (ArrayNode) data.get("list");
            arrayNode.forEach(node -> {
                val identifier = node.get("id").asText();
                val deviceName = node.get("name").asText();
                val memberCapacity = node.get("memberCapacity").asInt();
                ArrayNode schedules = (ArrayNode) node.get("schedules");
                // 添加设备
                addMeetingDevice(deviceName, memberCapacity, identifier);
                if (schedules.size() != 0) {
                    val deviceKey = IntegrationConstants.formatIntegrationDeviceKey(integrationId, identifier);
                    // 使用 Jackson 进行反序列化
                    ObjectMapper objectMapper = new ObjectMapper();
                    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                    objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true);
                    List<WpMeeting> wpMeetings = objectMapper.convertValue(schedules, List.class);
                    wpMeetings.forEach(meeting -> {
                        // 保存预约的会议历史数据
                        try {
                            wpMeetingRoomService.addMeetingRoom(WpMeetingRequest.builder().type(0).build(), meeting);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
                }

            });
        } catch (Exception e) {
            log.error("Error in onSyncDevice", e);
        }
    }

    @SneakyThrows
    @EventSubscribe(payloadKeyExpression = "wp-integration.integration.add_device.*", eventType = ExchangeEvent.EventType.DOWN)
    public void onAddDevice(Event<WpIntegrationEntities.AddDevice> event) {
        val deviceName = event.getPayload().getContext("device_name", "Device Name");
        WpIntegrationEntities.AddDevice addDevice = event.getPayload();
        Integer memberCapacity = addDevice.getMemberCapacity();

        WpResponsePayload wpResponsePayload = wpMeetingRoomService.addConferenceRoom(addDevice, deviceName);
        if (wpResponsePayload == null) {
            log.error("Error in addConferenceRoom");
            return;
        }
        val identifier = wpResponsePayload.getData().get("id").asText();
        addMeetingDevice(deviceName, memberCapacity, identifier);
    }

    private void addMeetingDevice(String deviceName, Integer memberCapacity, String identifier) {
        List<Entity> entities = new ArrayList<>();
        String integrationId = WpIntegrationConstants.INTEGRATION_IDENTIFIER;
        val deviceKey = IntegrationConstants.formatIntegrationDeviceKey(integrationId, identifier);
        WpMeeting wpMeeting = WpMeeting.builder()
                .meetingRoomId(identifier)
                .build();
        // 添加额外实体
        addAdditionalEntities(deviceKey, entities, wpMeeting);
        // 添加设备
        addDevice(deviceName, memberCapacity, identifier, entities);
    }

    private static void addAdditionalEntities(String deviceKey, List<Entity> entities, WpMeeting wpMeeting) {
        val integrationId = WpIntegrationConstants.INTEGRATION_IDENTIFIER;

        Map<String, Object> attributes = new HashMap<>();
        if (wpMeeting != null) {
            // 遍历wpSchedule的所有属性，把属性值
            for (Map.Entry<String, Object> entry : JsonUtils.toMap(wpMeeting).entrySet()) {
                val key = entry.getKey();
                val value = entry.getValue();
                attributes.put(key, value);

            }
        }
        entities.add(new EntityBuilder(integrationId, deviceKey)
                .identifier("schedule")
                .property("schedule", AccessMod.RW)
                .valueType(EntityValueType.STRING)
                .attributes(attributes)
                .build());
        entities.add(new EntityBuilder(integrationId, deviceKey)
                .identifier(WpIntegrationConstants.InternalPropertyIdentifier.LAST_SYNC_TIME)
                .property(WpIntegrationConstants.InternalPropertyIdentifier.LAST_SYNC_TIME, AccessMod.R)
                .valueType(EntityValueType.LONG)
                .attributes(Map.of("internal", true))
                .build());
    }

    private void addDevice(String deviceName, Integer memberCapacity, String identifier, List<Entity> entities) {
        val integrationId = WpIntegrationConstants.INTEGRATION_IDENTIFIER;
        val device = new DeviceBuilder(integrationId)
                .name(deviceName)
                .identifier(identifier)
                .additional(Map.of("memberCapacity", memberCapacity))
                .entities(entities)
                .build();
        deviceServiceProvider.save(device);
    }

    @SneakyThrows
    @EventSubscribe(payloadKeyExpression = "wp-integration.integration.delete_device", eventType = ExchangeEvent.EventType.DOWN)
    public void onDeleteDevice(Event<WpIntegrationEntities.DeleteDevice> event) {
        val device = deviceServiceProvider.findByIdentifier(
                ((Device) event.getPayload().getContext("device")).getIdentifier(), WpIntegrationConstants.INTEGRATION_IDENTIFIER);
        val deviceId = device.getIdentifier();
        wpMeetingRoomService.deleteConferenceRoom(deviceId);
        deviceServiceProvider.deleteById(device.getId());
    }

}


