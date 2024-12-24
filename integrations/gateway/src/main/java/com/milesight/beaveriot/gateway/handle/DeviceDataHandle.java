package com.milesight.beaveriot.gateway.handle;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.Method;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.milesight.beaveriot.base.constants.StringConstant;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.gateway.GatewayConstants;
import com.milesight.beaveriot.gateway.GatewayIntegrationEntities;
import com.milesight.beaveriot.gateway.model.RobotData;
import com.milesight.beaveriot.gateway.util.AESUtil;
import com.milesight.beaveriot.gateway.util.APIHttpClient;
import com.milesight.beaveriot.gateway.util.HttpPostUtil;
import com.milesight.beaveriot.gateway.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class DeviceDataHandle {

    @Autowired
    private EntityValueServiceProvider entityValueServiceProvider;

    // jwt过期时间
    private static Long jwtExp = 0L;
    // 授权
    private static String authorization = "";

    private static final String secretKey = "1111111111111111";
    private static final String iv = "2222222222222222";
    private static final String organizationID = "1";
    private static final String serviceProfileID = "f6f7d81d-647f-4c7f-8409-3e5218c0c523";
    private static final String defaultWebhookUrl = "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=17967493-c9b8-4f4a-a86f-f5aca66252ca";

    private static final String loginUrl = "http://%s/api/internal/login";
    private static final String deviceUrl = "http://%s/api/urdevices";
    private static final String applicationUrl = "http://%s/api/urapplications";

    private static final String payloadcodecsUrl = "http://%s/api/payloadcodecs";
    private static final String integrationsMqttUrl = "http://%s/api/urapplications/%s/integrations/mqtt";

    /**
     * 获取JWT
     *
     * @param gatewayIP 网关IP
     * @param username  用户名
     * @param password  密码
     * @return
     * @throws Exception
     */
    private String getJwt(String gatewayIP, String username, String password) throws Exception {
        if (StrUtil.isBlank(authorization) || jwtExp.compareTo(DateUtil.currentSeconds()) <= 0) {
            String url = String.format(loginUrl, gatewayIP);
            String encrypt = AESUtil.encrypt(password, secretKey, iv);
            Map<String, String> headers = Map.of("Content-Type", "application/json");
            Map<String, Object> params = Map.of("username", username, "password", encrypt);
            String response = HttpPostUtil.doPostJson(url, headers, JSONUtil.toJsonStr(params));
            log.info("登录请求响应: " + response);
            JSONObject responseJson = JSONUtil.parseObj(response);
            authorization = responseJson.getStr("jwt");
            if (StrUtil.isBlank(authorization)) {
                Map<String, String> config = getConfig();
                String webhookUrl = config.get(GatewayIntegrationEntities.Gateway.Fields.webhookUrl);
                pushRobotData(webhookUrl, "获取JWT失败", "gatewayIP:" + gatewayIP + ", username:" + username, responseJson.getStr("error"));
                throw new Exception("获取JWT失败");
            }
            Map<String, Object> decodeJwt = JwtUtil.decodeJwt(authorization);
            jwtExp = Long.parseLong(decodeJwt.get("exp").toString());
        }
        return authorization;
    }

    /**
     * 创建设备
     *
     * @param params
     * @throws Exception
     */
    public void createDevice(Map<String, Object> params) throws Exception {
        if (MapUtil.isEmpty(params)) {
            return;
        }
        Map<String, String> config = getConfig();
        String gatewayIP = config.get(GatewayIntegrationEntities.Gateway.Fields.gatewayIP);
        String username = config.get(GatewayIntegrationEntities.Gateway.Fields.username);
        String password = config.get(GatewayIntegrationEntities.Gateway.Fields.password);
        if (MapUtil.isEmpty(config) || StrUtil.isBlank(gatewayIP) || StrUtil.isBlank(username) || StrUtil.isBlank(password)) {
            return;
        }
        if (null == params.get("applicationID")) {
            Object applicationNameObj = params.get(GatewayIntegrationEntities.AddDevice.Fields.applicationName);
            String applicationID = createDefaultApplication(null == applicationNameObj ? GatewayIntegrationEntities.DefaultApplicationName.JYX_TEST.name() : applicationNameObj.toString());
            params.put("applicationID", applicationID);
        }
        String webhookUrl = config.get(GatewayIntegrationEntities.Gateway.Fields.webhookUrl);
        String jwt = getJwt(gatewayIP, username, password);
        String url = String.format(deviceUrl, gatewayIP);
        Map<String, String> headers = Map.of("authorization", "Bearer " + jwt);
        String json = JSONUtil.toJsonStr(params);
        String response = HttpPostUtil.doPostJson(url, headers, json);
        log.info("创建设备请求响应: " + response);
        JSONObject responseJson = JSONUtil.parseObj(response);
        if (!responseJson.isEmpty() && responseJson.getInt("code") != 200) {
            pushRobotData(webhookUrl,"创建任务失败", "devEUI:" + params.get(GatewayIntegrationEntities.AddDevice.Fields.devEUI), responseJson.getStr("error"));
            throw new Exception("创建任务失败， error: " + responseJson.getStr("error"));
        }
        pushRobotData(webhookUrl,"创建设备成功", "devEUI:" + params.get(GatewayIntegrationEntities.AddDevice.Fields.devEUI), null);
    }

    /**
     * 删除设备
     *
     * @param devEUI
     * @throws Exception
     */
    public void deleteDevice(String devEUI) throws Exception {
        if (StrUtil.isBlank(devEUI)) {
            return;
        }
        Map<String, String> config = getConfig();
        String gatewayIP = config.get(GatewayIntegrationEntities.Gateway.Fields.gatewayIP);
        String username = config.get(GatewayIntegrationEntities.Gateway.Fields.username);
        String password = config.get(GatewayIntegrationEntities.Gateway.Fields.password);
        if (MapUtil.isEmpty(config) || StrUtil.isBlank(gatewayIP) || StrUtil.isBlank(username) || StrUtil.isBlank(password)) {
            return;
        }
        String jwt = getJwt(gatewayIP, username, password);
        String url = String.format(deviceUrl, gatewayIP) + StringConstant.SLASH + devEUI;
        Map<String, String> headers = Map.of("authorization", "Bearer " + jwt);
        String response = HttpPostUtil.doDelete(url, headers, MapUtil.newHashMap());
        log.info("删除设备请求响应: " + response);
        JSONObject responseJson = JSONUtil.parseObj(response);
        String webhookUrl = config.get(GatewayIntegrationEntities.Gateway.Fields.webhookUrl);
        // todo 目前实际请求时，删除不会返回数据code等信息，所以这里直接返回
        if (!responseJson.isEmpty() && responseJson.getInt("code") != 200) {
            String error = responseJson.getStr("error");
            // 删除失败，并且提示设备不存在，则忽略 - 防止该设备没有被绑定在该平台上
            if (StrUtil.isNotBlank(error) && error.contains("not exist")) {
                return;
            }
            pushRobotData(webhookUrl, "删除设备失败", "devEUI:" + devEUI, responseJson.getStr("error"));
            throw new Exception("删除任务失败， error: " + responseJson.getStr("error"));
        }
        pushRobotData(webhookUrl,"删除设备成功", "devEUI:" + devEUI, null);
    }

    /**
     * 查询设备列表
     *
     * @return
     * @throws Exception
     */
    public JSONArray queryDevice() throws Exception {
        Map<String, String> config = getConfig();
        String gatewayIP = config.get(GatewayIntegrationEntities.Gateway.Fields.gatewayIP);
        String username = config.get(GatewayIntegrationEntities.Gateway.Fields.username);
        String password = config.get(GatewayIntegrationEntities.Gateway.Fields.password);
        if (MapUtil.isEmpty(config) || StrUtil.isBlank(gatewayIP) || StrUtil.isBlank(username) || StrUtil.isBlank(password)) {
            return null;
        }
        String jwt = getJwt(gatewayIP, username, password);
        String url = String.format(deviceUrl, gatewayIP);
        Map<String, String> headers = Map.of("authorization", "Bearer " + jwt);
        Map<String, Object> params = Map.of("search", "", "order", "asc", "offset", 0, "limit", 9999, "organizationID", organizationID);
        String response = HttpPostUtil.doGet(url, headers, params);
        System.out.println("获取设备列表响应: " + response);
        JSONObject responseJson = JSONUtil.parseObj(response);
        if (!responseJson.isEmpty() && null != responseJson.get("code") && responseJson.getInt("code") != 200) {
            throw new Exception("获取设备列表失败， error: " + responseJson.getStr("error"));
        }
        return responseJson.getJSONArray("deviceResult");
    }

    /**
     * 获取配置
     *
     * @return
     */
    public Map<String, String> getConfig() {
        List<String> keys = CollUtil.toList(GatewayConstants.GATEWAY_GATEWAY_IP, GatewayConstants.GATEWAY_USERNAME, GatewayConstants.GATEWAY_PASSWORD, GatewayConstants.GATEWAY_WEBHOOK_URL);
        Map<String, JsonNode> values = entityValueServiceProvider.findValuesByKeys(keys);
        if (MapUtil.isEmpty(values)) {
            return MapUtil.newHashMap();
        }
        return Map.of(
                GatewayIntegrationEntities.Gateway.Fields.gatewayIP, values.get(GatewayConstants.GATEWAY_GATEWAY_IP).asText(),
                GatewayIntegrationEntities.Gateway.Fields.username, values.get(GatewayConstants.GATEWAY_USERNAME).asText(),
                GatewayIntegrationEntities.Gateway.Fields.password, values.get(GatewayConstants.GATEWAY_PASSWORD).asText(),
                GatewayIntegrationEntities.Gateway.Fields.webhookUrl, values.get(GatewayConstants.GATEWAY_WEBHOOK_URL).asText()
        );
    }

    /**
     * 创建默认应用
     *
     * @param defaultApplicationName
     * @return 默认应用id
     * @throws Exception
     */
    private String createDefaultApplication(String defaultApplicationName) throws Exception {
        JSONArray array = getApplications();
        if (null == array) {
            return null;
        }
        for (Object object : array) {
            if (!(object instanceof JSONObject)) {
                continue;
            }
            JSONObject jsonObject = (JSONObject) object;
            if (null != jsonObject.get("name") && defaultApplicationName.equalsIgnoreCase(jsonObject.getStr("name"))) {
                String applicationID = jsonObject.getStr("id");
                // 集成mqtt
                integrationsMqtt(Method.PUT, applicationID, MqttDataHandle.host);
                return applicationID;
            }
        }
        try {
            // 创建应用
            return createApplication(defaultApplicationName, defaultApplicationName);
        } catch (Exception e) {
            log.error("创建应用失败， error: " + e.getMessage());
        }
        return null;
    }

    /**
     * 创建应用
     *
     * @param name
     * @param description
     * @throws Exception
     */
    public String createApplication(String name, String description) throws Exception {
        Map<String, String> config = getConfig();
        String gatewayIP = config.get(GatewayIntegrationEntities.Gateway.Fields.gatewayIP);
        String username = config.get(GatewayIntegrationEntities.Gateway.Fields.username);
        String password = config.get(GatewayIntegrationEntities.Gateway.Fields.password);
        if (MapUtil.isEmpty(config) || StrUtil.isBlank(gatewayIP) || StrUtil.isBlank(username) || StrUtil.isBlank(password)) {
            return null;
        }
        String jwt = getJwt(gatewayIP, username, password);
        String url = String.format(applicationUrl, gatewayIP);
        Map<String, String> headers = Map.of("authorization", "Bearer " + jwt);
        Map<String, Object> params = Map.of(
                "name", name,
                "description", description,
                "organizationID", organizationID,
                "serviceProfileID", serviceProfileID
        );
        String json = JSONUtil.toJsonStr(params);
        String response = HttpPostUtil.doPostJson(url, headers, json);
        log.info("创建应用请求响应: " + response);
        JSONObject responseJson = JSONUtil.parseObj(response);
        String webhookUrl = config.get(GatewayIntegrationEntities.Gateway.Fields.webhookUrl);
        if (null == responseJson || responseJson.isEmpty() || null == responseJson.get("id")) {
            pushRobotData(webhookUrl, "创建应用失败", "应用名:" + name, responseJson.getStr("error"));
            throw new Exception("创建应用失败， error: " + responseJson.getStr("error"));
        }
        String applicationID = responseJson.getStr("id");
        pushRobotData(webhookUrl, "创建应用成功", "应用名:" + name, null);
        // 集成mqtt
        integrationsMqtt(Method.POST, applicationID, MqttDataHandle.host);
        return applicationID;
    }

    /**
     * 集成mqtt
     *
     * @param applicationID
     * @param host
     * @throws Exception
     */
    public void integrationsMqtt(Method method, String applicationID, String host) throws Exception {
        Map<String, String> config = getConfig();
        String gatewayIP = config.get(GatewayIntegrationEntities.Gateway.Fields.gatewayIP);
        String username = config.get(GatewayIntegrationEntities.Gateway.Fields.username);
        String password = config.get(GatewayIntegrationEntities.Gateway.Fields.password);
        if (MapUtil.isEmpty(config) || StrUtil.isBlank(gatewayIP) || StrUtil.isBlank(username) || StrUtil.isBlank(password)) {
            return;
        }
        String jwt = getJwt(gatewayIP, username, password);
        String url = String.format(integrationsMqttUrl, gatewayIP, applicationID);
        Map<String, String> headers = Map.of("authorization", "Bearer " + jwt);
        Map<String, Object> params = new HashMap<>();
        params.put("host", host);
        params.put("uplinkTopic", MqttDataHandle.uplinke);
        params.put("downlinkTopic", MqttDataHandle.downlink);
        params.put("clientID", "gateway_client");
        params.put("TLSMode", 0);
        params.put("connectTimeout", 30);
        params.put("id", "11");
        params.put("keepAliveInterval", 60);
        params.put("password", "");
        params.put("port", 1883);
        params.put("useAuth", false);
        params.put("useTLS", false);
        params.put("username", "");
        params.put("ackQoS", MqttDataHandle.qos);
        params.put("ackTopic", "");
        params.put("errorQoS", MqttDataHandle.qos);
        params.put("errorTopic", "");
        params.put("joinQoS", MqttDataHandle.qos);
        params.put("joinTopic", "");
        params.put("upQoS", MqttDataHandle.qos);
        params.put("downlinkQoS", MqttDataHandle.qos);
        params.put("mcDownlinkTopic", "");
        params.put("mcDownlinkQoS", MqttDataHandle.qos);
        params.put("requestTopic", "");
        params.put("requestQoS", MqttDataHandle.qos);
        params.put("responseTopic", "");
        params.put("responseQoS", MqttDataHandle.qos);
        String response = Method.POST.equals(method) ? HttpPostUtil.doPostJson(url, headers, JSONUtil.toJsonStr(params)) : HttpPostUtil.doPutJson(url, headers, JSONUtil.toJsonStr(params));
        log.info("集成mqtt请求响应: " + response);
        JSONObject responseJson = JSONUtil.parseObj(response);
        String webhookUrl = config.get(GatewayIntegrationEntities.Gateway.Fields.webhookUrl);
        if (!responseJson.isEmpty() && responseJson.getInt("code") != 200) {
            pushRobotData(webhookUrl, "应用集成mqtt失败", "应用id:" + applicationID, responseJson.getStr("error"));
            throw new Exception("集成mqtt失败， error: " + responseJson.getStr("error"));
        }
    }

    /**
     * 获取全部编解码器内容
     */
    public JSONArray getAllPayloadCodecContent() throws Exception {
        Map<String, String> config = getConfig();
        String gatewayIP = config.get(GatewayIntegrationEntities.Gateway.Fields.gatewayIP);
        String username = config.get(GatewayIntegrationEntities.Gateway.Fields.username);
        String password = config.get(GatewayIntegrationEntities.Gateway.Fields.password);
        if (MapUtil.isEmpty(config) || StrUtil.isBlank(gatewayIP) || StrUtil.isBlank(username) || StrUtil.isBlank(password)) {
            return null;
        }
        String jwt = getJwt(gatewayIP, username, password);
        String url = String.format(payloadcodecsUrl, gatewayIP);
        Map<String, String> headers = Map.of("authorization", "Bearer " + jwt);
        Map<String, Object> params = Map.of("type", "default", "order", "asc", "offset", 0, "limit", 9999);
        String response = HttpPostUtil.doGet(url, headers, params);
        System.out.println("获取编解列表响应: " + response);
        JSONObject responseJson = JSONUtil.parseObj(response);
        if (!responseJson.isEmpty() && null != responseJson.get("code") && responseJson.getInt("code") != 200) {
            throw new Exception("获取编解列表失败， error: " + responseJson.getStr("error"));
        }
        return responseJson.getJSONArray("result");
    }

    /**
     * 获取应用列表
     */
    public JSONArray getApplications() throws Exception {
        Map<String, String> config = getConfig();
        String gatewayIP = config.get(GatewayIntegrationEntities.Gateway.Fields.gatewayIP);
        String username = config.get(GatewayIntegrationEntities.Gateway.Fields.username);
        String password = config.get(GatewayIntegrationEntities.Gateway.Fields.password);
        if (MapUtil.isEmpty(config) || StrUtil.isBlank(gatewayIP) || StrUtil.isBlank(username) || StrUtil.isBlank(password)) {
            return null;
        }
        String jwt = getJwt(gatewayIP, username, password);
        String url = String.format(applicationUrl, gatewayIP);
        Map<String, String> headers = Map.of("authorization", "Bearer " + jwt);
        Map<String, Object> params = Map.of("search", "", "order", "asc", "offset", 0, "limit", 9999, "organizationID", organizationID);
        String response = HttpPostUtil.doGet(url, headers, params);
        System.out.println("获取应用列表响应: " + response);
        JSONObject responseJson = JSONUtil.parseObj(response);
        if (!responseJson.isEmpty() && null != responseJson.get("code") && responseJson.getInt("code") != 200) {
            throw new Exception("获取应用列表失败， error: " + responseJson.getStr("error"));
        }
        return responseJson.getJSONArray("result");
    }

    /**
     * 推送企微消息
     */
    public void pushRobotData(String webhookUrl, String operation, String content, String errMeg) {
        if (StrUtil.isBlank(operation) || StrUtil.isBlank(content)) {
            return;
        }
        String pushContent = "网关数据接入：\n" +
                "操作 | " + operation +" \n" +
                "内容 | " + content;
        if (StrUtil.isNotBlank(errMeg)) {
            pushContent += " \n错误信息 | " + errMeg;
        }
        try {
            APIHttpClient ac = new APIHttpClient(StrUtil.blankToDefault(webhookUrl, DeviceDataHandle.defaultWebhookUrl));
            Integer statusCode = ac.postH(RobotData.toJsonStr(new RobotData(pushContent, StrUtil.isNotBlank(errMeg) ? 1 : 0)));
            if (statusCode != 200) {
                log.error(">>>>>>>>>>> 网关, 推送企微消息 send fail, pushContent:{}", pushContent);
            }
        } catch (Exception e) {
            log.error(">>>>>>>>>>> 网关, 推送企微消息 send fail, pushContent:{}", pushContent, e);
        }
    }

}
