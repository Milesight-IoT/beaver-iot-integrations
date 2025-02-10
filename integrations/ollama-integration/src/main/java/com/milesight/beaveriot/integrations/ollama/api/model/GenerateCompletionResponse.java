package com.milesight.beaveriot.integrations.ollama.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * @Author yuanh
 * @Description
 * @Package com.milesight.beaveriot.integrations.ollama.api.model
 * @Date 2025/2/7 10:29
 */
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class GenerateCompletionResponse extends BaseResponse {
    private String model;
    private String createdAt;
    private String response;
    private Boolean done;
    private String doneReason;
    private Long totalDuration;
    private Long loadDuration;
    private Integer promptEvalCount;
    private Long promptEvalDuration;
    private Integer evalCount;
    private Long evalDuration;
}
