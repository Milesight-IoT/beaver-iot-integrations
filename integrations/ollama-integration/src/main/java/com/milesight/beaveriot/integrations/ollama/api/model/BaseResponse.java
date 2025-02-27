package com.milesight.beaveriot.integrations.ollama.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * @Author yuanh
 * @Description
 * @Package com.milesight.beaveriot.integrations.ollama.api.model
 * @Date 2025/2/7 13:52
 */
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class BaseResponse {
    private Object error;
}
