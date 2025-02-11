package com.milesight.beaveriot.integration.aws.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.api.ExchangeFlowExecutor;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.integration.model.event.ExchangeEvent;
import com.milesight.beaveriot.eventbus.annotations.EventSubscribe;
import com.milesight.beaveriot.eventbus.api.Event;
import com.milesight.beaveriot.integration.aws.constant.AwsIntegrationConstants;
import com.milesight.beaveriot.integration.aws.entity.AwsConnectionPropertiesEntities;
import com.milesight.beaveriot.integration.aws.entity.AwsServiceEntities;
import com.milesight.beaveriot.integration.aws.model.IntegrationStatus;
import com.milesight.beaveriot.integration.aws.sdk.service.IIotStrategy;
import com.milesight.beaveriot.integration.aws.util.AwsTslUtils;
import com.milesight.beaveriot.parser.ParserPlugIn;
import com.milesight.cloud.sdk.client.model.DeviceDetailResponse;
import com.milesight.cloud.sdk.client.model.ThingSpec;
import com.milesight.msc.sdk.utils.TimeUtils;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.iotwireless.model.WirelessDeviceStatistics;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Getter
@Service
public class AwsDataSyncService {

    @Lazy
    @Autowired
    private AwsDeviceService awsDeviceService;

    @Autowired
    private DeviceServiceProvider deviceServiceProvider;

    @Autowired
    private EntityValueServiceProvider entityValueServiceProvider;

    @Autowired
    private ExchangeFlowExecutor exchangeFlowExecutor;

    @Autowired
    private IIotStrategy iotStrategy;

    private Timer timer;

    private int periodSeconds = 0;

    @Autowired
    private ParserPlugIn parserPlugIn;

    // Only two existing tasks allowed at a time (one running and one waiting)
    private static final ExecutorService syncAllDataExecutor = new ThreadPoolExecutor(1, 1,
            0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(1),
            (r, executor) -> {
                throw new RejectedExecutionException("Another task is running.");
            });

    private static final ExecutorService concurrentSyncDeviceDataExecutor = new ThreadPoolExecutor(2, 4,
            300L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

    private static final ConcurrentHashMap<String, Object> deviceIdentifierToTaskLock = new ConcurrentHashMap<>(128);

    @EventSubscribe(payloadKeyExpression = "aws-iot-integration.integration.scheduled_data_fetch.*", eventType = ExchangeEvent.EventType.DOWN)
    public void onScheduledDataFetchPropertiesUpdate(Event<AwsConnectionPropertiesEntities.ScheduledDataFetch> event) {
        if (event.getPayload().getPeriod() != null) {
            periodSeconds = event.getPayload().getPeriod();
        }
        restart();
    }

    @EventSubscribe(payloadKeyExpression = "aws-iot-integration.integration.openapi_status", eventType = ExchangeEvent.EventType.DOWN)
    public void onOpenapiStatusUpdate(Event<AwsConnectionPropertiesEntities> event) {
        val status = event.getPayload().getOpenapiStatus();
        if (IntegrationStatus.READY.name().equals(status)) {
            try {
                syncAllDataExecutor.submit(this::syncDeltaData);
            } catch (RejectedExecutionException  e) {
                log.error("Task rejected: ", e);
            }
        }
    }

    @SneakyThrows
    @EventSubscribe(payloadKeyExpression = "aws-iot-integration.integration.sync_device", eventType = ExchangeEvent.EventType.DOWN)
    public void onSyncDevice(Event<AwsServiceEntities.SyncDevice> event) {
        syncAllDataExecutor.submit(this::syncAllData).get();
    }


    public void restart() {
        stop();
        start();
    }

    public void stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        log.info("timer stopped");
    }

    public void init() {
        start();
    }

