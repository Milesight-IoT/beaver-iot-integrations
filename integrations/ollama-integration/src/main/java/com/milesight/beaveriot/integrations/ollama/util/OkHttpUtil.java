package com.milesight.beaveriot.integrations.ollama.util;

import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class OkHttpUtil {

    private static final OkHttpClient client;

    static {
        // 初始化 OkHttpClient，设置超时时间
        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS) // 连接超时
                .readTimeout(120, TimeUnit.SECONDS)    // 读取超时
                .writeTimeout(10, TimeUnit.SECONDS)   // 写入超时
                .build();
    }

    /**
     * 发送 GET 请求
     *
     * @param url     请求地址
     * @param headers 请求头（可为空）
     * @return 响应结果
     * @throws IOException 请求失败时抛出异常
     */
    public static String get(String url, Map<String, String> headers) throws IOException {
        Request.Builder builder = new Request.Builder().url(url);
        addHeaders(builder, headers);
        Request request = builder.build();
        try (Response response = client.newCall(request).execute()) {
            return getResponse(response);
        }
    }

    /**
     * 发送 POST 请求（JSON 数据）
     *
     * @param url     请求地址
     * @param headers 请求头（可为空）
     * @param json    JSON 格式的请求体
     * @return 响应结果
     * @throws IOException 请求失败时抛出异常
     */
    public static String postJson(String url, Map<String, String> headers, String json) throws IOException {
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(json, JSON);
        Request.Builder builder = new Request.Builder().url(url).post(body);
        addHeaders(builder, headers);
        Request request = builder.build();

        try (Response response = client.newCall(request).execute()) {
            return getResponse(response);
        }
    }

    /**
     * 发送 POST 请求（表单数据）
     *
     * @param url    请求地址
     * @param headers 请求头（可为空）
     * @param formData 表单数据
     * @return 响应结果
     * @throws IOException 请求失败时抛出异常
     */
    public static String postForm(String url, Map<String, String> headers, Map<String, String> formData) throws IOException {
        FormBody.Builder formBuilder = new FormBody.Builder();
        for (Map.Entry<String, String> entry : formData.entrySet()) {
            formBuilder.add(entry.getKey(), entry.getValue());
        }
        RequestBody body = formBuilder.build();
        Request.Builder builder = new Request.Builder().url(url).post(body);
        addHeaders(builder, headers);
        Request request = builder.build();

        try (Response response = client.newCall(request).execute()) {
            return getResponse(response);
        }
    }

    /**
     * 发送 PUT 请求（JSON 数据）
     *
     * @param url     请求地址
     * @param headers 请求头（可为空）
     * @param json    JSON 格式的请求体
     * @return 响应结果
     * @throws IOException 请求失败时抛出异常
     */
    public static String putJson(String url, Map<String, String> headers, String json) throws IOException {
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(json, JSON);
        Request.Builder builder = new Request.Builder().url(url).put(body);
        addHeaders(builder, headers);
        Request request = builder.build();

        try (Response response = client.newCall(request).execute()) {
            return getResponse(response);
        }
    }

    /**
     * 发送 DELETE 请求
     *
     * @param url     请求地址
     * @param headers 请求头（可为空）
     * @return 响应结果
     * @throws IOException 请求失败时抛出异常
     */
    public static String delete(String url, Map<String, String> headers) throws IOException {
        Request.Builder builder = new Request.Builder().url(url).delete();
        addHeaders(builder, headers);
        Request request = builder.build();

        try (Response response = client.newCall(request).execute()) {
            return getResponse(response);
        }
    }

    @NotNull
    private static String getResponse(Response response) throws IOException {
        ResponseBody body = response.body();
        if (!response.isSuccessful() && body == null) {
            throw new IOException("Unexpected code: " + response.code());
        }
        return body.string();
    }

    /**
     * 添加请求头
     *
     * @param builder 请求构建器
     * @param headers 请求头
     */
    private static void addHeaders(Request.Builder builder, Map<String, String> headers) {
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }
    }
}
