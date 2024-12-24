package com.milesight.beaveriot.integration.wp.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WpResponsePayload {

    private String status;
    private String requestId;
    private JsonNode data;
}
