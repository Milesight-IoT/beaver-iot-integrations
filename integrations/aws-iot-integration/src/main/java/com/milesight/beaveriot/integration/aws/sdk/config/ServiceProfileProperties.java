package com.milesight.beaveriot.integration.aws.sdk.config;

import com.milesight.beaveriot.context.integration.bootstrap.IntegrationBootstrapManager;
import lombok.Data;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.stereotype.Component;

/**
 * @author lzy
 */
@Component
//@ConfigurationProperties(prefix = "sdk.aws.config.service-profile")
@Data
public class ServiceProfileProperties {

    private String activeServiceProfileId;
    private ServiceProfile[] profiles;


    @Data
    public static class ServiceProfile {
        private String addGwMetadata;
        private String prAllowed;
        private String raAllowed;
        private String serviceProfileId;
    }

}
