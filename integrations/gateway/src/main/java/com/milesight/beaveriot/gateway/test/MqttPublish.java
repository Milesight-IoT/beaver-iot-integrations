package com.milesight.beaveriot.gateway.test;

import cn.hutool.json.JSONUtil;
import com.milesight.beaveriot.gateway.handle.MqttDataHandle;
import com.milesight.beaveriot.gateway.handle.OnMessageCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.HashMap;
import java.util.Map;


public class MqttPublish {
    public static void main(String[] args) {
//        String pubTopic = "gateway_client";
//        String content = "22222222222222";
//        int qos = 0;
//        String broker = "tcp://" + MqttDataHandle.host + ":1883";
//        String clientId = MqttDataHandle.downlink;
//        MemoryPersistence persistence = new MemoryPersistence();
//        try {
//            MqttClient client = new MqttClient(broker, clientId, persistence);
//            // MQTT 连接选项
//            MqttConnectOptions connOpts = new MqttConnectOptions();
//            // 保留会话
//            connOpts.setCleanSession(true);
//            // 设置回调
//            client.setCallback(new OnMessageCallback());
//            // 建立连接
//            client.connect(connOpts);
//            // 消息发布所需参数
//            MqttMessage message = new MqttMessage(content.getBytes());
//            message.setQos(qos);
//            client.publish(pubTopic, message);
//            client.disconnect();
//            System.out.println("Disconnected");
//            client.close();
//            System.exit(0);
//        } catch (MqttException me) {
//            me.printStackTrace();
//        }
        Map<String, Object> content = new HashMap<>();
        content.put("devEUI", "1111");
        content.put("data", Map.of("reboot", true));
        mqttPublist(JSONUtil.toJsonStr(content));
    }

    public static void mqttPublist(String content) {
        MemoryPersistence persistence = new MemoryPersistence();
        try {
            MqttClient client = new MqttClient("tcp://" + MqttDataHandle.host + ":1883", MqttDataHandle.downlink, persistence);
            // MQTT 连接选项
            MqttConnectOptions connOpts = new MqttConnectOptions();
            // 保留会话
            connOpts.setCleanSession(true);
            // 设置回调
            client.setCallback(new OnMessageCallback());
            // 建立连接
            client.connect(connOpts);
            // 消息发布所需参数
            MqttMessage message = new MqttMessage(content.getBytes());
            message.setQos(0);
            client.publish(MqttDataHandle.uplinke, message);
            client.disconnect();
            System.out.println("Disconnected");
            client.close();
            System.exit(0);
        } catch (MqttException me) {
            me.printStackTrace();
        }
    }
}
