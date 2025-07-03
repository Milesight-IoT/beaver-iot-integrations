package com.milesight.beaveriot.integrations.camthinkaiinference.support;

import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.context.integration.model.EntityBuilder;
import com.milesight.beaveriot.integrations.camthinkaiinference.constant.Constants;

import java.text.MessageFormat;

/**
 * author: Luxb
 * create: 2025/6/20 15:54
 **/
public class EntitySupport {
    public static String getDeviceEntityKey(String deviceKey, String identifier) {
        return MessageFormat.format(Constants.ENTITY_KEY_FORMAT, deviceKey, identifier);
    }

    public static String getDeviceEntityChildrenKey(String deviceKey, String parentIdentifier, String identifier) {
        return MessageFormat.format(Constants.CHILDREN_ENTITY_KEY_FORMAT, deviceKey, parentIdentifier, identifier);
    }

    public static Entity buildDeviceStringEntity(String integrationId, String deviceKey, String identifier, String name) {
        return buildDeviceEntity(integrationId, deviceKey, identifier, name, EntityValueType.STRING);
    }

    public static Entity buildDeviceLongEntity(String integrationId, String deviceKey, String identifier, String name) {
        return buildDeviceEntity(integrationId, deviceKey, identifier, name, EntityValueType.LONG);
    }

    public static Entity buildDeviceEntity(String integrationId, String deviceKey, String identifier, String name, EntityValueType valueType) {
        return new EntityBuilder(integrationId, deviceKey)
                .identifier(identifier)
                .property(name, AccessMod.R)
                .valueType(valueType)
                .build();
    }
}
