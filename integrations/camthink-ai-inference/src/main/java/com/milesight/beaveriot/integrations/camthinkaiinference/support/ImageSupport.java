package com.milesight.beaveriot.integrations.camthinkaiinference.support;

import lombok.Data;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.Base64;

/**
 * author: Luxb
 * create: 2025/6/24 11:03
 **/
public class ImageSupport {
    public static final String IMAGE_BASE64_HEADER_FORMAT = "data:{0};base64,";

    public static boolean isUrl(String content) {
        return content != null && (content.startsWith("http://") || content.startsWith("https://"));
    }

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
            result.setImageBase64(MessageFormat.format(IMAGE_BASE64_HEADER_FORMAT, contentType) + imageBase64);
            result.setImageSuffix(fileExtension);
            return result;
        }
        return result;
    }

    @SuppressWarnings("unused")
    public static ImageResult getImageBase64FromPath(String filePath) throws Exception {
        ImageResult result = new ImageResult();

        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("File does not exist: " + filePath);
        }
        if (!Files.isReadable(path)) {
            throw new IllegalArgumentException("File cannot be read: " + filePath);
        }

        byte[] imageBytes = Files.readAllBytes(path);

        String mimeType = Files.probeContentType(path);
        if (mimeType == null || !mimeType.startsWith("image/")) {
            mimeType = "image/jpeg";
        }

        String fileExtension = getFileExtensionFromMimeType(mimeType);

        String imageBase64 = Base64.getEncoder().encodeToString(imageBytes);
        result.setImageBase64(MessageFormat.format(IMAGE_BASE64_HEADER_FORMAT, mimeType) + imageBase64);
        result.setImageSuffix(fileExtension);

        return result;
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
