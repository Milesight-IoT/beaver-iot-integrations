package com.milesight.beaveriot.integration.wp.service;


import cn.hutool.json.JSONObject;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.api.ExchangeFlowExecutor;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.entity.repository.EntityHistoryRepository;
import com.milesight.beaveriot.integration.wp.entity.WpIntegrationEntities;
import com.milesight.beaveriot.integration.wp.model.WpMeeting;
import com.milesight.beaveriot.integration.wp.model.WpMeetingRequest;
import com.milesight.beaveriot.integration.wp.model.WpMeetingResponse;
import com.milesight.beaveriot.integration.wp.model.WpResponsePayload;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;


@Service
@Slf4j
public class WpMeetingRoomService {


    @Autowired
    private DeviceServiceProvider deviceServiceProvider;

    @Autowired
    private EntityValueServiceProvider entityValueServiceProvider;

    @Autowired
    private ExchangeFlowExecutor exchangeFlowExecutor;

    @Autowired
    private EntityHistoryRepository entityHistoryRepository;

    @Autowired
    private WpClient wpClient;


    public WpResponsePayload addConferenceRoom(WpIntegrationEntities.AddDevice addDevice, String deviceName) throws Exception {
        Integer memberCapacity = addDevice.getMemberCapacity();
        HttpResponse<String> response = wpClient.addConferenceRoom(deviceName, String.valueOf(memberCapacity));
        if (response.statusCode() == 200) {
            // 使用 Jackson 进行反序列化
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true);
            val wpResponsePayload = objectMapper.readValue(response.body(), WpResponsePayload.class);
            return wpResponsePayload;
        }
        return null;
    }

    public void deleteConferenceRoom(String deviceId) throws Exception {
        //String[]转int[]
        int[] intArray = new int[1];
        intArray[0] = Integer.parseInt(deviceId);
        wpClient.deleteConferenceRoom(intArray);
    }

    public WpResponsePayload allConferenceRoom() throws Exception {
        HttpResponse<String> response = wpClient.allConferenceRoom();
        if (response.statusCode() == 200) {
            // 使用 Jackson 进行反序列化
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true);
            WpResponsePayload wpResponsePayload = objectMapper.readValue(response.body(), WpResponsePayload.class);
            return wpResponsePayload;
        }
        return null;
    }

    public WpMeetingResponse addMeetingRoom(WpMeetingRequest wpMeetingRequest, WpMeeting wpMeeting) {
        WpMeetingResponse wpMeetingResponse = new WpMeetingResponse();
        String id = "";
        try {
            if (wpMeetingRequest.getType() == 0) {
                wpMeetingResponse = addMeeting(wpMeeting);
            } else if (wpMeetingRequest.getType() == 1) {
                wpMeetingResponse = updateMeeting(wpMeeting);
            } else if (wpMeetingRequest.getType() == 2) {
                wpMeetingResponse = deleteMeeting(wpMeeting);
            }
        } catch (Exception e) {
            log.error("addMeeting error", e);
        }
        if (wpMeeting == null) {
            wpMeeting = WpMeeting.builder()
                    .subject(wpMeetingRequest.getSubject())
                    .meetingRoomId(wpMeetingRequest.getKey())
                    .firstStartTime(wpMeetingRequest.getFirst())
                    .lastEndTime(wpMeetingRequest.getLast())
                    .startDate(wpMeetingRequest.getDate())
                    .startTime(wpMeetingRequest.getTime())
                    .meetingId(wpMeetingRequest.getMeeting())
                    .build();
        } else {
            wpMeeting.setMeetingId(id);
        }
        wpMeeting.setCreateTime(System.currentTimeMillis());
        if (wpMeetingRequest.getType() == 2) {
            wpMeeting.setSubject(null);
        }
        Map<String, Object> result = new HashMap<String, Object>();
        result.put(wpMeeting.getMeetingRoomId(), wpMeeting.toString());
        entityValueServiceProvider.saveHistoryRecord(ExchangePayload.create(result), Long.parseLong(wpMeeting.getFirstStartTime()) * 1000);
        return wpMeetingResponse;
    }

    public WpMeetingResponse addMeeting(WpMeeting wpMeeting) {
        WpMeetingResponse result = WpMeetingResponse.builder().build();
        HttpResponse<String> data = null;
        try {
            data = wpClient.addMeeting(wpMeeting);
            // 解析 JSON 响应
            JSONObject jsonObject = new JSONObject(data.body());
            // 提取 "data" 对象
            JSONObject dataObject = jsonObject.getJSONObject("data");
            if (dataObject == null) {
                return null;
            }
            result.setCode(dataObject.getInt("code"));
            result.setMessage(String.valueOf(dataObject.getInt("id")));
        } catch (Exception e) {
            result.setCode(500);
        }
        return result;
    }

    public WpMeetingResponse updateMeeting(WpMeeting wpMeeting) {
        WpMeetingResponse result = WpMeetingResponse.builder().build();
        HttpResponse<String> data = null;
        try {
            data = wpClient.updateMeeting(wpMeeting);
            // 解析 JSON 响应
            JSONObject jsonObject = new JSONObject(data.body());
            // 提取 "data" 对象
            JSONObject dataObject = jsonObject.getJSONObject("data");
            if (dataObject == null) {
                return null;
            }
            result.setCode(dataObject.getInt("code"));
            result.setMessage(String.valueOf(dataObject.getInt("id")));
        } catch (Exception e) {
            result.setCode(500);
        }
        return result;
    }

    public WpMeetingResponse deleteMeeting(WpMeeting wpMeeting) {
        WpMeetingResponse result = WpMeetingResponse.builder().build();
        HttpResponse<String> data = null;
        try {
            data = wpClient.deleteMeeting(wpMeeting);
            // 解析 JSON 响应
            JSONObject jsonObject = new JSONObject(data.body());
            // 提取 "data" 对象
            JSONObject dataObject = jsonObject.getJSONObject("data");
            if (dataObject == null) {
                return null;
            }
            result.setCode(dataObject.getInt("code"));
            result.setMessage(String.valueOf(dataObject.getInt("id")));
        } catch (Exception e) {
            result.setCode(500);
        }
        return result;
    }
}
