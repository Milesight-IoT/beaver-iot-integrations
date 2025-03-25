package com.milesight.beaveriot.integrations.milesightgateway.model.response;

import com.milesight.beaveriot.integrations.milesightgateway.model.api.DeviceListAppItem;
import com.milesight.beaveriot.integrations.milesightgateway.model.api.DeviceListProfileItem;
import lombok.Data;

import java.util.List;

/**
 * ConnectionValidateResponse class.
 *
 * @author simon
 * @date 2025/2/24
 */
@Data
public class ConnectionValidateResponse {
    private List<DeviceListAppItem> appResult;

    private List<DeviceListProfileItem> profileResult;
}
