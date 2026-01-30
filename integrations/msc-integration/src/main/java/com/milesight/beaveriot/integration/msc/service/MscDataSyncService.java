package com.milesight.beaveriot.integration.msc.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.milesight.beaveriot.base.annotations.shedlock.DistributedLock;
import com.milesight.beaveriot.base.annotations.shedlock.LockScope;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.api.DeviceStatusServiceProvider;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.constants.ExchangeContextKeys;
import com.milesight.beaveriot.context.i18n.locale.LocaleContext;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.security.SecurityUserContext;
import com.milesight.beaveriot.context.security.TenantContext;
import com.milesight.beaveriot.eventbus.annotations.EventSubscribe;
import com.milesight.beaveriot.eventbus.api.Event;
import com.milesight.beaveriot.integration.msc.constant.MscErrorCode;
import com.milesight.beaveriot.integration.msc.constant.MscIntegrationConstants;
import com.milesight.beaveriot.integration.msc.entity.MscConnectionPropertiesEntities;
import com.milesight.beaveriot.integration.msc.entity.MscServiceEntities;
import com.milesight.beaveriot.integration.msc.model.IntegrationStatus;
import com.milesight.beaveriot.integration.msc.util.MscTslUtils;
import com.milesight.beaveriot.scheduler.integration.IntegrationScheduled;
import com.milesight.cloud.sdk.client.model.DeviceDetailResponse;
import com.milesight.cloud.sdk.client.model.DeviceSearchRequest;
import com.milesight.msc.sdk.error.MscSdkException;
import com.milesight.msc.sdk.utils.TimeUtils;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.spring.aop.ScopedLockConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Getter
@Service
public class MscDataSyncService {

    @Lazy
    @Autowired
    private IMscClientProvider mscClientProvider;

    @Lazy
    @Autowired
    private MscDeviceService mscDeviceService;

    @Autowired
    private DeviceServiceProvider deviceServiceProvider;

    @Autowired
    private DeviceStatusServiceProvider deviceStatusServiceProvider;

    @Autowired
    private EntityValueServiceProvider entityValueServiceProvider;

    @Autowired
    private LockProvider lockProvider;

    @Lazy
    @Autowired
    private MscDataSyncService self;

