package com.milesight.beaveriot.integrations.milesightgateway.codec.model;

import lombok.Data;

/**
 * Vendor class.
 *
 * @author simon
 * @date 2025/2/27
 */
@Data
public class Vendor {
    String id;

    String name;

    String logo;

    String website;

    String description;

    /**
     * Organization unique identity
     */
    String oui;

    String devices;
}
