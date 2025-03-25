package com.milesight.beaveriot.integrations.milesightgateway.codec

import com.milesight.beaveriot.context.integration.model.DeviceBuilder
import com.milesight.beaveriot.context.integration.model.Entity
import com.milesight.beaveriot.integrations.milesightgateway.codec.model.DeviceDef
import spock.lang.Specification

class ConvertTest extends Specification {
    private static void visualizeEntity(Entity entity) {
        if (entity.getParentIdentifier() != null) {
            println("\t" + "[" + entity.getKey() + "]: " + entity.getName() + "(" + entity.getValueType() + ")");
        } else {
            println("[" + entity.getKey() + "]: " + entity.getName() + "(" + entity.getValueType() + ")");
        }
    }

    def "test codec to device"() {
        given:
        def device = new DeviceBuilder("integration-id")
                .name("Test device")
                .identifier("test-eui")
                .build();
        when:
        def deviceDefFile = new File(getClass().getResource("/codec.json").toURI())
        def deviceDef = ResourceString.jsonInstance().readValue(deviceDefFile, DeviceDef.class)
        DeviceHelper.updateResourceInfo(device, deviceDef);
        then:
        device.getEntities().forEach {
            visualizeEntity(it)
            it.getChildren().forEach {
                visualizeEntity(it)
            }
        }
    }

    private Map<String, Object> getSampleJsonMap() {
        def entityMap = new HashMap<String, Object>();
        entityMap.put("integration-id.device.device-key.struct.arg3[3]", 4);
        entityMap.put("integration-id.device.device-key.struct.arg3[5]", 6);
        entityMap.put("integration-id.device.device-key.read_only_string", "Read Only String");
        entityMap.put("integration-id.device.device-key.struct.arg3[7]", 8);
        entityMap.put("integration-id.device.device-key.bool_field", 0);
        entityMap.put("integration-id.device.device-key.plain_array.[0]", 1);
        entityMap.put("integration-id.device.device-key.plain_array.[2]", 3);
        entityMap.put("integration-id.device.device-key.plain_array.[4]", 5);
        entityMap.put("integration-id.device.device-key.struct.arg3[1]", 2);
        entityMap.put("integration-id.device.device-key.plain_array.[6]", 7);
        entityMap.put("integration-id.device.device-key.float_unit_field", 26.6);
        entityMap.put("integration-id.device.device-key.struct.arg2#arg1", 1);
        entityMap.put("integration-id.device.device-key.struct.arg2#arg2", 0);
        entityMap.put("integration-id.device.device-key.struct.arg3[4]", 5);
        entityMap.put("integration-id.device.device-key.struct.arg3[6]", 7);
        entityMap.put("integration-id.device.device-key.struct.arg4[0]#arg2#arg1", 1);
        entityMap.put("integration-id.device.device-key.struct.arg4[0]#arg2#arg2", 2);
        entityMap.put("integration-id.device.device-key.struct.arg2#arg3#arg2", 1);
        entityMap.put("integration-id.device.device-key.plain_array.[1]", 2);
        entityMap.put("integration-id.device.device-key.struct.arg2#arg3#arg1", 0);
        entityMap.put("integration-id.device.device-key.plain_array.[3]", 4);
        entityMap.put("integration-id.device.device-key.struct.arg3[0]", 1);
        entityMap.put("integration-id.device.device-key.plain_array.[5]", 6);
        entityMap.put("integration-id.device.device-key.struct.arg3[2]", 3);
        entityMap.put("integration-id.device.device-key.plain_array.[7]", 8);
        entityMap.put("integration-id.device.device-key.struct.arg1", 0);
        entityMap.put("integration-id.device.device-key.struct.arg4[0]#arg1", 1);
        entityMap.put("integration-id.device.device-key.int_enum_field", 1);
        entityMap.put("integration-id.device.device-key.write_only_string", "Write Only String");
        entityMap.put("integration-id.device.device-key.int_field", 50);
        return entityMap;
    }

    def "test json to entity map"() {
        given:
        def jsonFile = new File(getClass().getResource("/downlink.json").toURI())
        def jsonNode = ResourceString.jsonInstance().readTree(jsonFile)
        when:
        def result = EntityValueConverter.convertToEntityKeyMap("integration-id.device.device-key", jsonNode);
        then:
        result.entrySet().forEach {println(it)}
    }

    def "test entity map to json"() {
        given:
        def deviceKey = "integration-id.device.device-key";

        when:
        def result = EntityValueConverter.convertToJson(deviceKey, getSampleJsonMap());
        def sampleJsonFile = new File(getClass().getResource("/downlink.json").toURI());
        def sampleJson = ResourceString.jsonInstance().readTree(sampleJsonFile);
        then:
        println(result)
    }
}
