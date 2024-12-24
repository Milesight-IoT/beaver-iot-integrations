package com.milesight.beaveriot.vms.api;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.milesight.beaveriot.common.util.MilesightHttpUtil;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.support.SpringContext;
import com.milesight.beaveriot.vms.constants.VmsConstants;
import com.milesight.beaveriot.vms.entity.VmsIntegrationEntities;
import com.milesight.beaveriot.vms.model.*;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author yuanh
 * @Description
 * @Package com.milesight.beaveriot.vms.utils
 * @Date 2024/11/20 16:33
 */
@UtilityClass
public class VmsApi {
    private static String VMS_API_URL = "http://127.0.0.1:8092";
    private static String USER_NAME = "username";
    private static String PASSWORD = "pwd123";
    private static final String LOGIN_URL = "/api/user/login";
    private static final String DEVICE_LIST_URL = "/api/device/loginlist?session=%s";
    private static final String DEVICE_STATUS_LIST_URL = "/api/device/allstatus?session=%s";
    private static final String RTSP_LIVE_URL = "/api/rtsp/live?session=%s&deviceId=%s&streamType=%s";
    private static final String HLS_LIVE_URL = "/api/hls/live?session=%s&deviceId=%s&streamType=%s";
    private static final String HLS_VOD_URL = "/api/hls/vod?session=%s&deviceId=%s&starttime=%s&stoptime=%s";
    private static final String SNAP_LIVE_URL = "/api/snap/live?session=%s&deviceId=%s";
    private static final String SNAP_VOD_URL = "/api/snap/live?session=%s&deviceId=%s&timestamp=%s";

    public static void init() {
        EntityValueServiceProvider entityValueServiceProvider = SpringContext.getBean(EntityValueServiceProvider.class);
        String vmsInfoId = VmsConstants.INTEGRATION_ID + ".integration." + VmsConstants.Entity.VMS_INFO + ".";
        String urlId = vmsInfoId + VmsConstants.Entity.VMS_URL;
        String usernameId = vmsInfoId + VmsConstants.Entity.USERNAME;
        String passwordId = vmsInfoId + VmsConstants.Entity.PASSWORD;
        Map<String, JsonNode> jsonNodeMap = entityValueServiceProvider.findValuesByKeys(List.of(urlId, usernameId, passwordId));
        jsonNodeMap.forEach((key, value) -> {
            if (value == null || value.isNull()) {
                return;
            }
            if (key.equals(urlId)) {
                VMS_API_URL = value.textValue();
            }
            if (key.equals(usernameId)) {
                USER_NAME = value.textValue();
            }
            if (key.equals(passwordId)) {
                PASSWORD = value.textValue();
            }
        });
    }

    public static synchronized void setVmsInfo(VmsIntegrationEntities.VmsInfo vmsInfo) {
        VMS_API_URL = vmsInfo.getVmsUrl();
        USER_NAME = vmsInfo.getUsername();
        PASSWORD = vmsInfo.getPassword();
    }

    private static String getPasswordMd5(String password) {
        if (password == null) {
            return "";
        }
        return DigestUtil.md5Hex(password).toUpperCase();
    }

    /**
     * 登录vms
     *
     * @return
     */
    public static VmsLoginResponse login() {
        VmsLoginRequest request = VmsLoginRequest.builder()
                .username(USER_NAME)
                .password(getPasswordMd5(PASSWORD))
                .build();
        String body = MilesightHttpUtil.postBody(VMS_API_URL + LOGIN_URL, 5000, request, null);
        return JSONUtil.toBean(body, VmsLoginResponse.class);
    }

    /**
     * 获取设备列表
     *
     * @param session
     * @return
     */
    public static VmsDeviceListResponse getDeviceList(String session) {
        String url = String.format(VMS_API_URL + DEVICE_LIST_URL, session);
        String body = MilesightHttpUtil.get(url, 5000, null, null);
        return JSONUtil.toBean(body, VmsDeviceListResponse.class);
    }

    /**
     * 获取设备状态
     *
     * @param session
     * @return
     */
    public static VmsStatusListResponse getDeviceStatusList(String session) {
        String url = String.format(VMS_API_URL + DEVICE_STATUS_LIST_URL, session);
        String body = MilesightHttpUtil.get(url, 5000, null, null);
        return JSONUtil.toBean(body, VmsStatusListResponse.class);
    }

