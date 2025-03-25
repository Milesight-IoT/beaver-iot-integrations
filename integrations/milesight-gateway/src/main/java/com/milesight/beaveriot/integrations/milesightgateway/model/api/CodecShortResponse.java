package com.milesight.beaveriot.integrations.milesightgateway.model.api;

import lombok.Data;

import java.util.List;

/**
 * DeviceCodecShortResponse class.
 *
 * @author simon
 * @date 2025/3/18
 */
@Data
public class CodecShortResponse {
    private Integer totalCount;

    private List<CodecShortListItem> result;
}
