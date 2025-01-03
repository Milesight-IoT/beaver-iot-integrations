package com.milesight.beaveriot.integration.aws.model.parser;

import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import lombok.Data;

@Data
public class ProductRequest {

    private String name;

    private String integration;

    private ExchangePayload paramEntities;
}
