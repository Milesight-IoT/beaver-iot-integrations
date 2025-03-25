package com.milesight.beaveriot.integrations.milesightgateway.model.api;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * DeviceListResponse class.
 *
 * @author simon
 * @date 2025/2/24
 */
@Data
public class DeviceListResponse {
    private Integer devTotalCount;

    private List<Map<String, Object>> deviceResult;

    private Integer appTotalCount;

    private List<DeviceListAppItem> appResult;

    private Integer pfTotalCount;

    private List<DeviceListProfileItem> profileResult;

    private String channelPlan;

    private Boolean disable;
}
