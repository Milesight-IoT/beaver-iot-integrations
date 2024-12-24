package com.milesight.beaveriot.gateway.util;

import cn.hutool.json.JSONUtil;
import com.milesight.beaveriot.gateway.model.RobotData;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @description: Http Client
 * @author: hubg
 * @create: 2021-11-23 15:36
 **/
@Slf4j
@Component
public class APIHttpClient {
    // 接口地址
    private String apiURL;
    private HttpClient httpClient = new DefaultHttpClient();
    private HttpPost method = null;

    public APIHttpClient() {
    }

    /**
     * 接口地址
     *
     * @param url
     */
    public APIHttpClient(String url) {
        setApiURL(url);
    }

    public void setApiURL(String apiURL) {
        this.apiURL = apiURL;
        method = null;
        if (apiURL != null) {
            method = new HttpPost(apiURL);
        }
    }


    public HttpClient getHttpClient() {
        return httpClient;
    }

    public HttpPost getMethod() {
        if (method == null && apiURL != null) {
            method = new HttpPost(apiURL);
        }
        return method;
    }

    /**
     * 调用 API
     *
     * @param parameters  参数是个json字符串
     * @return
     */
    public Integer postH(String parameters) {
        log.debug("apiURL:"+ apiURL + ", parameters:" + parameters);
        HttpPost med = getMethod();
        int statusCode = -1;
        if (med != null & parameters != null && !"".equals(parameters.trim())) {
            try {
                // 建立一个NameValuePair数组，用于存储欲传送的参数
                med.addHeader("Content-type", "application/json; charset=utf-8");
                med.setHeader("Accept", "application/json");
                med.setEntity(new StringEntity(parameters, StandardCharsets.UTF_8));
                HttpResponse response = getHttpClient().execute(med);
                statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 200) {
                    log.error("Method failed:" + response.getStatusLine());
                    log.error("返回值:" + response);
                }
            } catch (IOException e) {
                log.error("执行失败", e);
            }

        }
        return statusCode;
    }

    public static void main(String[] args) {
        APIHttpClient ac = new APIHttpClient("https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=17967493-c9b8-4f4a-a86f-f5aca66252ca");
        String jObjStr = JSONUtil.toJsonStr(new RobotData("hello world", 0));

        System.out.println(jObjStr);
        System.out.println(ac.postH(jObjStr));
    }
}
