package com.milesight.beaveriot.integrations.aiinference.support;

import lombok.Data;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

/**
 * author: Luxb
 * create: 2025/6/24 11:03
 **/
public class ImageSupport {
    public static ImageResult getImageBase64FromUrl(String imageUrl) throws Exception {
        ImageResult result = new ImageResult();
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10)) // 设置连接超时时间为10秒
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(imageUrl))
                .timeout(Duration.ofSeconds(60))
                .build();

        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() == 200) {
            String contentType = response.headers().firstValue("Content-Type").orElse("image/jpeg");
            String fileExtension = getFileExtensionFromMimeType(contentType);

            String imageBase64 = Base64.getEncoder().encodeToString(response.body());
            result.setImageBase64(imageBase64);
            result.setImageSuffix(fileExtension);
            System.out.println("data:image/jpeg;base64," + imageBase64);
            return result;
        } else {
            System.err.println("Failed to download image: HTTP " + response.statusCode());
        }
        return null;
    }

    private static String getFileExtensionFromMimeType(String mimeType) {
        return switch (mimeType.toLowerCase()) {
            case "image/png" -> "png";
            case "image/gif" -> "gif";
            case "image/bmp" -> "bmp";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
    }

    @Data
    public static class ImageResult {
        private String imageBase64;
        private String imageSuffix;
    }
}
