package com.milesight.beaveriot.integration.aws.sdk.config;


import com.milesight.beaveriot.integration.aws.sdk.enums.LoraClass;
import com.milesight.beaveriot.integration.aws.sdk.enums.RfRegion;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author linzy
 */
@Component
//@ConfigurationProperties(prefix = "sdk.aws.config.device-profile")
@Data
public class DeviceProfileProperties {

    private DeviceProfile[] profiles;

    @Data
    public static class DeviceProfile {
        private String rfRegion;
        private LoraClass loraClass;
        private String deviceProfileId;
    }

    public String getDeviceProfileId(RfRegion rfRegion, LoraClass loraClass) {
        for (DeviceProfile deviceProfile : profiles) {
            if (deviceProfile.getRfRegion().equals(rfRegion.getValue()) || deviceProfile.getRfRegion().equals(rfRegion.name())) {
                if (deviceProfile.getLoraClass().equals(loraClass)) {
                    return deviceProfile.getDeviceProfileId();
                }
            }
        }
        return null;
    }

}