    private final ExecutorService executor = new ThreadPoolExecutor(2, 20,
            300L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

    @EventSubscribe(payloadKeyExpression = "msc-integration.integration.openapi_status")
    public void onOpenapiStatusUpdate(Event<MscConnectionPropertiesEntities> event) {
        val status = event.getPayload().getOpenapiStatus();
        if (IntegrationStatus.READY.name().equals(status)) {
            runSyncTask(true);
        }
    }

    private void updateScheduled(boolean enabled) {
        val exchangePayload = ExchangePayload.create(
                MscConnectionPropertiesEntities.ScheduledDataFetch.getKey(
                        MscConnectionPropertiesEntities.ScheduledDataFetch.Fields.enabled), enabled);
        entityValueServiceProvider.saveValuesAndPublishSync(exchangePayload);
    }

    private Future<?> runSyncTask(boolean delta) {
        val tenantId = TenantContext.getTenantId();
        val user = SecurityUserContext.getSecurityUser();
        val locale = LocaleContext.getLocale();
        return executor.submit(() -> {
            TenantContext.setTenantId(tenantId);
            SecurityUserContext.setSecurityUser(user);
            LocaleContext.setLocale(locale);

            val lockConfiguration = ScopedLockConfiguration.builder(LockScope.TENANT)
                    .name("msc-integration:sync-all")
                    .lockAtLeastFor(Duration.ofSeconds(0))
                    .lockAtMostFor(Duration.ofMinutes(10))
                    .throwOnLockFailure(false)
                    .build();
            lockProvider.lock(lockConfiguration).ifPresentOrElse(lock -> {
                try {
                    log.info("Fetching data from MSC");
                    if (delta) {
                        log.info("Only sync differential data");
                    }
                    syncAllDeviceData(delta);
                } catch (Exception e) {
                    log.error("Error occurred while retrieving data from MSC.", e);
                    if (e instanceof MscSdkException mscSdkException) {
                        throw MscErrorCode.wrap(mscSdkException).build();
                    }
                } finally {
                    lock.unlock();
                }
            }, () -> {
                log.info("Another task is running, skipping this task");
                throw ServiceException.with(MscErrorCode.MSC_ANOTHER_TASK_RUNNING).build();
            });
        });
    }

    @SneakyThrows
    @EventSubscribe(payloadKeyExpression = "msc-integration.integration.sync_device")
    public void onSyncDevice(Event<MscServiceEntities.SyncDevice> event) {
        val openApiStatus = entityValueServiceProvider.findValueByKey(
                MscConnectionPropertiesEntities.getKey(MscConnectionPropertiesEntities.Fields.openapiStatus));
        if (!IntegrationStatus.READY.name().equalsIgnoreCase(String.valueOf(openApiStatus))) {
            throw ServiceException.with(MscErrorCode.MSC_CONNECTION_NOT_READY).build();
        }
        runSyncTask(false).get();
    }

    public void disable(String tenantId) {
        updateScheduled(false);
    }

    @IntegrationScheduled(
            name = "msc-integration.sync",
            fixedRateEntity = "msc-integration.integration.scheduled_data_fetch.period",
            enabledEntity = "msc-integration.integration.scheduled_data_fetch.enabled"
    )
    public void scheduledSync() {
        // ensure user context is empty
        SecurityUserContext.clear(false);
        runSyncTask(true);
    }

    @SneakyThrows
    void syncAllDeviceData(boolean delta) {
        if (mscClientProvider == null || mscClientProvider.getMscClient() == null) {
            log.warn("MscClient not initiated.");
            return;
        }
        syncDevicesFromMsc();
        syncDeviceHistoryDataFromMsc(delta);
    }

    void syncDevicesFromMsc() throws IOException {
        log.info("Sync devices from MSC.");
        val mscClient = mscClientProvider.getMscClient();
        val allDevices = deviceServiceProvider.findAll(MscIntegrationConstants.INTEGRATION_IDENTIFIER);
        log.info("Found {} devices from local.", allDevices.size());
        val existingDevices = allDevices.stream().map(Device::getIdentifier).collect(Collectors.toSet());
        long pageNumber = 1;
        long pageSize = 10;
        long total = 0;
        long fetched = -1;
        while (fetched < total) {
            val response = mscClient.device().searchDetails(new DeviceSearchRequest()
                            .pageSize(pageSize)
                            .pageNumber(pageNumber))
                    .execute()
                    .body();
            if (response == null || response.getData() == null || response.getData().getTotal() == null) {
                log.warn("Response is empty: {}", response);
                return;
            }
            val list = response.getData().getContent();
            if (list == null || list.isEmpty()) {
                log.warn("Content is empty.");
                return;
            }
            pageNumber++;
            fetched += pageSize;
            total = response.getData().getTotal();

            val syncDeviceTasks = list.stream().map(details -> {
                val identifier = details.getSn();
                if (identifier == null) {
                    return CompletableFuture.completedFuture(null);
                }
                var type = Task.Type.ADD_LOCAL_DEVICE;
                if (existingDevices.contains(identifier)) {
                    existingDevices.remove(identifier);
                    type = Task.Type.UPDATE_LOCAL_DEVICE;
                }
                return syncDeviceData(new Task(type, identifier, details));
            }).toArray(CompletableFuture[]::new);
            CompletableFuture.allOf(syncDeviceTasks).join();
        }
        log.info("Pull devices from MSC finished, total devices: {}", total);

        val removeDevicesTasks = existingDevices.stream()
                .map(identifier -> syncDeviceData(new Task(Task.Type.REMOVE_LOCAL_DEVICE, identifier, null)))
                .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(removeDevicesTasks).join();
    }

    void syncDeviceHistoryDataFromMsc(boolean delta) {
        log.info("Sync device history data from MSC.");
        val allDevices = deviceServiceProvider.findAll(MscIntegrationConstants.INTEGRATION_IDENTIFIER);
        log.info("Found {} devices from local.", allDevices.size());
        allDevices.forEach(device -> {
            try {
                long lastSyncTime = 0;
                if (delta) {
                    lastSyncTime = getAndUpdateLastSyncTime(device);
                }
                syncPropertiesHistory(device, lastSyncTime);
                // events and services are not supported yet
            } catch (Exception e) {
                log.error("Error occurs while syncing device history data from MSC, device key: {}", device.getKey(), e);
            }
        });
        log.info("Sync device history data from MSC finished, total devices: {}", allDevices.size());
    }

    public CompletableFuture<Boolean> syncDeviceData(Task task) {
        val lockConfiguration = ScopedLockConfiguration.builder(LockScope.TENANT)
                .name("msc-integration:sync-device:" + task.identifier)
                .lockAtLeastFor(Duration.ofSeconds(0))
                .lockAtMostFor(Duration.ofSeconds(30))
                .throwOnLockFailure(false)
                .build();
        val lock = lockProvider.lock(lockConfiguration).orElse(null);
        if (lock == null) {
            log.info("Skip execution because device task is running: {}", task.identifier);
            return CompletableFuture.completedFuture(null);
        }

        val tenantId = TenantContext.getTenantId();
        val user = SecurityUserContext.getSecurityUser();
        val locale = LocaleContext.getLocale();
        return CompletableFuture.supplyAsync(() -> {
            try {
                TenantContext.setTenantId(tenantId);
                SecurityUserContext.setSecurityUser(user);
                LocaleContext.setLocale(locale);

                Device device = null;
                switch (task.type) {
                    case ADD_LOCAL_DEVICE -> device = addLocalDevice(task);
                    case UPDATE_LOCAL_DEVICE -> device = updateLocalDevice(task);
                    default -> {
                        // do nothing
                    }
                }

                if (task.type != Task.Type.REMOVE_LOCAL_DEVICE && device == null) {
                    log.warn("Add or update local device failed: {}", task.identifier);
                    return false;
                }
                return true;
            } catch (Exception e) {
                log.error("Error while syncing local device data.", e);
                return false;
            } finally {
                lock.unlock();
            }
        }, executor);
    }

    private long getAndUpdateLastSyncTime(Device device) {
        // update last sync time
        val timestamp = TimeUtils.currentTimeSeconds();
        val lastSyncTimeKey = MscIntegrationConstants.InternalPropertyIdentifier.getLastSyncTimeKey(device.getKey());
        val lastSyncTime = Optional.ofNullable(entityValueServiceProvider.findValueByKey(lastSyncTimeKey))
                .map(n -> (long) n)
                .orElse(0L);
        entityValueServiceProvider.saveValuesAndPublishSync(ExchangePayload.create(lastSyncTimeKey, timestamp));
        return lastSyncTime;
    }

    @SneakyThrows
    private void syncPropertiesHistory(Device device, long lastSyncTime) {
        // deviceId should not be null
        val deviceId = (String) device.getAdditional().get(MscIntegrationConstants.DeviceAdditionalDataName.DEVICE_ID);
        long time24HoursBefore = TimeUtils.currentTimeSeconds() - TimeUnit.DAYS.toSeconds(1);
        long startTime = Math.max(lastSyncTime, time24HoursBefore) * 1000;
        long endTime = TimeUtils.currentTimeMillis();
        long pageSize = 100;
        String pageKey = null;
        boolean hasNextPage = true;
        val isLatestData = new AtomicBoolean(true);
        while (hasNextPage) {
            val page = mscClientProvider.getMscClient()
                    .device()
                    .getPropertiesHistory(deviceId, startTime, endTime, pageSize, pageKey, null)
                    .execute()
                    .body();
            if (page == null || page.getData() == null || page.getData().getList() == null) {
                log.warn("Response is empty.");
                break;
            }
            pageKey = page.getData().getNextPageKey();
            hasNextPage = pageKey != null;
            page.getData().getList().forEach(item -> {
                val objectMapper = mscClientProvider.getMscClient().getObjectMapper();
                val properties = objectMapper.convertValue(item.getProperties(), JsonNode.class);
                val timestamp = item.getTs() != null ? item.getTs() : TimeUtils.currentTimeMillis();
                self.saveHistoryData(device.getKey(), null, properties, timestamp, isLatestData.get());
                if (isLatestData.get()) {
                    isLatestData.set(false);
                }
            });
        }
    }

    @DistributedLock(name = "msc-integration:saveHistoryData(#{#p0},#{#p3})", waitForLock = "5s")
    public void saveHistoryData(String deviceKey, String eventId, JsonNode data, long timestampMs, boolean isLatestData) {
        val payload = eventId == null
                ? MscTslUtils.convertJsonNodeToExchangePayload(deviceKey, data)
                : MscTslUtils.convertJsonNodeToExchangePayload(String.format("%s.%s", deviceKey, eventId), data, false);
        if (payload == null || payload.isEmpty()) {
            return;
        }
        payload.setTimestamp(timestampMs);

        val existingKeys = entityValueServiceProvider.existHistoryRecord(payload.keySet(), timestampMs);
        log.debug("Existing keys: {}, ts: {}", existingKeys, timestampMs);
        payload.entrySet().removeIf(entry -> existingKeys.contains(entry.getKey()));
        if (payload.isEmpty()) {
            log.debug("Nothing updated: {}, {}", deviceKey, eventId);
            return;
        }

        log.debug("Save device history data: {}", payload);
        if (!isLatestData) {
            entityValueServiceProvider.saveHistoryRecord(payload, payload.getTimestamp());
        } else {
            payload.putContext(ExchangeContextKeys.EXCHANGE_IGNORE_INVALID_KEY, true);
            entityValueServiceProvider.saveValuesAndPublishAsync(payload, MscIntegrationConstants.EventType.LATEST_VALUE);
        }
    }

    @SneakyThrows
    private Device updateLocalDevice(Task task) {
        log.info("Update local device: {}", task.identifier);
        val details = getDeviceDetails(task);
        val deviceId = details.getDeviceId();
        val thingSpec = mscDeviceService.getThingSpec(String.valueOf(deviceId));
        val device = mscDeviceService.updateLocalDevice(task.identifier, String.valueOf(deviceId), thingSpec);
        updateDeviceStatus(device, details.getConnectStatus());
        return device;
    }

    private void updateDeviceStatus(Device device, DeviceDetailResponse.ConnectStatusEnum connectStatus) {
        if (DeviceDetailResponse.ConnectStatusEnum.ONLINE.equals(connectStatus)) {
            deviceStatusServiceProvider.online(device);
        } else {
            deviceStatusServiceProvider.offline(device);
        }
    }

    @SneakyThrows
    private Device addLocalDevice(Task task) {
        log.info("Add local device: {}", task.identifier);
        val details = getDeviceDetails(task);
        val deviceId = details.getDeviceId();
        val thingSpec = mscDeviceService.getThingSpec(String.valueOf(deviceId));
        val device = mscDeviceService.addLocalDevice(task.identifier, details.getName(), String.valueOf(deviceId), thingSpec);
        updateDeviceStatus(device, details.getConnectStatus());
        return device;
    }

    @SuppressWarnings("ConstantConditions")
    private DeviceDetailResponse getDeviceDetails(Task task)
            throws IOException, NullPointerException, IndexOutOfBoundsException {

        var details = task.details;
        if (details == null) {
            details = mscClientProvider.getMscClient().device().searchDetails(DeviceSearchRequest.builder()
                            .sn(task.identifier)
                            .pageNumber(1L)
                            .pageSize(1L)
                            .build())
                    .execute()
                    .body()
                    .getData()
                    .getContent()
                    .get(0);
        }
        return details;
    }

    public void stop() {
        executor.shutdownNow();
    }

    public record Task(@Nonnull Type type, @Nonnull String identifier, @Nullable DeviceDetailResponse details) {

        public enum Type {
            ADD_LOCAL_DEVICE,
            UPDATE_LOCAL_DEVICE,
            REMOVE_LOCAL_DEVICE,
            ;
        }

    }

}
