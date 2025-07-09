package com.milesight.beaveriot.integrations.mqttdevice.support;

/**
 * author: Luxb
 * create: 2025/5/15 17:15
 **/
public class TopicSupporter {
    public static boolean matches(String pattern, String topic) {
        String[] topicParts = topic.split("/");
        String[] patternParts = pattern.split("/");

        if (topicParts.length != patternParts.length) {
            return false;
        }

        for (int i = 0; i < topicParts.length; i++) {
            String p = patternParts[i];
            String t = topicParts[i];

            if (p.equals("+")) {
                continue;
            }

            if (!p.equals(t)) {
                return false;
            }
        }

        return true;
    }

    public static String convert(String topic) {
        return topic.replace(DataCenter.DEVICE_ID_PLACEHOLDER, "+");
    }
}
