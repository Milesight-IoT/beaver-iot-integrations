package com.milesight.beaveriot.integrations.milesightgateway.codec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.integrations.milesightgateway.codec.model.*;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * ResourceRequester class.
 *
 * @author simon
 * @date 2025/2/27
 */
@Slf4j
public class ResourceRequester {
    private final ObjectMapper json = ResourceString.jsonInstance();

    private <T> T requestJsonResource(String resourcePath, Class<T> clazz) {
        String requestUrl = ResourceConstant.DEFAULT_DEVICE_CODEC_URI + resourcePath;
        try {
            return json.readValue(new URL(requestUrl), clazz);
        } catch (Exception e) {
            throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), "Request json error from " + requestUrl + " : " + e.getMessage()).build();
        }
    }

    public String requestJsonResource(String resourcePath) {
        String requestUrlStr = ResourceConstant.DEFAULT_DEVICE_CODEC_URI + resourcePath;
        URL requestUrl;
        try {
            requestUrl = new URL(requestUrlStr);
        } catch (MalformedURLException e) {
            throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), "Invalid Url: " + requestUrlStr).build();
        }

        try (
                InputStream inputStream = requestUrl.openStream();
                ByteArrayOutputStream result = new ByteArrayOutputStream();
        ) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }

            return result.toString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), "Request json error from " + requestUrl + " : " + e.getMessage()).build();
        }
    }

    public VersionResponse requestCodecVersion() {
        VersionResponse response = requestJsonResource(ResourceConstant.DEFAULT_DEVICE_CODEC_VERSION, VersionResponse.class);
        if (response.getVersion() == null || response.getVendors() == null) {
            throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), "Broken remote version resource: " + response).build();
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
