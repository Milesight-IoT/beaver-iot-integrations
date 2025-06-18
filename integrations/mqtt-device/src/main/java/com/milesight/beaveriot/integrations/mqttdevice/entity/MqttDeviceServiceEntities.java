package com.milesight.beaveriot.integrations.mqttdevice.entity;

import com.milesight.beaveriot.context.integration.context.AddDeviceAware;
import com.milesight.beaveriot.context.integration.entity.annotation.Attribute;
import com.milesight.beaveriot.context.integration.entity.annotation.Entities;
import com.milesight.beaveriot.context.integration.entity.annotation.Entity;
import com.milesight.beaveriot.context.integration.entity.annotation.IntegrationEntities;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.integrations.mqttdevice.support.DataCenter;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * author: Luxb
 * create: 2025/6/18 8:48
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@IntegrationEntities
public class MqttDeviceServiceEntities extends ExchangePayload {
    public static final String DATA_INPUT_IDENTIFIER = "data-input";
    public static final String DATA_INPUT_JSON_DATA_INTEGRATION = "integration";
    public static final String DATA_INPUT_JSON_DATA_IDENTIFIER = "json-data";
    public static final String DATA_INPUT_TEMPLATE_IDENTIFIER = "template";
    public static final String DATA_INPUT_TEMPLATE_KEY = DataCenter.INTEGRATION_ID + ".integration." + DATA_INPUT_IDENTIFIER + "." + DATA_INPUT_TEMPLATE_IDENTIFIER;

    @Entity(type = EntityType.SERVICE, identifier = DATA_INPUT_IDENTIFIER)
    private DataInput dataInput;

    @Data
    @EqualsAndHashCode(callSuper = true)
    @Entities
    public static class DataInput extends ExchangePayload implements AddDeviceAware {
        @Entity(name = "integration", identifier = DATA_INPUT_JSON_DATA_INTEGRATION, attributes = @Attribute(optional = true))
        private String integration;

        @Entity(name = "jsonData", identifier = DATA_INPUT_JSON_DATA_IDENTIFIER)
        private String jsonData;

        @Entity(name = "template", identifier = DATA_INPUT_TEMPLATE_IDENTIFIER, attributes = @Attribute(enumClass = EmptyEnum.class))
        private String template;
    }

    public enum EmptyEnum {
    }
}
