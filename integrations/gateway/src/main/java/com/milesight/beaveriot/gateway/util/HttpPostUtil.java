package com.milesight.beaveriot.gateway.util;

import cn.hutool.core.map.MapUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.milesight.beaveriot.gateway.model.PayloadCodecContent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class HttpPostUtil {

    /**
     * 发送POST请求（JSON格式）
     *
     * @param url     请求的URL
     * @param headers 请求头
     * @param json    请求的JSON字符串
     * @return 响应结果
     */
    public static String doPostJson(String url, Map<String, String> headers, String json) {
        return createRequestByJson(Method.POST, url, headers, json);
    }

    /**
     * 发送PUT请求（JSON格式）
     *
     * @param url     请求的URL
     * @param headers 请求头
     * @param json    请求的JSON字符串
     * @return 响应结果
     */
    public static String doPutJson(String url, Map<String, String> headers, String json) {
        return createRequestByJson(Method.PUT, url, headers, json);
    }

    /**
     * 发送GET请求
     *
     * @param url     请求的URL
     * @param headers 请求头
     * @param params  请求参数
     * @return 响应结果
     */
    public static String doGet(String url, Map<String, String> headers, Map<String, Object> params) {
        return createRequest(Method.GET, url, headers, params);
    }

    /**
     * 发送DELETE请求
     *
     * @param url     请求的URL
     * @param headers 请求头
     * @param params  请求参数
     * @return 响应结果
     */
    public static String doDelete(String url, Map<String, String> headers, Map<String, Object> params) {
        return createRequest(Method.DELETE, url, headers, params);
    }

    /**
     * 创建HTTP请求
     *
     * @param method  HTTP方法（GET/DELETE等）
     * @param url     请求的URL
     * @param headers 请求头
     * @param params  请求参数
     * @return 响应结果
     */
    public static String createRequest(Method method, String url, Map<String, String> headers, Map<String, Object> params) {
        // 创建请求
        HttpRequest request = HttpUtil.createRequest(method, url);
        // 设置请求头
        if (headers != null) {
            headers.forEach(request::header);
        }
        // 设置请求参数
        if (params != null) {
            request.form(params);
        }
        // 发送请求并获取响应
        HttpResponse response = request.execute();
        // 返回响应结果
        return response.body();
    }

    /**
     * 创建HTTP请求
     *
     * @param method  HTTP方法（POST/PUT等）
     * @param url     请求的URL
     * @param headers 请求头
     * @param json    请求的JSON字符串
     * @return 响应结果
     */
    public static String createRequestByJson(Method method, String url, Map<String, String> headers, String json) {
        // 创建POST请求
        HttpRequest request = HttpUtil.createRequest(method, url);
        // 设置请求头
        if (headers != null) {
            headers.forEach(request::header);
        }
        // 设置请求体为JSON格式
        request.body(json, "application/json");
        // 发送请求并获取响应
        HttpResponse response = request.execute();
        // 返回响应结果
        return response.body();
    }

    public static void main(String[] args) {
        // 登录
//        String url = "http://192.168.47.173/api/internal/login";
//        Map<String, String> headers = Map.of("Content-Type", "application/json");
//        Map<String, Object> params = Map.of("username", "admin", "password", "NicJjG18XOV3U1efQyo8AQ==");
//        String json = JSONUtil.toJsonStr(params);
//        String response = doPostJson(url, headers, json);
//        System.out.println("JSON POST请求响应: " + response);

        String jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOiJsb3JhLWFwcC1zZXJ2ZXIiLCJleHAiOjE3MzI5NTU5MzMsImlzcyI6ImxvcmEtYXBwLXNlcnZlciIsIm5iZiI6MTczMjg2OTUzMywic3ViIjoidXNlciIsInVzZXJuYW1lIjoiYWRtaW4ifQ.7waA7XVm-JBEB332wuj3PEMJ0Gh_bCKYN4nU4h-fjzw";
//        // 获取应用列表
//        String url = "http://192.168.43.50/api/urapplications";
//        Map<String, String> headers = Map.of("authorization", "Bearer " + jwt);
//        Map<String, Object> params = Map.of("search", "", "order", "asc", "offset", 0, "limit", 9999, "organizationID", 1);
//        String response = doGet(url, headers, params);
//        System.out.println("GET请求响应: " + response);

        // 获取设备列表
//        String url = "http://192.168.43.50/api/urdevices";
//        Map<String, String> headers = Map.of("authorization", "Bearer " + jwt);
//        Map<String, Object> params = Map.of("search", "", "order", "asc", "offset", 0, "limit", 9999, "organizationID", 1);
//        String response = doGet(url, headers, params);
//        System.out.println("GET请求响应: " + response);

        // 新增设备
        String url = "http://192.168.43.50/api/urdevices";
        Map<String, String> headers = Map.of("authorization", "Bearer " + jwt);
        Map<String, Object> params = MapUtil.newHashMap();
        params.put("devEUI", "24E124782C466278");
        params.put("name", "jyx");
        params.put("applicationID", "1");
        /*params.put("description", "jyx-test");
        params.put("profileID", "940dcdbe-8d5b-4a1e-b7c6-4e5437162015");
        params.put("fCntUp", 4);
        params.put("fCntDown", 2);
        params.put("skipFCntCheck", true);*/
        //params.put("appKey", "b1eab904f666ce48c065d4298076923e");
        params.put("devAddr", "069a7d8b");
        /*params.put("appSKey", "b1eab904f666ce48c065d4298076924e");
        params.put("nwkSKey", "1f6fd0d57df9f8a9e526d05b7fff844d");
        //params.put("mbMode", "tcp");
        //params.put("mbFramePort", "175");
        //params.put("mbTCPPort", "18080");
        params.put("fPort", 1);
        params.put("payloadCodecID", "0");*/
        String json = JSONUtil.toJsonStr(params);
        String response = doPostJson(url, headers, json);
        System.out.println("JSON POST请求响应: " + response);

        // 删除设备
//        String devEUI = "24E124782C466278";
//        String url = "http://192.168.43.50/api/urdevices/" + devEUI;
//        Map<String, String> headers = Map.of("authorization", "Bearer " + jwt);
//        Map<String, Object> params = MapUtil.newHashMap();
//        String response = doDelete(url, headers, params);
//        System.out.println("DELETE请求响应: " + response);

        // 获取编解码列表
//        String url = "http://192.168.43.50/api/payloadcodecs";
//        Map<String, String> headers = Map.of("authorization", "Bearer " + jwt);
//        Map<String, Object> params = Map.of("type", "default", "order", "asc", "offset", 0, "limit", 9999);
//        String response = doGet(url, headers, params);
//        JSONObject responseJson = JSONUtil.parseObj(response);
//        //System.out.println("GET请求响应: " + response);
//        JSONArray jsonArray = responseJson.getJSONArray("result");
//        Map<String, String> codecNameMap = new HashMap<>();
//        for (Object obj : jsonArray) {
//            try {
//                PayloadCodecContent content = JSONUtil.toBean(JSONUtil.toJsonStr(obj), PayloadCodecContent.class);
//                codecNameMap.put(content.getId(), content.getName());
//            } catch (Exception e) {
//                System.out.println(e.getMessage());
//            }
//        }
//        Map<String, Map<String, String>> codecNameEnumMap = MapUtil.of("enum", codecNameMap);
//        Map<String, Map<String, String>> stringMapMap = Collections.unmodifiableMap(codecNameEnumMap);
//        System.out.println(stringMapMap);
    }
}
