package com.milesight.beaveriot.integration.msc.service;

import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.security.TenantContext;
import com.milesight.beaveriot.eventbus.annotations.EventSubscribe;
import com.milesight.beaveriot.eventbus.api.Event;
import com.milesight.beaveriot.integration.msc.constant.MscIntegrationConstants;
import com.milesight.beaveriot.integration.msc.entity.MscConnectionPropertiesEntities;
import com.milesight.beaveriot.integration.msc.model.IntegrationStatus;
import com.milesight.beaveriot.integration.msc.model.WebhookPayload;
import com.milesight.beaveriot.pubsub.MessagePubSub;
import com.milesight.beaveriot.pubsub.api.annotation.MessageListener;
import com.milesight.beaveriot.pubsub.api.message.RemoteBroadcastMessage;
import com.milesight.msc.sdk.utils.HMacUtils;
import com.milesight.msc.sdk.utils.TimeUtils;
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@Service
public class MscWebhookService {

    private static final String WEBHOOK_STATUS_KEY = MscConnectionPropertiesEntities.getKey(MscConnectionPropertiesEntities.Fields.webhookStatus);

    private final Map<String, WebhookContext> tenantIdToWebhookContext = new ConcurrentHashMap<>();

    @Autowired
    private EntityValueServiceProvider entityValueServiceProvider;

    @Autowired
    private DeviceServiceProvider deviceServiceProvider;

    @Lazy
    @Autowired
    private IMscClientProvider mscClientProvider;

    @Autowired
    private MscDataSyncService dataSyncService;

    @Autowired
    private MessagePubSub messagePubSub;

    public void init(String tenantId) {
        val webhookSettingsKey = MscConnectionPropertiesEntities.getKey(MscConnectionPropertiesEntities.Fields.webhook);
        val webhookSettings = entityValueServiceProvider.findValuesByKey(webhookSettingsKey, MscConnectionPropertiesEntities.Webhook.class);
        if (webhookSettings.isEmpty()) {
            log.info("Webhook settings not found");
            return;
        }
        val enabled = webhookSettings.getEnabled();
        val secretKey = webhookSettings.getSecretKey();
        initWebhookContext(tenantId, enabled, secretKey);
    }

    private void initWebhookContext(String tenantId, Boolean enabled, String secretKey) {
        if (!Boolean.TRUE.equals(enabled)) {
            log.debug("Webhook is disabled for tenant '{}'", tenantId);
            updateWebhookStatus(IntegrationStatus.NOT_READY);
            tenantIdToWebhookContext.remove(tenantId);
            return;
        }

        val webhookContext = tenantIdToWebhookContext.computeIfAbsent(tenantId, WebhookContext::new);
        if (secretKey != null && !secretKey.isEmpty() && !secretKey.equals(webhookContext.secretKey)) {
            webhookContext.mac = HMacUtils.getMac(secretKey);
            webhookContext.secretKey = secretKey;
        }
        webhookContext.enabled = true;
    }

    @EventSubscribe(payloadKeyExpression = "msc-integration.integration.webhook.*")
    public void onWebhookPropertiesUpdate(Event<MscConnectionPropertiesEntities.Webhook> event) {
        val tenantId = TenantContext.getTenantId();
        val webhookSettings = event.getPayload();
        initWebhookContext(tenantId, webhookSettings.getEnabled(), webhookSettings.getSecretKey());
        messagePubSub.publishAfterCommit(new MscWebhookContextUpdateEvent(tenantId, webhookSettings.getEnabled(), webhookSettings.getSecretKey()));
    }

    @MessageListener
    public void onMscWebhookContextUpdate(MscWebhookContextUpdateEvent event) {
        log.info("update webhook context for tenant '{}'", event.getTenantId());
        initWebhookContext(event.getTenantId(), event.getEnabled(), event.getSecretKey());
    }

