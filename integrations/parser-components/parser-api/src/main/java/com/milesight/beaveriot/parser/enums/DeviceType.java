package com.milesight.beaveriot.parser.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 设备类型
 */
@Getter
@RequiredArgsConstructor
public enum DeviceType {
    /**
     * 网关
     */
    GATEWAY(),
    /**
     * 子设备
     */
    SUB_DEVICE(),
    /**
     * 直连设备
     */
    COMMON();

    @Override
    public String toString() {
        return name();
    }
}