    public static VmsRtspLiveResponse getRtspLive(String session, String deviceId, Integer streamType) {
        if (CharSequenceUtil.isBlank(session) || CharSequenceUtil.isBlank(deviceId) || streamType == null) {
            return null;
        }
        String url = String.format(VMS_API_URL + RTSP_LIVE_URL, session, deviceId, streamType);
        String body = MilesightHttpUtil.get(url, 5000, null, null);
        return JSONUtil.toBean(body, VmsRtspLiveResponse.class);
    }

    public static VmsHlsLiveResponse getHlsLive(String session, String deviceId) {
        if (CharSequenceUtil.isBlank(session) || CharSequenceUtil.isBlank(deviceId)) {
            return null;
        }
        String url = String.format(VMS_API_URL + HLS_LIVE_URL, session, deviceId, 1);
        String body = MilesightHttpUtil.get(url, 5000, null, null);
        return JSONUtil.toBean(body, VmsHlsLiveResponse.class);
    }

    public static String getSession() {
        //查询所有设备
        VmsLoginResponse login = VmsApi.login();
        //登录失败
        Assert.notNull(login, "login failure!");
        String session = login.getSession();
        Assert.notBlank(session, "session is null!");
        return session;
    }

    public static List<VmsDeviceListResponse.DeviceInfo> getDeviceInfoList(String session) {
        VmsDeviceListResponse deviceList = VmsApi.getDeviceList(session);
        if (deviceList == null || deviceList.getDeviceInfos().isEmpty()) {
            return Collections.emptyList();
        }
        return deviceList.getDeviceInfos();
    }


    public static Map<String, VmsStatusListResponse.StatusInfo> getStatusInfoMap(String session) {
        VmsStatusListResponse deviceStatusList = VmsApi.getDeviceStatusList(session);
        if (deviceStatusList == null || deviceStatusList.getStatus().isEmpty()) {
            return Collections.emptyMap();
        }
        List<VmsStatusListResponse.StatusInfo> statusInfos = deviceStatusList.getStatus();
        return statusInfos.stream()
                .collect(Collectors.toMap(
                        VmsStatusListResponse.StatusInfo::getDevId,
                        statusInfo -> statusInfo,
                        (existing, replacement) -> existing
                ));
    }


    public static String getRtspUrl(String session, String devId) {
        return Optional.ofNullable(VmsApi.getRtspLive(session, devId, 0))
                .map(VmsRtspLiveResponse::getMainStreamUrl)
                .orElse(null);
    }

    public static String getHlsUrl(String session, String devId) {
        return Optional.ofNullable(VmsApi.getHlsLive(session, devId))
                .map(VmsHlsLiveResponse::getHlsUrl)
                .orElse(null);
    }

    public static String getHlsVodList(String devId, Long startTime) {
        String session = getSession();
        startTime = startTime == null ? System.currentTimeMillis() / 1000 - 600 : startTime;
        Long stopTime = getStopTime(startTime);
        VmsHlsLiveResponse hlsVod = VmsApi.getHlsVod(session, devId, startTime, stopTime);
        return Optional.ofNullable(hlsVod)
                .map(VmsHlsLiveResponse::getHlsUrl)
                .orElse(null);

    }

    public static VmsHlsLiveResponse getHlsVod(String session, String devId, Long startTime, Long stopTime) {
        String url = String.format(VMS_API_URL + HLS_VOD_URL, session, devId, startTime, stopTime);
        String body = MilesightHttpUtil.get(url, 5000, null, null);
        return JSONUtil.toBean(body, VmsHlsLiveResponse.class);
    }

    public static String getSnapLive(String devId) {
        String session = getSession();
        String url = String.format(VMS_API_URL + SNAP_LIVE_URL, session, devId);
        InputStream body = MilesightHttpUtil.getInputStream(url, 5000, null, null);
        return convertInputStreamToBase64(body);
    }

    public static String getSnapVod(String devId, Long timestamp) {
        String session = getSession();
        String url = String.format(VMS_API_URL + SNAP_VOD_URL, session, devId, timestamp);
        InputStream body = MilesightHttpUtil.getInputStream(url, 5000, null, null);
        return convertInputStreamToBase64(body);
    }

    @SneakyThrows
    private static String convertInputStreamToBase64(InputStream inputStream) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;

        // 将 InputStream 读入 ByteArrayOutputStream
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, bytesRead);
        }

        // 获取字节数组
        byte[] imageBytes = byteArrayOutputStream.toByteArray();

        // 关闭流
        inputStream.close();
        byteArrayOutputStream.close();

        // 将字节数组编码为 Base64 字符串
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    private static long getStopTime(long startTime) {
        // 计算加上10分钟后的时间戳
        long endTime = startTime + 600;
        long currentTime = System.currentTimeMillis() / 1000;
        return Math.min(endTime, currentTime);
    }
}
