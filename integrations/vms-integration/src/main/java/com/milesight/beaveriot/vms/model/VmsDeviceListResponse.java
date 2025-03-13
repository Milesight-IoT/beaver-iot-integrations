package com.milesight.beaveriot.vms.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class VmsDeviceListResponse extends VmsBaseResponse {
    private List<DeviceInfo> deviceInfos;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DeviceInfo {
        private String addr;
        private String devId;
        private Integer deviceType;
        private Boolean isDisable;
        private String mac;
        private String runServerId;
        private String serverId;
    }
}
