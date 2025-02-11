package com.milesight.beaveriot.parser.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Data Link Type
 */
@Getter
@RequiredArgsConstructor
public enum DataLinkType {
    UP_LINK,
    DOWN_LINK;

    @Override
    public String toString() {
        return name();
    }
}
