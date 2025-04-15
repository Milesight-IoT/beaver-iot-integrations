package com.milesight.beaveriot.integrations.milesightgateway.model.response;

import lombok.Data;

import java.util.List;

/**
 * GatewayListResponse class.
 *
 * @author simon
 * @date 2025/3/12
 */
@Data
public class GatewayListResponse {
    private List<GatewayListItem> gateways;
}
