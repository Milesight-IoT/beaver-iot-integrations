package com.milesight.beaveriot.integration.aws.service;

import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.api.ExchangeFlowExecutor;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.integration.model.event.ExchangeEvent;
import com.milesight.beaveriot.eventbus.annotations.EventSubscribe;
import com.milesight.beaveriot.eventbus.api.Event;
import com.milesight.beaveriot.integration.aws.constant.AwsIntegrationConstants;
import com.milesight.beaveriot.integration.aws.util.WeChatMessageUtils;
import com.milesight.beaveriot.parser.ParserPlugIn;
import com.milesight.beaveriot.integration.aws.entity.AwsConnectionPropertiesEntities;
import com.milesight.beaveriot.integration.aws.model.IntegrationStatus;
import com.milesight.beaveriot.parser.model.LnsAwsPayload;
import com.milesight.beaveriot.parser.model.ParserPayload;
import com.milesight.msc.sdk.utils.HMacUtils;
import com.milesight.msc.sdk.utils.TimeUtils;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import java.util.concurrent.atomic.AtomicInteger;


@Slf4j
@Service
public class AwsWebhookService {

    private static final String WEBHOOK_STATUS_KEY = AwsConnectionPropertiesEntities.getKey(AwsConnectionPropertiesEntities.Fields.webhookStatus);

    private static final int MAX_FAILURES = 10;

    private final AtomicInteger failureCount = new AtomicInteger(0);

    @Getter
    private boolean enabled = false;

    private Mac mac;

    @Autowired
    private EntityValueServiceProvider entityValueServiceProvider;

    @Autowired
    private ExchangeFlowExecutor exchangeFlowExecutor;

    @Autowired
    private DeviceServiceProvider deviceServiceProvider;

    @Autowired
    private ParserPlugIn parserPlugIn;

    @Autowired
    private AwsDataSyncService dataSyncService;

    @Autowired
    private WebhookPushService webhookPushService;

    @EventSubscribe(payloadKeyExpression = "aws-iot-integration.integration.webhook.*", eventType = ExchangeEvent.EventType.DOWN)
    public void onWebhookPropertiesUpdate(Event<AwsConnectionPropertiesEntities.Webhook> event) {
        enabled = Boolean.TRUE.equals(event.getPayload().getEnabled());
        if (event.getPayload().getSecretKey() != null && !event.getPayload().getSecretKey().isEmpty()) {
            mac = HMacUtils.getMac(event.getPayload().getSecretKey());
        } else {
            mac = null;
        }
    }

    public void handleWebhookData(LnsAwsPayload lnsAwsPayload) {
        val deviceId = lnsAwsPayload.getWirelessDeviceId();
        Device device = deviceServiceProvider.findByIdentifier(deviceId, AwsIntegrationConstants.INTEGRATION_IDENTIFIER);
        if (device == null) {
            log.warn("Device not found: {}", deviceId);
            return;
        }
        val sn = device.getName();
        if (sn == null) {
            log.warn("Device sn not found: {}", deviceId);
            return;
        }
        lnsAwsPayload.setSn(sn);
        try {
            ParserPayload parserPayload = ParserPayload.builder()
                    .deviceId(deviceId)
                    .sn(sn)
                    .ipsoData(lnsAwsPayload.getPayloadData())
                    .build();
            handleDeviceData(parserPayload);
        } catch (Exception e) {
            log.error("Handle webhook data failed", e);
        }
    }

    private void handleDeviceData(ParserPayload parserPayload) {
        try {
            val sn = parserPayload.getSn();
            val deviceId = parserPayload.getDeviceId();
            // 解码
            parserPlugIn.decode(parserPayload);
            log.info("Received data: {}", parserPayload);
            val deviceDatas = parserPayload.getThingData();
            if (deviceDatas == null) {
                log.warn("Device data not found");
                return;
            }
            deviceDatas.forEach(deviceData -> {
                log.info("Received data: {}", deviceData);
                if (!"PROPERTY".equalsIgnoreCase(deviceData.getType())
                        && !"EVENT".equalsIgnoreCase(deviceData.getType())) {
                    log.debug("Not tsl property or event: {}", deviceData.getType());
                    return;
                }
                val eventId = deviceData.getTslId();
                val data = deviceData.getPayload();
                val device = deviceServiceProvider.findByIdentifier(deviceId, AwsIntegrationConstants.INTEGRATION_IDENTIFIER);
                if (device == null) {
                    log.warn("Device not added, try to sync data: {}", sn);
                    dataSyncService.syncDeviceData(new AwsDataSyncService.Task(AwsDataSyncService.Task.Type.ADD_LOCAL_DEVICE, sn, null));
                    return;
                }
                // save data
                dataSyncService.saveHistoryData(device.getKey(), eventId, data, TimeUtils.currentTimeMillis() , true);
                try {
                    webhookPushService.webhookPush(WeChatMessageUtils.createDeviceMessage(sn, data));
                    // 异常值推送
                    if (parserPayload.getExceptions().isEmpty()){
                        return;
                    }
                    if (parserPayload.getExceptions().contains(deviceData.getId())){
                        webhookPushService.alarmPush(WeChatMessageUtils.createAlarmMessage(sn, deviceData.getId()));
                    }
                } catch (Exception e) {
                    log.error("Parser data failed", e);
                }
            });
        } catch (Exception e) {
            log.error("Parser data failed", e);
        }
    }

}
