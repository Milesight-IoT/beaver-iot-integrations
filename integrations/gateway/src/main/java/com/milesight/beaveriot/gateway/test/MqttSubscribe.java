package com.milesight.beaveriot.gateway.test;

import com.milesight.beaveriot.gateway.handle.MqttDataHandle;
import com.milesight.beaveriot.gateway.handle.OnMessageCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;


public class MqttSubscribe {
    public static void main(String[] args) {
        String subTopic = MqttDataHandle.uplinke;
        String broker = "tcp://" + MqttDataHandle.host + ":1883";
        String clientId = MqttDataHandle.uplinke;
        MemoryPersistence persistence = new MemoryPersistence();
        try {
            MqttClient client = new MqttClient(broker, clientId, persistence);
            // MQTT 连接选项
            MqttConnectOptions connOpts = new MqttConnectOptions();
            // 保留会话
            connOpts.setCleanSession(true);
            // 设置回调
            client.setCallback(new OnMessageCallback());
            // 建立连接
            client.connect(connOpts);
            // 订阅
            client.subscribe(subTopic);
//            client.disconnect();
//            System.out.println("Disconnected");
//            client.close();
//            System.exit(0);
        } catch (MqttException me) {
            me.printStackTrace();
        }
    }
}
