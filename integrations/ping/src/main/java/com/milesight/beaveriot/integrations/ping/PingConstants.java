package com.milesight.beaveriot.integrations.ping;

public class PingConstants {
    private PingConstants() {}

    public static final String INTEGRATION_ID = "ping";

    public enum DeviceStatus {
        ONLINE, OFFLINE;
    }
}
