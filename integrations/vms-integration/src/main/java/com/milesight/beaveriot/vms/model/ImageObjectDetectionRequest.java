package com.milesight.beaveriot.vms.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * @Author yuanh
 * @Description
 * @Package com.milesight.beaveriot.vms.model
 * @Date 2024/11/26 17:55
 */
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class ImageObjectDetectionRequest {
    private String image;
}
