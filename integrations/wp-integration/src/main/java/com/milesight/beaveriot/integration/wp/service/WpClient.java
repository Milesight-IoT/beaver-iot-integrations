package com.milesight.beaveriot.integration.wp.service;


import cn.hutool.json.JSONObject;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.integration.wp.constant.WpIntegrationConstants;
import com.milesight.beaveriot.integration.wp.model.WpMeeting;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class WpClient {


    @Autowired
    private EntityValueServiceProvider entityValueServiceProvider;

    @Autowired
    private EntityServiceProvider entityServiceProvider;

    private static String COOKIE;

    public void getToken() throws Exception {
        val username = entityValueServiceProvider.findValueByKey(WpIntegrationConstants.getKey("openapi.username"));
        val password = entityValueServiceProvider.findValueByKey(WpIntegrationConstants.getKey("openapi.password"));
        if (username == null) {
            return;
        }
        // 创建 HttpClient 实例
        HttpClient client = HttpClient.newHttpClient();
        // 创建请求体 JSON 字符串
        String jsonInputString = String.format("{\"password\":\"%s\",\"username\":\"%s\",\"terminalType\":\"web\"}", password.asText(), username.asText());

        // 创建 HttpRequest 实例
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("https://workplace.yeastar.cn/services/oauth/token"))
                .header("User-Agent", "Apifox/1.0.0 (https://apifox.com)")
                .header("Content-Type", "application/json")
                .header("Accept", "*/*")
                .header("Cookie", "Authorization2=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1aWQiOjc4OTAyLCJ2ZXIiOjE3MzI2MjI4OTgsImdyYW50X3R5cGUiOiJwYXNzd29yZCIsInVzZXJfbmFtZSI6Imxpbnp5QG1pbGVzaWdodC5jb20iLCJwd2RfdmVyIjowLCJ0ZXJtaW5hbCI6IndlYiIsInJlZ2lvbiI6IkNOIiwianRpIjoiMjA0N2VmMjYtNmE1Zi00NmE5LTg0MTMtZjAxMTUyYjBkZmJjIiwiY2xpZW50X2lkIjoid2ViR2F0ZXdheSIsInRzIjoxNzMyNjIyODk4LCJleHBfaW4iOjI1OTIwMDB9.D6w5YSA4QGFdz8gfNRsjDdF46qOo8-ouOM0GHJx3ToQ")
                .POST(HttpRequest.BodyPublishers.ofString(jsonInputString))
                .build();

        // 发送请求并获取响应
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // 获取响应头中的 Set-Cookie
        Map<String, List<String>> headers = response.headers().map();
        List<String> cookiesHeader = headers.get("Set-Cookie");
        COOKIE = cookiesHeader.get(0);
    }

    public HttpResponse<String> addConferenceRoom(String name, String memberCapacity) throws Exception {
        getToken();
        // 请求体
        String jsonPayload = "{"
                + "\"name\": \"" + name + "\","
                + "\"buildingId\": 1300,"
                + "\"facilityIds\": [6864, 6865, 6866, 6867, 6868],"
                + "\"type\": \"common\","
                + "\"restrictInWorkingHours\": true,"
                + "\"minDuration\": 30,"
                + "\"maxDuration\": 180,"
                + "\"allowToBookBefore\": 365,"
                + "\"repeatable\": true,"
                + "\"checkInRequired\": true,"
                + "\"checkInNoticeTime\": 5,"
                + "\"checkInPermission\": \"Only Organizer\","
                + "\"autoRelease\": true,"
                + "\"autoReleaseTime\": 10,"
                + "\"checkOutPermission\": \"Only Organizer\","
                + "\"checkInStatistics\": false,"
                + "\"deviceIds\": [],"
                + "\"displayBooking\": true,"
                + "\"lightOnCheckIn\": false,"
                + "\"lightOffCheckOut\": false,"
                + "\"lightOnOffWork\": false,"
                + "\"automaticCheckInWhenPeopleDetected\": 0,"
                + "\"automaticExtendWhenPeopleDetected\": 0,"
                + "\"autoCheckOut\": true,"
                + "\"autoCheckOutDuration\": 15,"
                + "\"userIds\": [],"
                + "\"departmentIds\": [1009324],"
                + "\"memberCapacity\": \"" + memberCapacity + "\","
                + "\"enterpriseId\": \"101741\""
                + "}";
        String url = "https://workplace.yeastar.cn/services/manager/conference/api/v1/meeting_rooms";
        // 发送请求
        return sendPostRequest(jsonPayload, url);

    }

    public void deleteConferenceRoom(int[] deviceIds) throws Exception {
        getToken();
        String jsonPayload = "{\"ids\":" + Arrays.toString(deviceIds) + ",\"enterpriseId\":\"101741\"}";
        String url = "https://workplace.yeastar.cn/services/manager/conference/api/v1/meeting_rooms";
        sendPostRequest(jsonPayload, url);
    }


    public HttpResponse<String> allConferenceRoom() throws Exception {
        getToken();
        String url = "https://workplace.yeastar.cn/services/manager/conference/api/v1/meeting_rooms?enterpriseId=101741&pageSize=10&pageNumber=1&currentTime=1732597053";
        return sendPostRequest(null, url);
    }

    public HttpResponse<String> sendPostRequest(String jsonPayload, String url) throws Exception {
        // 创建 HttpClient
        HttpClient client = HttpClient.newHttpClient();
        // 创建 HttpRequest
        HttpRequest request;
        if (jsonPayload == null) {
            // 创建 HttpRequest
            request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("accept", "application/json, text/plain, */*")
                    .header("accept-language", "zh-cn")
                    .header("cache-control", "no-cache")
                    .header("enterpriseid", "101741")
                    .header("origin", "https://workplace.yeastar.cn")
                    .header("pragma", "no-cache")
                    .header("priority", "u=1, i")
                    .header("referer", "https://workplace.yeastar.cn/admin/")
                    .header("sec-ch-ua", "\"Google Chrome\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"")
                    .header("sec-ch-ua-mobile", "?0")
                    .header("sec-ch-ua-platform", "\"Windows\"")
                    .header("sec-fetch-dest", "empty")
                    .header("sec-fetch-mode", "cors")
                    .header("sec-fetch-site", "same-origin")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                    .header("cookie", COOKIE)
                    .header("content-type", "application/json;charset=UTF-8")
                    .GET()
                    .build();
        } else {
            // 创建 HttpRequest
            request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("accept", "application/json, text/plain, */*")
                    .header("accept-language", "zh-cn")
                    .header("cache-control", "no-cache")
                    .header("enterpriseid", "101741")
                    .header("origin", "https://workplace.yeastar.cn")
                    .header("pragma", "no-cache")
                    .header("priority", "u=1, i")
                    .header("referer", "https://workplace.yeastar.cn/admin/")
                    .header("sec-ch-ua", "\"Google Chrome\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"")
                    .header("sec-ch-ua-mobile", "?0")
                    .header("sec-ch-ua-platform", "\"Windows\"")
                    .header("sec-fetch-dest", "empty")
                    .header("sec-fetch-mode", "cors")
                    .header("sec-fetch-site", "same-origin")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                    .header("cookie", COOKIE)
                    .header("content-type", "application/json;charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();
        }
        // 发送请求并获取响应
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public HttpResponse<String> addMeeting(WpMeeting wpMeeting) throws Exception {
        long firstStartTimeLong = Long.parseLong(wpMeeting.getFirstStartTime());
        long lastEndTimeLong = Long.parseLong(wpMeeting.getLastEndTime());
        String key = extractNumber(wpMeeting.getMeetingRoomId());
        HttpResponse<String> data = addMeetingRequest(key, wpMeeting.getSubject(), firstStartTimeLong, lastEndTimeLong, wpMeeting.getStartTime(), wpMeeting.getStartDate());
        return data;
    }

    public static String extractNumber(String input) {
        // 固定前缀和后缀
        String prefix = "wp-integration.device.";
        String suffix = ".schedule";
        // 确保输入字符串包含前缀和后缀
        if (input.startsWith(prefix) && input.endsWith(suffix)) {
            // 提取前缀后的部分
            String withoutPrefix = input.substring(prefix.length());
            // 提取后缀前的部分
            String number = withoutPrefix.substring(0, withoutPrefix.length() - suffix.length());
            return number;
        }
        return null; // 如果输入字符串不符合预期格式，则返回null
    }

    public HttpResponse<String> addMeetingRequest(String id, String subject, long firstStartTime, long lastEndTime, String startTime, String startDate) throws Exception {
        getToken();
        String url = String.format("https://workplace.yeastar.cn/services/conference/api/v1/meeting_rooms/%s/conferences", id);

        // 计算两个时间戳之间的差值（以秒为单位）
        long differenceInSeconds = lastEndTime - firstStartTime;

        // 将差值从秒转换为分钟
        long differenceInMinutes = differenceInSeconds / 60;
        // 创建请求体
        String jsonInputString = String.format(
                "{\n" +
                        "    \"subject\": \"%s\",\n" +
                        "    \"scheduleSetting\": {\n" +
                        "        \"duration\": %d,\n" +
                        "        \"willStartReminderMinutes\": [\n" +
                        "            15\n" +
                        "        ],\n" +
                        "        \"scheduleMode\": \"ONCE\",\n" +
                        "        \"simpleRepeatType\": \"NEVER\",\n" +
                        "        \"firstStartTime\": %d,\n" +
                        "        \"lastEndTime\": %d,\n" +
                        "        \"customRecurrencePattern\": null,\n" +
                        "        \"syncToThirdParty\": true,\n" +
                        "        \"startTime\": \"%s\",\n" +
                        "        \"startDate\": \"%s\",\n" +
                        "        \"timezone\": \"UTC+8 Asia/Shanghai\",\n" +
                        "        \"enableDst\": true\n" +
                        "    },\n" +
                        "    \"host\": {\n" +
                        "        \"id\": 78902,\n" +
                        "        \"memberId\": 73825,\n" +
                        "        \"name\": \"test\",\n" +
                        "        \"onlyName\": \"test\",\n" +
                        "        \"departmentIds\": [\n" +
                        "            1009324\n" +
                        "        ]\n" +
                        "    },\n" +
                        "    \"participants\": [],\n" +
                        "    \"visitType\": 0,\n" +
                        "    \"allowSelfRegistration\": false,\n" +
                        "    \"conferenceReserveSource\": \"WEB_GRID\"\n" +
                        "}", subject, differenceInMinutes, firstStartTime, lastEndTime, startTime, startDate);

        return sendPostRequest(jsonInputString, url);
    }

    public HttpResponse<String> deleteMeeting(WpMeeting wpMeeting) throws Exception {
        getToken();
        String url = String.format("https://workplace.yeastar.cn/services/conference/api/v1/conferences/%s", wpMeeting.getMeetingId());
        String jsonInputString = String.format("{\n" +
                "    \"selectStartTime\": %s,\n" +
                "    \"selectMode\": \"SELECTED\",\n" +
                "    \"cancelVisitorRecord\": false\n" +
                "}", wpMeeting);
        return sendPostRequest(jsonInputString, url);
    }

    public HttpResponse<String> updateMeeting(WpMeeting wpMeeting) throws Exception {
        long firstStartTimeLong = Long.parseLong(wpMeeting.getFirstStartTime());
        long lastEndTimeLong = Long.parseLong(wpMeeting.getLastEndTime());
        String key = extractNumber(wpMeeting.getMeetingRoomId());
        HttpResponse<String> data = updateMeetingRequest(key, wpMeeting.getSubject(), firstStartTimeLong, lastEndTimeLong, wpMeeting.getStartTime(), wpMeeting.getStartDate());
        return data;
    }

    public HttpResponse<String> updateMeetingRequest(String id, String subject, long firstStartTime, long lastEndTime, String startTime, String startDate) throws Exception {
        getToken();
        String url = String.format("https://workplace.yeastar.cn/services/conference/api/v1/conferences/%s", id);

        // 计算两个时间戳之间的差值（以秒为单位）
        long differenceInSeconds = lastEndTime - firstStartTime;

        // 将差值从秒转换为分钟
        long differenceInMinutes = differenceInSeconds / 60;
        // 创建请求体
        String jsonInputString = String.format(
                "{\n" +
                        "    \"subject\": \"%s\",\n" +
                        "    \"scheduleSetting\": {\n" +
                        "        \"duration\": %d,\n" +
                        "        \"willStartReminderMinutes\": [\n" +
                        "            15\n" +
                        "        ],\n" +
                        "        \"scheduleMode\": \"ONCE\",\n" +
                        "        \"simpleRepeatType\": \"NEVER\",\n" +
                        "        \"firstStartTime\": %d,\n" +
                        "        \"lastEndTime\": %d,\n" +
                        "        \"customRecurrencePattern\": null,\n" +
                        "        \"syncToThirdParty\": true,\n" +
                        "        \"startTime\": \"%s\",\n" +
                        "        \"startDate\": \"%s\",\n" +
                        "        \"timezone\": \"UTC+8 Asia/Shanghai\",\n" +
                        "        \"enableDst\": true\n" +
                        "    },\n" +
                        "    \"host\": {\n" +
                        "        \"id\": 78902,\n" +
                        "        \"memberId\": 73825,\n" +
                        "        \"name\": \"test\",\n" +
                        "        \"onlyName\": \"test\",\n" +
                        "        \"departmentIds\": [\n" +
                        "            1009324\n" +
                        "        ]\n" +
                        "    },\n" +
                        "    \"participants\": [],\n" +
                        "    \"visitType\": 0,\n" +
                        "    \"allowSelfRegistration\": false,\n" +
                        "    \"conferenceReserveSource\": \"WEB_GRID\"\n" +
                        "}", subject, differenceInMinutes, firstStartTime, lastEndTime, startTime, startDate);
        return sendPostRequest(jsonInputString, url);
    }
}
