package com.milesight.beaveriot.integrations.milesightgateway.codec;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.context.integration.model.AttributeBuilder;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.context.integration.model.EntityBuilder;
import com.milesight.beaveriot.integrations.milesightgateway.codec.model.*;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Device class.
 *
 * @author simon
 * @date 2025/3/6
 */
public class DeviceHelper {
    private DeviceHelper() {}

    @Data
    public static class UpdateResourceResult {
        private Device device;

        private Entity decoderEntity;

        private Entity encoderEntity;
    }

    @Data
    private static class ExpandedObject {
        private DeviceDefObject def;

        private String identifier;

        private String parentEntityIdentifier;

        private String parentObjectEntityIdentifier;
    }

    public static UpdateResourceResult updateResourceInfo(Device device, DeviceDef deviceDef) {
        UpdateResourceResult updateResourceResult = new UpdateResourceResult();
        Map<String, Entity> entityMap = new LinkedHashMap<>();

        // Build Device Entity
        buildDeviceEntityMap(entityMap, device, deviceDef);

        // Build Additional Codec Script Entity
        if (entityMap.containsKey(ResourceConstant.DECODER_ENTITY_IDENTIFIER)) {
            throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), "Reserved decoder identifier").build();
        }
        updateResourceResult.setDecoderEntity(new EntityBuilder()
                .identifier(ResourceConstant.DECODER_ENTITY_IDENTIFIER)
                .property(ResourceConstant.DECODER_ENTITY_NAME, AccessMod.R)
                .valueType(EntityValueType.STRING)
                .visible(false)
                .build());
        entityMap.put(ResourceConstant.DECODER_ENTITY_IDENTIFIER, updateResourceResult.getDecoderEntity());

        if (entityMap.containsKey(ResourceConstant.ENCODER_ENTITY_IDENTIFIER)) {
            throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), "Reserved encoder identifier").build();
        }
        updateResourceResult.setEncoderEntity(new EntityBuilder()
                .identifier(ResourceConstant.ENCODER_ENTITY_IDENTIFIER)
                .property(ResourceConstant.ENCODER_ENTITY_NAME, AccessMod.R)
                .valueType(EntityValueType.STRING)
                .visible(false)
                .build());
        entityMap.put(ResourceConstant.ENCODER_ENTITY_IDENTIFIER, updateResourceResult.getEncoderEntity());

        device.setEntities(entityMap.values().stream().toList());
        updateResourceResult.setDevice(device);
        return updateResourceResult;
    }

    private static void buildDeviceEntityMap(Map<String, Entity> entityMap, Device device, DeviceDef deviceDef) {
        Map<String, List<DeviceDefObject>> childObjectDefMap = new HashMap<>();
        Deque<ExpandedObject> objectToProcess = new ArrayDeque<>();

        // split parents and children to be processed
        initProcessEOList(deviceDef, objectToProcess, childObjectDefMap);

        while (!objectToProcess.isEmpty()) {
            ExpandedObject eo = objectToProcess.pop();

            EntityBuilder eb = new EntityBuilder(device.getIntegrationId(), device.getKey());
            eb.identifier(eo.getIdentifier());
            String parentEntityIdentifier = eo.getParentEntityIdentifier();
            if (parentEntityIdentifier != null) {
                eb.parentIdentifier(parentEntityIdentifier);
            }

            DeviceDefObject def = eo.getDef();
            buildEntity(def, eb);
            Entity entity = eb.build();

            final boolean isStructObject = def.getDataType().equals(ObjectDataType.OBJECT);
            final boolean isArrayObject = def.getDataType().equals(ObjectDataType.ARRAY);
            if (!isStructObject && !isArrayObject) {
                entityMap.put(entity.getKey(), entity);
                continue;
            }

            String childParentEntityIdentifier = parentEntityIdentifier == null ? entity.getIdentifier() : parentEntityIdentifier;
            if (isStructObject) {
                // struct
                generateStructChildEO(eo, childParentEntityIdentifier, childObjectDefMap).forEach(objectToProcess::push);
            } else if (def.getValueType().equals(ObjectValueType.STRUCT)) {
                // array of struct
                generateArrayStructChildEO(eo, childParentEntityIdentifier, childObjectDefMap).forEach(objectToProcess::push);
            } else {
                // plain array
                generatePlainArrayChildEO(eo, childParentEntityIdentifier).forEach(objectToProcess::push);
            }

            if (parentEntityIdentifier == null) {
                entityMap.put(entity.getKey(), entity);
            }
        }
    }

    private static void initProcessEOList(DeviceDef deviceDef, Deque<ExpandedObject> initEOs, Map<String, List<DeviceDefObject>> childObjectDefMap) {
        deviceDef.getObject().forEach(deviceDefObject -> {
            String defObjectId = deviceDefObject.getId();
            if (ResourceString.containsForbiddenChars(defObjectId)) {
                throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), "Invalid codec object id: " + defObjectId).build();
            }

            int pointSeparator = defObjectId.lastIndexOf(ResourceConstant.CODEC_LEVEL_SEPARATOR);
            if (pointSeparator < 0) {
                ExpandedObject eo = new ExpandedObject();
                eo.setDef(deviceDefObject);
                eo.setIdentifier(defObjectId);
                initEOs.add(eo);
            } else {
                String parentObjectId = defObjectId.substring(0, pointSeparator);
                List<DeviceDefObject> childList = childObjectDefMap.computeIfAbsent(parentObjectId, k -> new ArrayList<>());
                childList.add(deviceDefObject);
            }
        });
    }

    private static List<ExpandedObject> generateStructChildEO(ExpandedObject parentEO, String childParentEntityIdentifier, Map<String, List<DeviceDefObject>> childObjectDefMap) {
        List<ExpandedObject> childEOs = new ArrayList<>();
        childObjectDefMap.get(parentEO.getDef().getId()).forEach(childDef -> {
            ExpandedObject childEO = new ExpandedObject();
            childEO.setDef(childDef);
            childEO.setParentEntityIdentifier(childParentEntityIdentifier);
            childEO.setParentObjectEntityIdentifier(parentEO.getIdentifier());
            int pointSeparator = childDef.getId().lastIndexOf(ResourceConstant.CODEC_LEVEL_SEPARATOR);
            String childId = childDef.getId().substring(pointSeparator + 1);
            if (parentEO.getParentEntityIdentifier() == null) {
                childEO.setIdentifier(childId);
            } else {
                childEO.setIdentifier(parentEO.getIdentifier() + ResourceConstant.ENTITY_LEVEL_SEPARATOR + childId);
            }

            childEOs.add(0, childEO);
        });

        return childEOs;
    }

    private static int getMaxLength(DeviceDefObject def) {
        return def.getMaxLength() > 1 ? 1 : def.getMaxLength();
    }

    private static List<ExpandedObject> generateArrayStructChildEO(ExpandedObject parentEO, String childParentEntityIdentifier, Map<String, List<DeviceDefObject>> childObjectDefMap) {
        DeviceDefObject parentDef = parentEO.getDef();
        String arrayItemObjectId = parentEO.getDef().getId() + ResourceConstant.CODEC_LEVEL_SEPARATOR + ResourceConstant.CODEC_ARRAY_ITEM_IDENTIFIER;
        List<ExpandedObject> childEOs = new ArrayList<>();
        for (int i = 0; i < getMaxLength(parentDef); i++) {
            final int arrayIndex = i;
            childObjectDefMap.get(arrayItemObjectId).forEach(childDef -> {
                ExpandedObject childEO = new ExpandedObject();
                childEO.setDef(childDef);
                childEO.setParentEntityIdentifier(childParentEntityIdentifier);
                childEO.setParentObjectEntityIdentifier(parentEO.getIdentifier());
                int pointSeparator = childDef.getId().lastIndexOf(ResourceConstant.CODEC_LEVEL_SEPARATOR);
                String childId = childDef.getId().substring(pointSeparator + 1);
                if (parentEO.getParentEntityIdentifier() == null) {
                    childEO.setIdentifier(ResourceConstant.getEntityArraySeparator(arrayIndex) + ResourceConstant.ENTITY_LEVEL_SEPARATOR + childId);
                } else {
                    childEO.setIdentifier(parentEO.getIdentifier() + ResourceConstant.getEntityArraySeparator(arrayIndex) + ResourceConstant.ENTITY_LEVEL_SEPARATOR + childId);
                }

                childEOs.add(0, childEO);
            });
        }

        return childEOs;
    }

    private static List<ExpandedObject> generatePlainArrayChildEO(ExpandedObject parentEO, String childParentEntityIdentifier) {
        DeviceDefObject parentDef = parentEO.getDef();
        List<ExpandedObject> childEOs = new ArrayList<>();
        for (int i = 0; i < parentDef.getMaxLength(); i++) {
            ExpandedObject childEO = new ExpandedObject();
            DeviceDefObject childDef = getPlainArrayChildDef(parentDef);

            childEO.setDef(childDef);
            childEO.setParentEntityIdentifier(childParentEntityIdentifier);
            childEO.setParentObjectEntityIdentifier(parentEO.getIdentifier());
            if (parentEO.getParentEntityIdentifier() == null) {
                childEO.setIdentifier(ResourceConstant.getEntityArraySeparator(i));
            } else {
                childEO.setIdentifier(parentEO.getIdentifier() + ResourceConstant.getEntityArraySeparator(i));
            }

            childEOs.add(0, childEO);
        }

        return childEOs;
    }

    private static DeviceDefObject getPlainArrayChildDef(DeviceDefObject parentDef) {
        DeviceDefObject childDef = new DeviceDefObject();
        childDef.setId(parentDef.getId());
        childDef.setName(parentDef.getName());
        childDef.setAccessMode(parentDef.getAccessMode());
        childDef.setValueType(parentDef.getValueType());
        childDef.setUnit(parentDef.getUnit());

        // Auto predicate child data type
        if (parentDef.getValueType().equals(ObjectValueType.STRING)) {
            childDef.setDataType(ObjectDataType.TEXT);
        } else {
            childDef.setDataType(ObjectDataType.NUMBER);
        }
        return childDef;
    }

    private static void buildEntity(DeviceDefObject deviceDefObject, EntityBuilder eb) {
        DeviceDefObject.ACCESS_MODE accessMode = Optional
                .ofNullable(deviceDefObject.getAccessMode())
                .orElse(DeviceDefObject.ACCESS_MODE.NONE);

        switch (accessMode) {
            case R, RW:
                eb.property(deviceDefObject.getName(), accessMode == DeviceDefObject.ACCESS_MODE.RW ? AccessMod.RW : AccessMod.R);
                break;
            case W:
                eb.service(deviceDefObject.getName());
                break;
            case NONE:
                eb.event(deviceDefObject.getName());
                break;
        }

        // Build Value Type
        switch (deviceDefObject.getValueType()) {
            case STRING -> eb.valueType(EntityValueType.STRING);
            case FLOAT -> eb.valueType(EntityValueType.DOUBLE);
            case STRUCT -> eb.valueType(EntityValueType.OBJECT);
            default -> {
                if (deviceDefObject.getDataType().equals(ObjectDataType.ARRAY)
                    || deviceDefObject.getDataType().equals(ObjectDataType.OBJECT)
                ) {
                    eb.valueType(EntityValueType.OBJECT);
                } else if (deviceDefObject.getDataType().equals(ObjectDataType.BOOL)) {
                    eb.valueType(EntityValueType.BOOLEAN);
                } else {
                    eb.valueType(EntityValueType.LONG);
                }
            }
            // No boolean, because so far, the enum values key is number instead of boolean.
        }

        // Build enum
        AttributeBuilder ab = new AttributeBuilder();
        if (deviceDefObject.getValues() != null) {
            if (deviceDefObject.getDataType().equals(ObjectDataType.BOOL)) {
                ab.enums(deviceDefObject.getValues().stream().collect(Collectors.toMap(
                        deviceDefObjectEnum -> deviceDefObjectEnum.getValue() == 0 ? "false" : "true",
                        DeviceDefObjectEnum::getName
                )));
            } else {
                ab.enums(deviceDefObject.getValues().stream().collect(Collectors.toMap(
                        deviceDefObjectEnum -> deviceDefObjectEnum.getValue().toString(),
                        DeviceDefObjectEnum::getName
                )));
            }
        }

        if (StringUtils.hasText(deviceDefObject.getUnit())) {
            ab.unit(deviceDefObject.getUnit());
        }

        eb.attributes(ab.build());
    }
}
