package com.milesight.beaveriot.gateway.handle;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.api.ExchangeFlowExecutor;
import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.AttachTargetType;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.context.integration.model.EntityBuilder;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.entity.po.EntityPO;
import com.milesight.beaveriot.entity.repository.EntityRepository;
import com.milesight.beaveriot.gateway.GatewayConstants;
import com.milesight.beaveriot.gateway.model.PayloadCodecContent;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
@Slf4j
public class MqttDataHandle {

    // 待抽离成可配置
    public static final String host = "192.168.43.48";
    public static final String uplinke = "device/uplinke";
    public static final String downlink = "device/downlink";
    public static final int qos = 0;

    @Autowired
    private EntityServiceProvider entityServiceProvider;
    @Autowired
    private EntityRepository entityRepository;
    @Autowired
    private DeviceServiceProvider deviceServiceProvider;
    @Autowired
    private EntityValueServiceProvider entityValueServiceProvider;
    @Autowired
    private ExchangeFlowExecutor exchangeFlowExecutor;
    @Autowired
    private DeviceDataHandle deviceDataHandle;

    /**
     * 拉取所有payloadCodecContent
     *
     * @throws Exception
     */
    public void getAllPayloadCodecContent() throws Exception {
        JSONArray jsonArray = deviceDataHandle.getAllPayloadCodecContent();
        if (null == jsonArray || jsonArray.isEmpty()) {
            return;
        }
        Map<String, String> codecNameMap = new HashMap<>();
        for (Object obj : jsonArray) {
            try {
                PayloadCodecContent content = JSONUtil.toBean(JSONUtil.toJsonStr(obj), PayloadCodecContent.class);
                codecNameMap.put(content.getId(), content.getName());
            } catch (Exception e) {
                log.error("onGatewayIdUpdated obj:{} error: {}", JSONUtil.toJsonStr(obj), e.getMessage());
            }
        }
        if (codecNameMap.isEmpty()) {
            return;
        }
        // 更新codecName的enum
        Map<String, Map<String, String>> codecNameEnumMap = MapUtil.of("enum", codecNameMap);
        EntityPO entityPO = entityRepository.findOne(filter -> filter.eq(EntityPO.Fields.key, GatewayConstants.INTEGRATION_ID + ".integration.add_device.payload_codec_ID")).orElse(null);
        if (null == entityPO) {
            return;
        }
        entityRepository.deleteById(entityPO.getId());
        entityPO.setValueAttribute(Collections.unmodifiableMap(codecNameEnumMap));
        EntityPO entity = new EntityPO();
        BeanUtils.copyProperties(entityPO, entity);
        entityRepository.save(entity);
    }

    public void dataHandle(String msg) {
        if (StrUtil.isBlank(msg)) {
            return;
        }
        Map<String, Object> map = JsonUtils.toMap(msg);
        Object devEUIObj = map.get("devEUI");
        if (MapUtil.isEmpty(map) || null == devEUIObj) {
            return;
        }
        String devEUI = devEUIObj.toString().toUpperCase();
        Device device = deviceServiceProvider.findByKey(GatewayConstants.INTEGRATION_ID + ".device." + devEUI);
        if (null == device) {
            log.debug("device not exists, devEUI: " + devEUI);
            return;
        }
        // 添加实体
        addEntity(device, map);
        // 新增最新和历史数据
        saveHistoryData(device, map, System.currentTimeMillis(), true);
    }

    /**
     * 新增实体
     *
     * @param device
     */
    private void addEntity(Device device, Map<String, Object> map) {
        List<Entity> entities = entityServiceProvider.findByTargetId(AttachTargetType.DEVICE, device.getId().toString());
        List<String> oldNames = entities.stream().map(Entity::getName).toList();
        List<String> newNames = map.keySet().stream().filter(name -> !oldNames.contains(name)).toList();
        if (CollUtil.isEmpty(newNames)) {
            return;
        }
        List<Entity> entitieList = new ArrayList<>();
        for (String name : newNames) {
            entitieList.add(new EntityBuilder(GatewayConstants.INTEGRATION_ID, device.getKey())
                    .identifier(name)
                    .property(name, AccessMod.R)
                    .valueType(getValueType(map.get(name)))
                    .attributes(new HashMap<>())
                    .build());
        }
        entityServiceProvider.batchSave(entitieList);
    }

    private EntityValueType getValueType(Object value) {
        if (value instanceof String) {
            return EntityValueType.STRING;
        } else if (value instanceof Integer) {
            return EntityValueType.LONG;
        } else if (value instanceof Long) {
            return EntityValueType.LONG;
        } else if (value instanceof Float) {
            return EntityValueType.DOUBLE;
        } else if (value instanceof BigDecimal) {
            return EntityValueType.DOUBLE;
        } else if (value instanceof Double) {
            return EntityValueType.DOUBLE;
        } else if (value instanceof Boolean) {
            return EntityValueType.BOOLEAN;
        } else if (value instanceof Byte[]) {
            return EntityValueType.BINARY;
        } else if (value instanceof byte[]) {
            return EntityValueType.BINARY;
        } else if (value instanceof JsonNode) {
            return EntityValueType.OBJECT;
        } else if (value instanceof Map) {
            return EntityValueType.OBJECT;
        } else if (value instanceof List) {
            return EntityValueType.OBJECT;
        } else if (EntityValueType.hasEntitiesAnnotation(value.getClass())) {
            return EntityValueType.OBJECT;
        }
        return EntityValueType.STRING;
    }

    /**
     * 新增最新和历史数据
     *
     * @param device
     */
    public void saveHistoryData(Device device, Map<String, Object> map, long timestampMs, boolean isLatestData) {
        val result = new HashMap<String, Object>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            result.put(device.getKey() + "." + entry.getKey(), entry.getValue().toString());
        }
        val payload = ExchangePayload.create(result);
        if (payload == null || payload.isEmpty()) {
            return;
        }
        payload.setTimestamp(timestampMs);
        log.debug("Save device history data: {}", payload);
        if (!isLatestData) {
            entityValueServiceProvider.saveHistoryRecord(payload, payload.getTimestamp());
        } else {
            exchangeFlowExecutor.asyncExchangeUp(payload);
        }
    }

    public void mqttPublist(String content) {
        MemoryPersistence persistence = new MemoryPersistence();
        try {
            MqttClient client = new MqttClient("tcp://" + MqttDataHandle.host + ":1883", MqttDataHandle.downlink, persistence);
            // MQTT 连接选项
            MqttConnectOptions connOpts = new MqttConnectOptions();
            // 保留会话
            connOpts.setCleanSession(true);
            // 设置回调
            client.setCallback(new OnMessageCallback());
            // 建立连接
            client.connect(connOpts);
            // 消息发布所需参数
            MqttMessage message = new MqttMessage(content.getBytes());
            message.setQos(qos);
            client.publish(MqttDataHandle.uplinke, message);
            client.disconnect();
            System.out.println("Disconnected");
            client.close();
            System.exit(0);
        } catch (MqttException me) {
            me.printStackTrace();
        }
    }

}
