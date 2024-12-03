package com.milesight.beaveriot.integration.msc.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.context.integration.model.AttributeBuilder;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.context.integration.model.EntityBuilder;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.integration.msc.model.TslEventWrapper;
import com.milesight.beaveriot.integration.msc.model.TslItemWrapper;
import com.milesight.beaveriot.integration.msc.model.TslParamWrapper;
import com.milesight.beaveriot.integration.msc.model.TslPropertyWrapper;
import com.milesight.beaveriot.integration.msc.model.TslServiceWrapper;
import com.milesight.cloud.sdk.client.model.ThingSpec;
import com.milesight.cloud.sdk.client.model.TslDataSpec;
import com.milesight.cloud.sdk.client.model.TslDataValidatorSpec;
import com.milesight.cloud.sdk.client.model.TslKeyValuePair;
import lombok.*;
import lombok.extern.slf4j.*;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class MscTslUtils {

    private MscTslUtils() {
        throw new IllegalStateException("Utility class");
    }

    @Nonnull
    public static List<Entity> thingSpecificationToEntities(@NonNull String integrationId, @NonNull String deviceKey, @NonNull ThingSpec thingSpec) {

        val items = getTslItems(thingSpec);
        val children = items.stream()
                .filter(item -> item.getDataSpec() != null && item.getDataSpec().getParentId() != null)
                .collect(Collectors.groupingBy(TslItemWrapper::getParentId));

        val parents = items.stream()
                .filter(item -> item.getDataSpec() == null || item.getDataSpec().getParentId() == null)
                .map(item -> new TslNode(item, null, null, null))
                .toList();
        val queue = new ArrayDeque<>(parents);
        val identifierToParentEntity = new HashMap<String, Entity>();
        val parentIdentifierToChildEntities = new HashMap<String, List<Entity>>();

        while (!queue.isEmpty()) {
            val node = queue.poll();
            val item = node.item;
            if (item instanceof TslParamWrapper && item.getId() == null) {
                // deprecated spec is not allowed
                continue;
            }

            String itemPath;
            if (node.parentPath != null) {
                if (node.arrayIndex != null) {
                    // Array item path is generated from index
                    itemPath = String.format("%s[%d]", node.parentPath, node.arrayIndex);
                } else {
                    // Child item path is inherited from parent
                    itemPath = String.format("%s.%s", node.parentPath, item.getId().substring(item.getId().lastIndexOf('.') + 1));
                }
            } else {
                itemPath = item.getId();
            }

            String rootIdentifier;
            val isArray = isArray(item.getDataSpec());
            val isRoot = node.rootIdentifier == null;
            if (isArray) {
                // Don't create entity for array node
                if (isRoot) {
                    // Array cannot be a parent
                    rootIdentifier = null;
                } else {
                    rootIdentifier = node.rootIdentifier;
                }
            } else {
                val entityBuilder = new EntityBuilder(integrationId, deviceKey)
                        .identifier(standardizeEntityIdentifier(itemPath))
                        .valueType(item.getValueType())
                        .attributes(convertTslDataSpecToEntityAttributes(item.getDataSpec()));
                switch (item.getEntityType()) {
                    case PROPERTY -> entityBuilder.property(item.getName(), item.getAccessMode());
                    case EVENT -> entityBuilder.event(item.getName());
                    case SERVICE -> entityBuilder.service(item.getName());
                }
                val entity = entityBuilder.build();

                if (isRoot) {
                    rootIdentifier = entity.getIdentifier();
                    identifierToParentEntity.put(rootIdentifier, entity);
                } else {
                    rootIdentifier = node.rootIdentifier;
                    parentIdentifierToChildEntities.computeIfAbsent(rootIdentifier, id -> new ArrayList<>()).add(entity);
                }
            }

            val maxArraySize = Optional.ofNullable(item.getDataSpec())
                    .map(TslDataSpec::getValidator)
                    .map(TslDataValidatorSpec::getMaxSize)
                    .orElse(4L);
            val childNodes = children.getOrDefault(item.getId(), List.of());
            if (childNodes.isEmpty()) {
                continue;
            }

            // Push children to queue
            if (isArray) {
                // Array only contains one type of child item
                val childNode = childNodes.get(0);
                IntStream.range(0, maxArraySize.intValue()).forEach(index ->
                        queue.add(new TslNode(childNode, itemPath, rootIdentifier, index)));
            } else {
                childNodes.forEach(childNode ->
                        queue.add(new TslNode(childNode, itemPath, rootIdentifier, null)));
            }
        }

        val entities = identifierToParentEntity.values().stream()
                .sorted(Comparator.comparing(Entity::getIdentifier))
                .collect(Collectors.toList());
        entities.forEach(entity -> {
            val childrenEntities = parentIdentifierToChildEntities.get(entity.getIdentifier());
            if (childrenEntities != null) {
                childrenEntities.sort(Comparator.comparing(Entity::getIdentifier));
                entity.setChildren(childrenEntities);
            }
            entity.initializeProperties(integrationId, deviceKey);
        });
        return entities;
    }

    record TslNode(TslItemWrapper item, String parentPath, String rootIdentifier, Integer arrayIndex) {

    }

    private static List<TslItemWrapper> getTslItems(ThingSpec thingSpec) {
        val items = new ArrayList<TslItemWrapper>();
        if (thingSpec.getProperties() != null) {
            thingSpec.getProperties()
                    .stream()
                    .map(TslPropertyWrapper::new)
                    .forEach(items::add);
        }
        if (thingSpec.getEvents() != null) {
            thingSpec.getEvents()
                    .stream()
                    .map(TslEventWrapper::new)
                    .peek(items::add)
                    .forEach(item -> items.addAll(getOutputParams(item)));
        }
        if (thingSpec.getServices() != null) {
            thingSpec.getServices()
                    .stream()
                    .map(TslServiceWrapper::new)
                    .peek(items::add)
                    .forEach(item -> items.addAll(getOutputParams(item)));
        }
        return items;
    }

    private static List<TslParamWrapper> getOutputParams(TslItemWrapper item) {
        return item.getOutputs().stream()
                .peek(v -> {
                    if (v.getDataSpec().getParentId() == null) {
                        // Bind output params to its function
                        v.getDataSpec().setParentId(v.getId());
                    }
                }).toList();
    }

    private static String standardizeEntityIdentifier(String rawIdentifier) {
        if (rawIdentifier == null || rawIdentifier.indexOf('.') < 0) {
            return rawIdentifier;
        }
        val fullIdentifier = rawIdentifier.replace('.', '@');
        return fullIdentifier.substring(fullIdentifier.indexOf('@') + 1);
    }

    @Nullable
    private static Map<String, Object> convertTslDataSpecToEntityAttributes(TslDataSpec dataSpec) {
        if (dataSpec == null) {
            return null;
        }
        val attributeBuilder = new AttributeBuilder();
        if (dataSpec.getUnitName() != null) {
            attributeBuilder.unit(dataSpec.getUnitName());
        }
        if (dataSpec.getValidator() != null) {
            if (dataSpec.getValidator().getMax() != null) {
                attributeBuilder.max(dataSpec.getValidator().getMax().doubleValue());
            }
            if (dataSpec.getValidator().getMin() != null) {
                attributeBuilder.min(dataSpec.getValidator().getMin().doubleValue());
            }
            if (dataSpec.getValidator().getMaxSize() != null) {
                attributeBuilder.maxLength(dataSpec.getValidator().getMaxSize().intValue());
            }
            if (dataSpec.getValidator().getMinSize() != null) {
                attributeBuilder.minLength(dataSpec.getValidator().getMinSize().intValue());
            }
        }
        if (dataSpec.getFractionDigits() != null) {
            attributeBuilder.fractionDigits(dataSpec.getFractionDigits().intValue());
        }
        if (dataSpec.getMappings() != null && !dataSpec.getMappings().isEmpty()) {
            attributeBuilder.enums(dataSpec.getMappings()
                    .stream()
                    .collect(Collectors.toMap(v -> {
                        if (TslDataSpec.DataTypeEnum.BOOL.equals(dataSpec.getDataType())) {
                            return switch (v.getKey()) {
                                case "false", "False", "FALSE", "0" -> "false";
                                case "true", "True", "TRUE", "1" -> "true";
                                default -> v.getKey();
                            };
                        }
                        return v.getKey();
                    }, TslKeyValuePair::getValue, (a, b) -> a)));
        }
        return attributeBuilder.build();
    }

    private static boolean isArray(TslDataSpec dataSpec) {
        if (dataSpec == null) {
            return false;
        }
        return TslDataSpec.DataTypeEnum.ARRAY.equals(dataSpec.getDataType());
    }

    public static EntityValueType convertDataTypeToEntityValueType(TslDataSpec.DataTypeEnum dataType) {
        if (dataType == null) {
            return null;
        }
        switch (dataType) {
            case STRING, ENUM, FILE, IMAGE:
                return EntityValueType.STRING;
            case INT, LONG, DATE, LOCAL_TIME:
                return EntityValueType.LONG;
            case FLOAT, DOUBLE:
                return EntityValueType.DOUBLE;
            case BOOL:
                return EntityValueType.BOOLEAN;
            case STRUCT:
                return EntityValueType.OBJECT;
            case ARRAY:
                return null;
            default:
                log.warn("Unsupported data type: {}", dataType);
                return null;
        }
    }

    @Nullable
    public static ExchangePayload convertJsonNodeToExchangePayload(String deviceKey, JsonNode jsonNode) {
        return convertJsonNodeToExchangePayload(deviceKey, jsonNode, true);
    }

    /**
     * Convert json node to exchange payload
     *
     * @param previousEntityKey The entity key from parent entity
     * @param jsonNode          The json data
     * @param isRoot            If json data is root properties, it should be true, otherwise it should be false
     * @return exchange payload
     */
    @Nullable
    public static ExchangePayload convertJsonNodeToExchangePayload(String previousEntityKey, JsonNode jsonNode, boolean isRoot) {
        val result = new HashMap<String, Object>();
        if (jsonNode == null || jsonNode.isEmpty() || !jsonNode.isObject()) {
            return null;
        }
        val entries = new ArrayDeque<JsonEntry>();
        int initialDepth = isRoot ? 0 : 1;
        entries.push(new JsonEntry(previousEntityKey, jsonNode, initialDepth));
        while (!entries.isEmpty()) {
            val parent = entries.pop();
            if (parent.value instanceof ObjectNode objectNode) {
                objectNode.fields().forEachRemaining(entry -> {
                    val fieldName = entry.getKey();
                    val value = entry.getValue();
                    val parentEntityKey = parent.parentEntityKey;
                    val entityKeyTemplate = parent.depth < 2 ? "%s.%s" : "%s@%s";
                    val entityKey = String.format(entityKeyTemplate, parentEntityKey, fieldName);
                    if (value == null || value.isNull()) {
                        log.debug("Null value is ignored: {}", entityKey);
                    } else if (value.isContainerNode()) {
                        entries.push(new JsonEntry(entityKey, value, parent.depth + 1));
                    } else {
                        result.put(entityKey, value);
                    }
                });
            } else if (parent.value instanceof ArrayNode arrayNode) {
                int i = 0;
                for (JsonNode value : arrayNode) {
                    val entityKey = String.format("%s[%d]", parent.parentEntityKey, i);
                    if (value == null || value.isNull()) {
                        log.debug("Null value is ignored: {}", entityKey);
                    } else if (value.isContainerNode()) {
                        // Only object count level
                        entries.push(new JsonEntry(entityKey, value, parent.depth));
                    } else {
                        result.put(entityKey, value);
                    }
                    i++;
                }
            }
        }
        if (result.isEmpty()) {
            return null;
        }
        return ExchangePayload.create(result);
    }

    private record JsonEntry(String parentEntityKey, JsonNode value, int depth) {
    }

    public static Map<String, JsonNode> convertExchangePayloadToGroupedJsonNode(@NotNull ObjectMapper objectMapper, @NotNull String entityKeyPublicPrefix, @NotNull Map<String, Object> keyValues) {
        Objects.requireNonNull(objectMapper);
        Objects.requireNonNull(entityKeyPublicPrefix);
        Objects.requireNonNull(keyValues);

        val result = new HashMap<String, JsonNode>();
        keyValues.forEach((key, value) -> {
            if (!key.startsWith(entityKeyPublicPrefix) || key.equals(entityKeyPublicPrefix)) {
                log.debug("Ignored invalid key: {}, prefix is {}", key, entityKeyPublicPrefix);
                return;
            }
            val path = key.substring(entityKeyPublicPrefix.length() + 1);
            if (path.length() == 0) {
                log.debug("Ignored invalid key: {}", key);
                return;
            }
            if (value == null) {
                log.debug("Null value is ignored: {}", key);
                return;
            }
            ensureParentAndSetValue(objectMapper, result, path, value);
        });
        return result;
    }

    private static void ensureParentAndSetValue(@NotNull ObjectMapper objectMapper, HashMap<String, JsonNode> result, String path, Object value) {
        val paths = path.split("[.@\\[]");
        if (paths.length == 0) {
            return;
        }
        val jsonValue = objectMapper.convertValue(value, JsonNode.class);
        if (paths.length == 1) {
            result.computeIfAbsent(paths[0], k -> jsonValue);
            return;
        }

        var parent = result.computeIfAbsent(paths[0], k -> createContainerNodeByKeyType(objectMapper, paths[1]));
        val lastIndex = paths.length - 1;
        for (int i = 1; i < lastIndex; i++) {
            var key = paths[i];
            var nextKey = paths[i + 1];
            parent = setValueIfNotExists(parent, key, () -> createContainerNodeByKeyType(objectMapper, nextKey));
            if (parent == null) {
                return;
            }
        }

        val key = paths[lastIndex];
        setValueIfNotExists(parent, key, () -> jsonValue);
    }

    private static JsonNode setValueIfNotExists(JsonNode parent, String key, Supplier<JsonNode> valueGetter) {
        JsonNode existingValue;
        if (isArrayIndexKey(key) && parent instanceof ArrayNode arrayParent) {
            val arrayIndex = Integer.parseInt(key.substring(0, key.length() - 1));
            if (arrayParent.size() <= arrayIndex) {
                fillUpArrayWithNull(arrayParent, arrayIndex + 1);
            }
            existingValue = parent.get(arrayIndex);
            if (existingValue == null || existingValue.isNull()) {
                val jsonValue = valueGetter.get();
                arrayParent.set(arrayIndex, jsonValue);
                existingValue = jsonValue;
            }
        } else if (parent instanceof ObjectNode objectParent) {
            existingValue = parent.get(key);
            if (existingValue == null) {
                val jsonValue = valueGetter.get();
                objectParent.set(key, jsonValue);
                existingValue = jsonValue;
            }
        } else {
            // Parent is not a container or key is invalid
            log.warn("Invalid path: {}", key);
            return null;
        }
        return existingValue;
    }

    private static boolean isArrayIndexKey(String key) {
        return key.endsWith("]");
    }

    private static JsonNode createContainerNodeByKeyType(@NotNull ObjectMapper objectMapper, String key) {
        return isArrayIndexKey(key) ? objectMapper.createArrayNode() : objectMapper.createObjectNode();
    }

    private static void fillUpArrayWithNull(ArrayNode array, Integer targetSize) {
        while (targetSize > array.size()) {
            array.addNull();
        }
    }

}
