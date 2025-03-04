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
public class VmsStatusListResponse extends VmsBaseResponse {
    private List<StatusInfo> status;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StatusInfo {
        private String devId;
        private Integer errCode;
        private Integer mswpStatus;
        private Boolean online;
    }
}
