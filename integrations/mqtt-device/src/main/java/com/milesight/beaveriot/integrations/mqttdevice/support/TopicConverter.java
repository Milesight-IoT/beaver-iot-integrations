package com.milesight.beaveriot.integrations.mqttdevice.support;

/**
 * author: Luxb
 * create: 2025/5/15 17:15
 **/
public class TopicConverter {
    public static String convert(String topic) {
        if (topic.startsWith("/")) {
            topic = topic.substring(1);
        }
        topic = DataCenter.INTEGRATION_ID + "/" + topic;
        return topic.replace(DataCenter.DEVICE_ID_PLACEHOLDER, "+");
    }
}
