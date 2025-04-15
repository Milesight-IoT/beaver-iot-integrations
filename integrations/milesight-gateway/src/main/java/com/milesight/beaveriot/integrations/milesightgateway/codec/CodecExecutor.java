package com.milesight.beaveriot.integrations.milesightgateway.codec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import lombok.SneakyThrows;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * CodecExecutor class.
 *
 * @author simon
 * @date 2025/3/19
 */
public class CodecExecutor {
    private CodecExecutor() {}

    private static final ObjectMapper json = new ObjectMapper();
    private static final HostAccess HOST_ACCESS = HostAccess.newBuilder()
            .allowArrayAccess(true)
            .allowMapAccess(true)
            .allowListAccess(true)
            .build();

    private static final String LANGUAGE_ID = "js";

    private static Context buildCodeCtx() {
        return Context.newBuilder(LANGUAGE_ID)
                .allowHostAccess(HOST_ACCESS)
                .option("engine.WarnInterpreterOnly", "false")
                .build();
    }

    @SneakyThrows
    public static String runDecode(String code, Integer fPort, int[] data) {
        try (Context context = buildCodeCtx()) {
            context.eval(LANGUAGE_ID, code);
            Value func = context.getBindings(LANGUAGE_ID).getMember("Decode");
            return json.writeValueAsString(func.execute(fPort, data).as(Map.class));
        }
    }

    @SneakyThrows
    public static String runEncode(String code, Integer fPort, JsonNode data) {
        try (Context context = buildCodeCtx()) {
            context.eval(LANGUAGE_ID, code);
            Value binding = context.getBindings(LANGUAGE_ID);
            Value func = binding.getMember("Encode");
            Integer[] encodedData = func.execute(fPort, convertToJSObject(context, data)).as(Integer[].class);
            byte[] encodedBytes = new byte[encodedData.length];
            for (int i = 0; i < encodedData.length; i++) {
                encodedBytes[i] = encodedData[i].byteValue();
            }
            return Base64.getEncoder().encodeToString(encodedBytes);
        }
    }

    private static Value convertToJSObject(Context context, JsonNode jsonNode) {
        if (jsonNode.isObject()) {
            Value jsObject = context.eval("js", "({})");
            jsonNode.fieldNames().forEachRemaining(fieldName -> {
                JsonNode fieldValue = jsonNode.get(fieldName);
                jsObject.putMember(fieldName, convertToJSObject(context, fieldValue));
            });
            return jsObject;
        } else if (jsonNode.isArray()) {
            Value jsArray = context.eval("js", "([])");
            for (int i = 0; i < jsonNode.size(); i++) {
                jsArray.setArrayElement(i, convertToJSObject(context, jsonNode.get(i)));
            }
            return jsArray;
        } else if (jsonNode.isTextual()) {
            return context.asValue(jsonNode.asText());
        } else if (jsonNode.isNumber()) {
            return context.asValue(jsonNode.numberValue());
        } else if (jsonNode.isBoolean()) {
            return context.asValue(jsonNode.asBoolean());
        } else if (jsonNode.isNull()) {
            return context.eval("js", "null");
        } else {
            return context.asValue(jsonNode.toString());
        }
    }
}
