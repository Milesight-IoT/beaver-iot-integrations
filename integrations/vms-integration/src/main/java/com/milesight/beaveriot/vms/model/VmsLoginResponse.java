package com.milesight.beaveriot.vms.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class VmsLoginResponse extends VmsBaseResponse {
    private String session;
    private String userId;
    private String permission;
    private String userRolesId;
}
