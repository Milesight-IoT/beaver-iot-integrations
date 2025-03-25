package com.milesight.beaveriot.integrations.milesightgateway.codec.model;

import lombok.Data;

/**
 * VersionResponse class.
 *
 * @author simon
 * @date 2025/2/28
 */
@Data
public class VersionResponse {
    String date;

    String version;

    /**
     * Vendor resource path
     */
    String vendors;

    /**
     * link to download all resources.
     */
    String link;
}
