package com.milesight.beaveriot.integration.aws.model.parser;

import lombok.Data;

import java.util.Map;

@Data
public class ProductResponseData {
    private String id;
    private String key;
    private String name;
    private String integration;
    private Map<String, Object> additionalData;
    private Long createdAt;
    private Long updatedAt;

    private String integrationName;
    private Boolean deletable;
}
