package com.milesight.beaveriot.integrations.milesightgateway.codec;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.integrations.milesightgateway.codec.model.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * ResourceRequester class.
 *
 * @author simon
 * @date 2025/2/27
 */
@Slf4j
public class ResourceRequester {
    private final ObjectMapper json = ResourceString.jsonInstance();

    @Getter
    private String repoUrl = ResourceConstant.DEFAULT_DEVICE_CODEC_URI;

    public ResourceRequester(String repoUrl) {
        if (StringUtils.hasText(repoUrl)) {
            this.repoUrl = repoUrl;
        }
    }

    private String joinUrl(String base, String path) {
        if (!base.endsWith("/")) {
            base = base + "/";
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        try {
            URI baseUri = new URI(base);
            URI resolvedUri = baseUri.resolve(path);
            return resolvedUri.toString();
        } catch (URISyntaxException e) {
            log.error("Invalid Url:" + base + " + " + path);
            throw ServiceException
                    .with(CodecErrorCode.CODEC_RESOURCE_INVALID_URL)
                    .args(Map.of("base", base, "path", path))
                    .build();
        }
    }

    private InputStream getUrlInputStream(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(ResourceConstant.JSON_REQUEST_CONNECTION_TIMEOUT);
        connection.setReadTimeout(ResourceConstant.JSON_REQUEST_READ_TIMEOUT);
        return connection.getInputStream();
    }

    private <T> T requestJsonResource(String resourcePath, Class<T> clazz) {
        String requestUrl = this.joinUrl(this.repoUrl, resourcePath);
        try {
            return json.readValue(getUrlInputStream(requestUrl), clazz);
        } catch (JsonParseException e) {
            log.error("Json format error from " + requestUrl + " : " + e.getMessage());
            throw ServiceException
                    .with(CodecErrorCode.CODEC_RESOURCE_FORMAT_ERROR)
                    .args(Map.of("requestUrl", requestUrl))
                    .build();
        } catch (IOException e) {
            log.error("Request json error from " + requestUrl + " : " + e.getMessage());
            throw ServiceException
                    .with(CodecErrorCode.CODEC_RESOURCE_REQUEST_ERROR)
                    .args(Map.of("requestUrl", requestUrl))
                    .build();
        }
    }

    public String requestResourceAsString(String resourcePath) {
        String requestUrlStr = this.joinUrl(this.repoUrl, resourcePath);
        try (
                InputStream inputStream = getUrlInputStream(requestUrlStr);
                ByteArrayOutputStream result = new ByteArrayOutputStream();
        ) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }

            return result.toString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Request resource error from " + resourcePath + " : " + e.getMessage());
            throw ServiceException
                    .with(CodecErrorCode.CODEC_RESOURCE_REQUEST_ERROR)
                    .args(Map.of("requestUrl", requestUrlStr))
                    .build();
        }
    }

    public VersionResponse requestCodecVersion() {
        VersionResponse response = requestJsonResource(ResourceConstant.DEFAULT_DEVICE_CODEC_VERSION, VersionResponse.class);
        if (response.getVersion() == null || response.getVendors() == null) {
            log.error("Broken remote version resource: " + response);
            throw ServiceException
                    .with(CodecErrorCode.CODEC_RESOURCE_FORMAT_ERROR)
                    .args(Map.of("requestUrl", "version"))
                    .build();
        }
        return response;
    }

    public List<Vendor> requestCodecVendors(String vendorListUri) {
        return requestJsonResource(vendorListUri, VendorResponse.class).getVendors();
    }

    public DeviceResourceResponse requestVendorDevices(String deviceListUri) {
        return requestJsonResource(deviceListUri, DeviceResourceResponse.class);
    }

    public DeviceDef requestDeviceDef(String deviceCodecUri) {
        return requestJsonResource(deviceCodecUri, DeviceDef.class);
    }
}
