package com.milesight.beaveriot.integration.msc.constant;

public interface MscIntegrationConstants {

    String INTEGRATION_IDENTIFIER = "msc-integration";

    interface EventType {

        String LATEST_VALUE = "LATEST_VALUE";

    }

    interface DeviceAdditionalDataName {

        String DEVICE_ID = "device_id";

    }

    interface InternalPropertyIdentifier {

        interface Pattern {
            String PREFIX = "_#";
            String SUFFIX = "#_";
            String TEMPLATE = "_#%s#_";
            String SYSTEM_ENTITY_PREFIX = "@";

            static boolean match(String key) {
                return (key.startsWith(PREFIX) && key.endsWith(SUFFIX)) || key.startsWith(SYSTEM_ENTITY_PREFIX);
            }
        }

        String LAST_SYNC_TIME = "_#last_sync_time#_";

        static String getLastSyncTimeKey(String deviceKey) {
            return String.format("%s.%s", deviceKey, LAST_SYNC_TIME);
        }

    }

}
