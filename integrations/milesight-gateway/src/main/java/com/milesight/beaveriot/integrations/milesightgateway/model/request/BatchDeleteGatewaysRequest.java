package com.milesight.beaveriot.integrations.milesightgateway.model.request;

import lombok.Data;

import java.util.List;

/**
 * BatchDeleteGatewaysRequest class.
 *
 * @author simon
 * @date 2025/3/19
 */
@Data
public class BatchDeleteGatewaysRequest {
    List<String> gateways;
}
