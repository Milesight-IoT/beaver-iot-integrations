package com.milesight.beaveriot.integrations.milesightgateway.model;

import com.milesight.beaveriot.base.enums.EnumCode;

public enum DeviceConnectStatus implements EnumCode {
    ONLINE, OFFLINE;

    @Override
    public String getCode() {
        return name();
    }

    @Override
    public String getValue() {
        return name();
    }
}
