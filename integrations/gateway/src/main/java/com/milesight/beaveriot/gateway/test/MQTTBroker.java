package com.milesight.beaveriot.gateway.test;

import io.moquette.broker.Server;
import io.moquette.broker.config.IConfig;
import io.moquette.broker.config.MemoryConfig;

import java.io.IOException;
import java.util.Properties;

public class MQTTBroker {
    public static void main(String[] args) {
        Server mqttBroker = new Server();
        Properties props = new Properties();
        props.setProperty("port", "1883");
        IConfig config = new MemoryConfig(props);

        try {
            mqttBroker.startServer(config);
            System.out.println("MQTT Broker started on port 1883. Press [Enter] to stop.");
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            mqttBroker.stopServer();
            System.out.println("MQTT Broker stopped.");
        }
    }
}