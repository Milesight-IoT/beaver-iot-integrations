package com.milesight.beaveriot.integration.aws.model.parser;

import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import lombok.Data;

@Data
public class PluginRequest {

    private String id;

    private String name;

    private String text;

    private ExchangePayload paramEntities;
}
