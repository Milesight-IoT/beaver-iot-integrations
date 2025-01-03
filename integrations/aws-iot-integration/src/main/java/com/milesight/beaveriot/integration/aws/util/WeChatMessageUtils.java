package com.milesight.beaveriot.integration.aws.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.milesight.beaveriot.base.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
public class WeChatMessageUtils {

    private WeChatMessageUtils() {
        throw new IllegalStateException("Utility class");
    }

    private static String getCurrentFormattedTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return now.format(formatter) + " +08:00";
    }

    public static String createAddDeviceAlarmMessage(String name, String sn) {
        return String.format("""
                {
                    "msgtype": "markdown",
                    "markdown": {
                        "content": "✅【添加设备】• INFO•\\n详请\\n  设备name : %s\\n  设备sn : %s\\n 创建时间 : %s"
                    }
                }
                """, name, sn, getCurrentFormattedTime());
    }

    public static String createDeleteDeviceAlarmMessage(String name, String sn) {
        return String.format("""
                {
                    "msgtype": "markdown",
                    "markdown": {
                        "content": "✅【删除设备】• INFO•\\n详请\\n  设备name : %s\\n  设备sn : %s\\n 删除时间 : %s"
                    }
                }
                """, name, sn, getCurrentFormattedTime());
    }

    public static String createAlarmMessage(String sn, String value) {
        return String.format("""
                {
                    "msgtype": "markdown",
                    "markdown": {
                        "content": "🚨【异常值告警】• WARNING•\\n详请\\n触发中告警 [1]\\n 告警名称 : 设备上报异常值\\n 告警级别 :  WARNING\\n  设备sn : %s\\n 告警状态 : FIRING\\n 告警时间 : %s\\n 告警描述 : %s"
                    }
                }
                """, sn, getCurrentFormattedTime(), value);
    }

    public static String createDeviceMessage(String sn, JsonNode value) {
        return String.format("""
                {
                     "ts": %s,
                     "msgId": %s,
                     "event": "device_message",
                     "ver": "1.0",
                     "data": {
                             "sn": %s,
                             "status": "success",
                             "res": %s
                     }
                 }
                """, System.currentTimeMillis(), generateMsgId(), sn, JsonUtils.toPrettyJSON(value));
    }

    public static String createAddDeviceMessage(String name, String sn) {
        return String.format("""
                {
                     "ts": %s,
                     "msgId": %s,
                     "event": "add_device",
                     "ver": "1.0",
                     "data": {
                             "sn": %s,
                             "status": "success",
                             "res": {
                                "name": %s
                             }
                     }
                 }
                """, System.currentTimeMillis(), generateMsgId(), sn, name);
    }

    /**
     * 生成消息id
     *
     * @return 消息id
     */
    public static String generateMsgId() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString().replace("-", "");
    }

    public static String createDeleteDeviceMessage(String name, String sn) {
        return String.format("""
                 {
                     "ts": %s,
                     "msgId": %s,
                     "event": "delete_device",
                     "ver": "1.0",
                     "data": {
                             "sn": %s,
                             "status": "success",
                             "res": {
                                "name": %s
                             }
                     }
                 }
                """, System.currentTimeMillis(), generateMsgId(), sn,name);
    }

}
