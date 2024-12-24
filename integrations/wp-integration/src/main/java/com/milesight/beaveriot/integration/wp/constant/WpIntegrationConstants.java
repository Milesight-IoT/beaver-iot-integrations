package com.milesight.beaveriot.integration.wp.constant;

import com.milesight.beaveriot.base.utils.StringUtils;

public interface WpIntegrationConstants {

    String INTEGRATION_IDENTIFIER = "wp-integration";

    public static String getKey(String propertyKey) {
        return WpIntegrationConstants.INTEGRATION_IDENTIFIER + ".integration." + StringUtils.toSnakeCase(propertyKey);
    }

    interface DeviceAdditionalDataName {

        String DEVICE_ID = "id";

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
