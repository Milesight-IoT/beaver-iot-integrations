package com.milesight.beaveriot.parser.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 上行链路数据包类型
 */
@Getter
@RequiredArgsConstructor
public enum UpLinkPacketType {

    // 设备确认回复
    DEVICE_ACK_REPLY,

    // 异常值
    EXCEPTION_VALUE,

    // 正常上报
    NORMAL_REPORT;

    @Override
    public String toString() {
        return name();
    }

}