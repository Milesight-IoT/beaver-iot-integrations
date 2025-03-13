package com.milesight.beaveriot.vms.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class VmsRtspLiveResponse extends VmsBaseResponse {
    private String mainStreamUrl;
    private String subStreamUrl;
}
