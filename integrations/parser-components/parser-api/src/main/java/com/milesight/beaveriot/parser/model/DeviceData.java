package com.milesight.beaveriot.parser.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceData {

    private String type;

    @JsonAlias("tslId")
    private String tslId;

    private String id;

    private JsonNode payload;
}