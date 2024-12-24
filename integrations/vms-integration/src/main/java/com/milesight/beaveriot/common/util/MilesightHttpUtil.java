package com.milesight.beaveriot.common.util;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.Map;

/**
 * @Author yuanh
 * @Description
 * @Package com.milesight.beaveriot.vms.util
 * @Date 2024/11/20 18:58
 */
@Slf4j
@UtilityClass
public class MilesightHttpUtil {
    /**
     * post请求
     *
     * @param url        请求地址
     * @param timeout    超时时间
     * @param requestObj 请求参数
     * @param headers    请求头
     * @return 返回结果
     */
    public static String postBody(String url, int timeout, Object requestObj, Map<String, String> headers) {
        try {
            log.info("MilesightHttpUtil.postBody url:{}", url);
            HttpRequest request = HttpRequest.post(url)
                    .header(Header.CONTENT_TYPE, ContentType.JSON.toString())
                    .setReadTimeout(timeout * 1000)
                    .setConnectionTimeout(3000);
            initHeaders(headers, request);
            initRequestObj(requestObj, request);
            HttpResponse response = request.execute();
            return response.body();
        } catch (Exception e) {
            log.error("MilesightHttpUtil.postBody failed, url:{}", url, e);
            throw e;
        }
    }

    /**
     * put请求
     *
     * @param url        请求地址
     * @param timeout    超时时间
     * @param requestObj 请求参数
     * @param headers    请求头
     * @return 返回结果
     */
    public static String putBody(String url, int timeout, Object requestObj, Map<String, String> headers) {
        try {
            log.info("MilesightHttpUtil.putBody url:{}", url);
            HttpRequest request = HttpRequest.put(url)
                    .header(Header.CONTENT_TYPE, ContentType.JSON.toString())
                    .setReadTimeout(timeout * 1000)
                    .setConnectionTimeout(3000);
            initHeaders(headers, request);
            initRequestObj(requestObj, request);
            HttpResponse response = request.execute();
            return response.body();
        } catch (Exception e) {
            log.error("MipRemoteUtil.put failed, url:{}", url, e);
            throw e;
        }
    }


    /**
     * get请求
     *
     * @param url        请求地址
     * @param timeout    超时时间
     * @param requestObj 请求参数
     * @param headers    请求头
     * @return
     */
    public static String get(String url, int timeout, Object requestObj, Map<String, String> headers) {
        try {
            log.info("MipRemoteUtil.get url:{}", url);
            HttpRequest request = HttpRequest.get(url)
                    .header(Header.CONTENT_TYPE, ContentType.JSON.toString())
                    .setReadTimeout(timeout * 1000)
                    .setConnectionTimeout(3000);
            initHeaders(headers, request);
            initRequestObj(requestObj, request);
            HttpResponse response = request.execute();
            return response.body();
        } catch (Exception e) {
            log.error("MipRemoteUtil.get failed, url:{}", url, e);
            throw e;
        }
    }


    public static InputStream getInputStream(String url, int timeout, Object requestObj, Map<String, String> headers) {
        try {
            log.info("MipRemoteUtil.get url:{}", url);
            HttpRequest request = HttpRequest.get(url)
                    .header(Header.CONTENT_TYPE, ContentType.JSON.toString())
                    .setReadTimeout(timeout * 1000)
                    .setConnectionTimeout(3000);
            initHeaders(headers, request);
            initRequestObj(requestObj, request);
            HttpResponse response = request.execute();
            return response.bodyStream();
        } catch (Exception e) {
            log.error("MipRemoteUtil.get failed, url:{}", url, e);
            throw e;
        }
    }

    private static void initHeaders(Map<String, String> headers, HttpRequest request) {
        if (MapUtil.isNotEmpty(headers)) {
            headers.forEach(request::header);
        }
    }

    private static void initRequestObj(Object requestObj, HttpRequest request) {
        if (requestObj == null) {
            return;
        }
        String header = request.header(Header.CONTENT_TYPE.toString());
        if (ContentType.JSON.toString().equals(header)) {
            request.body(getBody(requestObj));
        } else {
            request.form(getForm(requestObj));
        }
    }


    private static String getBody(Object requestObj) {
        if (requestObj instanceof String) {
            return (String) requestObj;
        }
        return JSONUtil.toJsonStr(requestObj);
    }

    private static Map<String, Object> getForm(Object requestObj) {
        if (requestObj instanceof Map) {
            return (Map<String, Object>) requestObj;
        }
        return BeanUtil.beanToMap(requestObj);
    }
}
