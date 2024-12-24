package com.milesight.beaveriot.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayloadCodecContent {

    private String id;
    private String name;
    private String description;
    private String devEUIPrefix;

}