    public void handleWebhookData(String signature,
                                  String webhookUuid,
                                  String requestTimestamp,
                                  String requestNonce,
                                  List<WebhookPayload> webhookPayloads) {

        if (log.isDebugEnabled()) {
            log.debug("Received webhook data: {} {} {} {} {}", signature, webhookUuid, requestTimestamp, requestNonce, webhookPayloads);
        } else {
            log.debug("Received webhook data, size: {}", webhookPayloads.size());
        }

        val tenantId = TenantContext.getTenantId();
        val webhookContext = tenantIdToWebhookContext.get(tenantId);
        if (webhookContext == null || !webhookContext.enabled) {
            log.debug("Webhook is disabled.");
            return;
        }

        val currentSeconds = TimeUtils.currentTimeSeconds();
        if (Long.parseLong(requestTimestamp) + 60 < currentSeconds) {
            log.warn("Webhook request outdated: {}", requestTimestamp);
            markWebhookStatusAsError();
            return;
        }

        if (!isSignatureValid(signature, requestTimestamp, requestNonce, webhookContext)) {
            log.warn("Signature invalid: {}", signature);
            markWebhookStatusAsError();
            return;
        }

        webhookPayloads.forEach(webhookPayload -> {
            log.debug("Receive webhook payload: {}", webhookPayload);
            val eventType = webhookPayload.getEventType();
            if (eventType == null) {
                log.warn("Event type not found");
                return;
            }

            // webhook is ready
            updateWebhookStatus(IntegrationStatus.READY);

            if ("device_data".equalsIgnoreCase(eventType)) {
                try {
                    handleDeviceData(webhookPayload);
                } catch (Exception e) {
                    log.error("Handle webhook data failed", e);
                }
            } else {
                log.debug("Ignored event type: {}", eventType);
            }
        });
    }

    /**
     * mark as error when continuously failed to validate signature or timestamp
     */
    private void markWebhookStatusAsError() {
        val tenantId = TenantContext.getTenantId();
        val webhookContext = tenantIdToWebhookContext.get(tenantId);
        if (webhookContext == null) {
            return;
        }
        updateWebhookStatus(IntegrationStatus.ERROR);
    }

    private void updateWebhookStatus(@NonNull IntegrationStatus status) {
        val tenantId = TenantContext.getTenantId();
        val webhookContext = tenantIdToWebhookContext.get(tenantId);
        if (webhookContext == null) {
            return;
        }
        entityValueServiceProvider.saveValuesAndPublishAsync(ExchangePayload.create(WEBHOOK_STATUS_KEY, status.name()));
    }

    private void handleDeviceData(WebhookPayload webhookPayload) {
        if (webhookPayload.getData() == null) {
            log.warn("Webhook data is null: {}", webhookPayload);
            return;
        }
        val client = mscClientProvider.getMscClient();
        val deviceData = client.getObjectMapper().convertValue(webhookPayload.getData(), WebhookPayload.DeviceData.class);
        if (!"PROPERTY".equalsIgnoreCase(deviceData.getType())
                && !"EVENT".equalsIgnoreCase(deviceData.getType())) {
            log.debug("Not tsl property or event: {}", deviceData.getType());
            return;
        }
        val eventId = deviceData.getTslId();
        val data = deviceData.getPayload();
        val ts = deviceData.getTs() != null ? deviceData.getTs() : webhookPayload.getEventCreatedTime() * 1000;
        val profile = deviceData.getDeviceProfile();
        if (data == null || profile == null) {
            log.warn("Invalid data: {}", deviceData);
            return;
        }

        val sn = deviceData.getDeviceProfile().getSn();
        val device = deviceServiceProvider.findByIdentifier(sn, MscIntegrationConstants.INTEGRATION_IDENTIFIER);
        if (device == null) {
            log.warn("Device not added, try to sync data: {}", sn);
            dataSyncService.syncDeviceData(new MscDataSyncService.Task(MscDataSyncService.Task.Type.ADD_LOCAL_DEVICE, sn, null));
            return;
        }

        // save data
        dataSyncService.saveHistoryData(device.getKey(), eventId, data, ts, true);
    }

    public boolean isSignatureValid(String signature, String requestTimestamp, String requestNonce, WebhookContext webhookContext) {
        val mac = webhookContext.getMac();
        if (mac != null) {
            val expectedSignature = HMacUtils.digestHex(mac, String.format("%s%s", requestTimestamp, requestNonce));
            return expectedSignature.equals(signature);
        }
        return true;
    }

    public void disable(String tenantId) {
        val exchangePayload = ExchangePayload.create(Map.of(
                MscConnectionPropertiesEntities.Webhook.getKey(MscConnectionPropertiesEntities.Webhook.Fields.enabled), "false",
                WEBHOOK_STATUS_KEY, IntegrationStatus.NOT_READY.name()
        ));
        entityValueServiceProvider.saveValuesAndPublishSync(exchangePayload);
    }

    public void stop() {
        tenantIdToWebhookContext.clear();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WebhookContext {
        private String tenantId;
        private boolean enabled;
        private String secretKey;
        private Mac mac;

        public WebhookContext(String tenantId) {
            this.tenantId = tenantId;
        }
    }

    @Data
    @ToString(callSuper = true)
    @EqualsAndHashCode(callSuper = true)
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MscWebhookContextUpdateEvent extends RemoteBroadcastMessage {
        private String tenantId;
        private Boolean enabled;
        private String secretKey;
    }

}
