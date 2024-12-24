package com.milesight.beaveriot.vms.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class VmsHlsLiveResponse extends VmsBaseResponse {
    private String hlsUrl;
}
