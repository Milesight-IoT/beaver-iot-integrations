package com.milesight.beaveriot.integrations.milesightgateway.codec.model;

import lombok.Data;

import java.util.List;

/**
 * VendorResponse class.
 *
 * @author simon
 * @date 2025/2/27
 */
@Data
public class VendorResponse {
    List<Vendor> vendors;
}
