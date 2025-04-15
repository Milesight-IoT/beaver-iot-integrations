package com.milesight.beaveriot.integrations.milesightgateway.codec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * EntityValueConverter class.
 *
 * @author simon
 * @date 2025/3/19
 */
public class EntityValueConverter {
    private EntityValueConverter() {}

    private final static ObjectMapper json = new ObjectMapper();

    // json to Payload Map
    private static Map<String, Object> flatten(JsonNode node, String prefix, Map<String, Object> flatMap) {
        boolean isRoot = !prefix.contains(".");
        String structSeparator = isRoot ? "." : "#";
        String arraySeparator = isRoot ? ".[" : "[";
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String fieldName = prefix.isEmpty() ? field.getKey() : prefix + structSeparator + field.getKey();
                flatten(field.getValue(), fieldName, flatMap);
            }
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                flatten(node.get(i), prefix + arraySeparator + i + "]", flatMap);
            }
        } else {
            flatMap.put(prefix, node);
        }

        return flatMap;
    }

    public static Map<String, Object> convertToEntityKeyMap(String deviceKey, JsonNode jsonData) {
        return flatten(jsonData, "", new HashMap<>()).entrySet().stream()
                .collect(Collectors.toMap(entry -> deviceKey + "." + entry.getKey(), Map.Entry::getValue));
    }

    // Payload Map to json
    private static JsonNode unflatten(Map<String, Object> flatMap) {
        ObjectNode rootNode = ResourceString.jsonInstance().createObjectNode();

        for (Map.Entry<String, Object> entry : flatMap.entrySet()) {
            String[] keys = entry.getKey().split("\\.");
            addNode(rootNode, keys, 0, entry.getValue());
        }

        return rootNode;
    }

    private static void addNode(ObjectNode currentNode, String[] keys, int index, Object value) {
        String key = keys[index];
        if (index == keys.length - 1) {
            // 如果是最后一个键，直接设置值
            if (key.matches(".*\\[\\d+\\]$")) {
                String arrayKey = key.substring(0, key.indexOf('['));
                int arrayIndex = Integer.parseInt(key.substring(key.indexOf('[') + 1, key.indexOf(']')));
                ArrayNode arrayNode = currentNode.withArray(arrayKey);
                while (arrayIndex >= arrayNode.size()) {
                    arrayNode.addNull();
                }
                arrayNode.set(arrayIndex, ResourceString.jsonInstance().valueToTree(value));
            } else {
                currentNode.put(key, ResourceString.jsonInstance().valueToTree(value));
            }
        } else {
            // 处理中间节点
            if (key.matches(".*\\[\\d+\\]$")) {
                String arrayKey = key.substring(0, key.indexOf('['));
                int arrayIndex = Integer.parseInt(key.substring(key.indexOf('[') + 1, key.indexOf(']')));
                ArrayNode arrayNode = currentNode.withArray(arrayKey);
                while (arrayIndex >= arrayNode.size()) {
                    arrayNode.addNull();
                }
                JsonNode node = arrayNode.get(arrayIndex);
                ObjectNode nextNode;
                if (node == null || node.isNull()) {
                    nextNode = json.createObjectNode();
                    arrayNode.set(arrayIndex, nextNode);
                } else {
                    nextNode = (ObjectNode) node;
                }
                addNode(nextNode, keys, index + 1, value);
            } else {
                JsonNode node = currentNode.get(key);
                ObjectNode nextNode;
                if (node == null) {
                    nextNode = json.createObjectNode();
                    currentNode.set(key, nextNode);
                } else {
                    nextNode = (ObjectNode) node;
                }
                addNode(nextNode, keys, index + 1, value);
            }
        }
    }

    public static JsonNode convertToJson(String deviceKey, Map<String, Object> entityDataMap) {
        return unflatten(entityDataMap.entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().substring(deviceKey.length() + 1).replace(".[", "[").replace("#", "."), Map.Entry::getValue)));
    }
}
