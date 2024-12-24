package com.milesight.beaveriot.integration.aws.constant;

import com.milesight.beaveriot.base.utils.StringUtils;

public interface AwsIntegrationConstants {

    String INTEGRATION_IDENTIFIER = "aws-iot-integration";

    public static String getKey(String propertyKey) {
        return AwsIntegrationConstants.INTEGRATION_IDENTIFIER + ".integration." + StringUtils.toSnakeCase(propertyKey);
    }

    public static String getSnKey() {
        return AwsIntegrationConstants.getKey("add_device." + AwsIntegrationConstants.DeviceAdditionalDataName.DEVICE_SN);
    }

    interface DeviceAdditionalDataName {

        String DEVICE_SN = "sn";

    }

    interface InternalPropertyIdentifier {

        interface Pattern {
            String PREFIX = "_#";
            String SUFFIX = "#_";
            String TEMPLATE = "_#%s#_";

            static boolean match(String key) {
                return key.startsWith(PREFIX) && key.endsWith(SUFFIX);
            }
        }

        String LAST_SYNC_TIME = "_#last_sync_time#_";

        static String getLastSyncTimeKey(String deviceKey) {
            return String.format("%s.%s", deviceKey, LAST_SYNC_TIME);
        }

    }

}
