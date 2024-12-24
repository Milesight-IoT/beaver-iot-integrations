package com.milesight.beaveriot.integration.aws.model.parser;

import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ParserResponseData {

    private String model;

    private String type;

    private String input;

    private String output;

    private ExchangePayload paramEntities;
}
