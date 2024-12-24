package com.milesight.beaveriot.integration.lavatory.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LavatoryRequest {

    /**
     * 楼层
     */
    private Integer floor;
    /**
     * 性别
     */
    private Integer sex;

}
