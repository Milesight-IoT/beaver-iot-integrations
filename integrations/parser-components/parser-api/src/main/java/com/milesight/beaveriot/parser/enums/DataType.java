package com.milesight.beaveriot.parser.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Data Link Type
 */
@Getter
@RequiredArgsConstructor
public enum DataType {
    IPSO,
    JSON;

    @Override
    public String toString() {
        return name();
    }
}
