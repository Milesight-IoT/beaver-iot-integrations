package com.milesight.beaveriot.gateway.handle;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ClientMQTT implements CommandLineRunner {

    public MqttClient client;
    private MqttConnectOptions options;

    @Autowired
    private MqttDataHandle mqttDataHandle;
    @Override
    public void run(String... args) {
        try {
            // host为主机名，clientid即连接MQTT的客户端ID，一般以唯一标识符表示，MemoryPersistence设置clientid的保存形式，默认为以内存保存
            client = new MqttClient("tcp://" + MqttDataHandle.host + ":1883", MqttDataHandle.uplinke, new MemoryPersistence());
            // MQTT的连接设置
            options = new MqttConnectOptions();
            // 设置是否清空session,这里如果设置为false表示服务器会保留客户端的连接记录，这里设置为true表示每次连接到服务器都以新的身份连接
            options.setCleanSession(true);
            // 设置超时时间 单位为秒
            options.setConnectionTimeout(10);
            // 设置会话心跳时间 单位为秒 服务器会每隔1.5*20秒的时间向客户端发送个消息判断客户端是否在线，但这个方法并没有重连的机制
            options.setKeepAliveInterval(20);
            //设置自动连接
            options.setAutomaticReconnect(true);
            // 设置回调
            client.setCallback(new MqttCallbackExtended() {
                //连接成功回调，需要重新订阅主题
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    sendNotify(true);
                    subscribe();
                }

                @Override
                public void connectionLost(Throwable cause) {
                    retryConnection(3);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    token.isComplete();
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    System.out.println("topic: " + topic);
                    System.out.println("Qos: " + message.getQos());
                    System.out.println("message content: " + new String(message.getPayload()));
                }
            });
            //setWill方法，如果项目中需要知道客户端是否掉线可以调用该方法。设置最终端口的通知消息
//            MqttTopic mqttTopic = client.getTopic(MqttDataHandle.topic);
//            options.setWill(mqttTopic, "close".getBytes(), MqttDataHandle.qos, true);
            client.connect(options);
            subscribe();
        } catch (Exception e) {
            log.error("mqtt异常" + e.getMessage());
            e.printStackTrace();
        }
    }

    private void subscribe() {
        try {
            //订阅队列
            client.subscribe(MqttDataHandle.uplinke, MqttDataHandle.qos, (topic, message) -> {
                String msg = new String(message.getPayload(), StandardCharsets.UTF_8);
                log.debug("mqtt收到信息:"+msg);
                //处理消息
                mqttDataHandle.dataHandle(msg);
            });
        } catch (MqttException e) {
            log.error("mqtt异常" + e.getMessage());
        }
    }

    private void retryConnection(int retryNumber) {
        //当重试多次，依旧失败，就是机器故障，需要通知人工处理
        if (retryNumber < 0) {
            sendNotify(false);
        }
        try {
            client.reconnect();
            TimeUnit.SECONDS.sleep(30);
            if (!client.isConnected()) {
                retryConnection(--retryNumber);
            }
        } catch (MqttException | InterruptedException e) {
            log.error("mqtt异常:"+e.getMessage());
        }
    }


    //发送重试通知
    private void sendNotify(boolean isSuccess) {
        if (isSuccess){
            log.trace("mqtt连接成功");
        } else {
            log.error("mqtt连接失败");
        }
    }
}
