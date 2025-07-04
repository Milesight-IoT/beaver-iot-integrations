package com.milesight.beaveriot.integrations.camthinkaiinference.api.model.response;

import lombok.Data;

/**
 * author: Luxb
 * create: 2025/6/13 9:56
 **/
@Data
public class CamThinkResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private Pagination pagination;
    private String errorCode;
}
