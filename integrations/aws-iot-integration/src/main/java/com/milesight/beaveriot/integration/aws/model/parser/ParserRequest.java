package com.milesight.beaveriot.integration.aws.model.parser;

import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import lombok.Data;

@Data
public class ParserRequest {

    private String id;

    private String model;

    private Boolean debugged;

    private String type;

    private String input;

    private String output;

    private ExchangePayload paramEntities;
}
