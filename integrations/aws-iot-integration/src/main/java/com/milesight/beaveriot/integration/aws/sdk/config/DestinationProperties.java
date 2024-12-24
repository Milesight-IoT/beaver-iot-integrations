package com.milesight.beaveriot.integration.aws.sdk.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author linzy
 */
@Component
//@ConfigurationProperties(prefix = "sdk.aws.config.destination")
@Data
public class DestinationProperties {
    private String name = "up_raw_lora_destination";
    private String ruleName = "up_raw_lora_rule2";

}
