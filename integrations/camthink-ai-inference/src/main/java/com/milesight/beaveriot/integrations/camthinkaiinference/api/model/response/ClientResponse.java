package com.milesight.beaveriot.integrations.camthinkaiinference.api.model.response;

import lombok.Builder;
import lombok.Data;

/**
 * author: Luxb
 * create: 2025/6/6 10:23
 **/
@Builder
@Data
public class ClientResponse {
    private int code;
    private String message;
    private String data;
    private boolean isSuccessful;
}
