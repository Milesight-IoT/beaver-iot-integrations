package com.milesight.beaveriot.integrations.camthinkaiinference.model.request;

import com.milesight.beaveriot.base.page.GenericPageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * author: Luxb
 * create: 2025/6/20 13:29
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class BoundDeviceSearchRequest extends GenericPageRequest {
    private String name;
}
