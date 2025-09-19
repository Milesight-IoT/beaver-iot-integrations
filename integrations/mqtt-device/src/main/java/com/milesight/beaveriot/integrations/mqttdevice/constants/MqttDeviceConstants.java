package com.milesight.beaveriot.integrations.mqttdevice.constants;

/**
 * author: Luxb
 * create: 2025/9/18 18:12
 **/
public class MqttDeviceConstants {
    // Unit: minutes
    public static final long DEFAULT_DEVICE_OFFLINE_TIMEOUT = 1500;
    public static final long MIN_DEVICE_OFFLINE_TIMEOUT = 1;
    public static final long MAX_DEVICE_OFFLINE_TIMEOUT = 2880;
}