    public void start() {
        log.info("timer starting");
        if (timer != null) {
            return;
        }
        if (periodSeconds == 0) {
            val scheduledDataFetchSettings = entityValueServiceProvider.findValuesByKey(
                    AwsConnectionPropertiesEntities.getKey(AwsConnectionPropertiesEntities.Fields.scheduledDataFetch),
                    AwsConnectionPropertiesEntities.ScheduledDataFetch.class);
            if (scheduledDataFetchSettings.isEmpty()) {
                periodSeconds = -1;
                return;
            }
            if (!Boolean.TRUE.equals(scheduledDataFetchSettings.getEnabled())
                    || scheduledDataFetchSettings.getPeriod() == null
                    || scheduledDataFetchSettings.getPeriod() == 0) {
                // not enabled or invalid period
                periodSeconds = -1;
            } else if (scheduledDataFetchSettings.getPeriod() > 0) {
                periodSeconds = scheduledDataFetchSettings.getPeriod();
            }
        }
        if (periodSeconds < 0) {
            return;
        }
        timer = new Timer();

        // setup timer
        val periodMills = periodSeconds * 1000L;
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    syncAllDataExecutor.submit(() -> syncDeltaData());
                } catch (RejectedExecutionException e) {
                    log.error("Task rejected: ", e);
                }
            }
        }, periodMills, periodMills);

        log.info("timer started");
    }

    /**
     * Pull data from Aws, all devices and part of history data which created after the last execution will be added to local storage.
     */
    private void syncDeltaData() {
        log.info("Fetching delta data from MSC");
        try {
            syncAllDeviceData(true);
        } catch (Exception e) {
            log.error("Error while fetching delta data from MSC", e);
        }
    }

    /**
     * Pull all devices and all history data.
     */
    private void syncAllData() {
        log.info("Fetching all data from MSC");
        try {
            syncAllDeviceData(false);
        } catch (Exception e) {
            log.error("Error while fetching all data from MSC", e);
        }
    }

    private Object markDeviceTaskRunning(String identifier, boolean force) {
        var lock = deviceIdentifierToTaskLock.get(identifier);
        if (force && lock != null) {
            return lock;
        } else if (lock == null) {
            lock = new Object();
            val previous = deviceIdentifierToTaskLock.putIfAbsent(identifier, lock);
            if (previous == null) {
                return lock;
            } else {
                // put value failed
                if (force) {
                    return previous;
                }
                return null;
            }
        } else {
            return null;
        }
    }

    private void markDeviceTaskFinished(String identifier, Object lock) {
        deviceIdentifierToTaskLock.remove(identifier, lock);
    }

    @SneakyThrows
    private void syncAllDeviceData(boolean delta) {
        syncDevicesFromAws();
    }

    private void syncDevicesFromAws() throws Exception {
        val allDevices = deviceServiceProvider.findAll(AwsIntegrationConstants.INTEGRATION_IDENTIFIER);
        val existingDevices = allDevices.stream().map(Device::getIdentifier).collect(Collectors.toSet());
        long pageNumber = 1;
        long pageSize = 10;
        long total = 10;
        long fetched = -1;
        List<WirelessDeviceStatistics> wirelessDeviceStatistics = iotStrategy.listWirelessDevices(null, null, null, null);
        while (fetched < total) {
            fetched += pageSize;
            total = wirelessDeviceStatistics.size();
            val syncDeviceTasks = wirelessDeviceStatistics.stream().map(details -> {
                val sn = details.name();
                val identifier = details.id();
                if (identifier == null) {
                    return CompletableFuture.completedFuture(null);
                }
                var type = Task.Type.ADD_LOCAL_DEVICE;
                if (existingDevices.contains(identifier)) {
                    existingDevices.remove(identifier);
                    type = Task.Type.UPDATE_LOCAL_DEVICE;
                }
                DeviceDetailResponse device = DeviceDetailResponse.builder()
                        .sn(sn)
                        .name(sn)
                        .build();
                return syncDeviceData(new Task(type, identifier, device));
            }).toArray(CompletableFuture[]::new);
            CompletableFuture.allOf(syncDeviceTasks);
        }
        log.info("Pull devices from AWS finished, total devices: {}", total);

        val removeDevicesTasks = existingDevices.stream()
                .map(identifier -> syncDeviceData(new Task(Task.Type.REMOVE_LOCAL_DEVICE, identifier, null)))
                .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(removeDevicesTasks);
    }


    public CompletableFuture<Boolean> syncDeviceData(Task task) {
        // if fetching or removing data, then return
        val lock = markDeviceTaskRunning(task.identifier, task.type == Task.Type.REMOVE_LOCAL_DEVICE);
        if (lock == null) {
            log.info("Skip execution because device task is running: {}", task.identifier);
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                Device device = null;
                switch (task.type) {
                    case REMOVE_LOCAL_DEVICE -> device = removeLocalDevice(task.identifier);
                    case ADD_LOCAL_DEVICE -> device = addLocalDevice(task);
                    case UPDATE_LOCAL_DEVICE -> device = updateLocalDevice(task);
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
                markDeviceTaskFinished(task.identifier, lock);
            }
        }, concurrentSyncDeviceDataExecutor);
    }

    public void saveHistoryData(String deviceKey, String eventId, JsonNode data, long timestampMs, boolean isLatestData) {
        val payload = eventId == null
                ? AwsTslUtils.convertJsonNodeToExchangePayload(deviceKey, data)
                : AwsTslUtils.convertJsonNodeToExchangePayload(String.format("%s.%s", deviceKey, eventId), data);
        if (payload == null || payload.isEmpty()) {
            return;
        }
        payload.setTimestamp(timestampMs);
        log.debug("Save device history data: {}", payload);
        if (!isLatestData) {
            entityValueServiceProvider.saveHistoryRecord(payload, payload.getTimestamp());
        } else {
            exchangeFlowExecutor.asyncExchangeUp(payload);
            entityValueServiceProvider.saveHistoryRecord(payload, payload.getTimestamp());
        }
    }

    @SneakyThrows
    private Device updateLocalDevice(Task task) {
        log.info("Update local device: {}", task.identifier);
        val details = getDeviceDetails(task);
        ThingSpec thingSpec = getThingSpec(details);
        if (thingSpec == null) {
            return null;
        }
        return awsDeviceService.updateLocalDevice(task.identifier, task.identifier, thingSpec);
    }

    private ThingSpec getThingSpec(DeviceDetailResponse details) {
        // 获取产品
        val product = parserPlugIn.getProductBySn(details.getSn());
        if (product == null) {
            return null;
        }
        // 物模型
        val thingSpecs = product.getThingSpecifications();
        // 使用 Jackson 进行反序列化
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true);
        ThingSpec thingSpec = objectMapper.convertValue(thingSpecs.get(0), ThingSpec.class);
        return thingSpec;
    }

    @SneakyThrows
    private Device addLocalDevice(Task task) {
        log.info("Add local device: {}", task.identifier);
        val details = getDeviceDetails(task);
        ThingSpec thingSpec = getThingSpec(details);
        if (thingSpec == null) {
            return null;
        }
        return awsDeviceService.addLocalDevice(task.identifier, details.getName(), details.getSn(),null, thingSpec);
    }

    @SuppressWarnings("ConstantConditions")
    private DeviceDetailResponse getDeviceDetails(Task task)
            throws IOException, NullPointerException, IndexOutOfBoundsException {

        var details = task.details;
        return details;
    }

    private Device removeLocalDevice(String identifier) {
        // delete is unsupported currently
        return null;
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
