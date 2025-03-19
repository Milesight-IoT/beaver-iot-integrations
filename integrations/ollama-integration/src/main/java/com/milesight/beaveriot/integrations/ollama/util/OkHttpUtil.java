package com.milesight.beaveriot.integrations.ollama.util;

import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class OkHttpUtil {

    private static final OkHttpClient client;

    static {
        // Initialize OkHttpClient and set timeout
        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)  // Connection timeout
                .readTimeout(120, TimeUnit.SECONDS)    // Read timeout
                .writeTimeout(10, TimeUnit.SECONDS)    // Write timeout
                .build();
    }

    /**
     * Sends a GET request
     *
     * @param url     The request URL
     * @param headers The request headers (can be null)
     * @return The response result
     * @throws IOException If the request fails, an exception is thrown
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
     * Sends a POST request (JSON data)
     *
     * @param url     The request URL
     * @param headers The request headers (can be null)
     * @param json    The request body in JSON format
     * @return The response result
     * @throws IOException If the request fails, an exception is thrown
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
     * Sends a POST request (form data)
     *
     * @param url      The request URL
     * @param headers  The request headers (can be null)
     * @param formData The form data
     * @return The response result
     * @throws IOException If the request fails, an exception is thrown
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
     * Sends a PUT request (JSON data)
     *
     * @param url     The request URL
     * @param headers The request headers (can be null)
     * @param json    The request body in JSON format
     * @return The response result
     * @throws IOException If the request fails, an exception is thrown
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
     * Sends a DELETE request
     *
     * @param url     The request URL
     * @param headers The request headers (can be null)
     * @return The response result
     * @throws IOException If the request fails, an exception is thrown
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
     * Adds request headers
     *
     * @param builder The request builder
     * @param headers The request headers
     */
    private static void addHeaders(Request.Builder builder, Map<String, String> headers) {
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }
    }
}
