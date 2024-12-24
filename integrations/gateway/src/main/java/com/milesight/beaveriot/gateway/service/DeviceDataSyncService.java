package com.milesight.beaveriot.gateway.service;

import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.integration.model.event.ExchangeEvent;
import com.milesight.beaveriot.eventbus.annotations.EventSubscribe;
import com.milesight.beaveriot.eventbus.api.Event;
import com.milesight.beaveriot.gateway.GatewayConstants;
import com.milesight.beaveriot.gateway.GatewayIntegrationEntities;
import com.milesight.beaveriot.gateway.GatewayService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.*;

@Slf4j
@Getter
@Service
public class DeviceDataSyncService {

    @Autowired
    private EntityValueServiceProvider entityValueServiceProvider;
    @Autowired
    private GatewayService gatewayService;

    private Timer timer;

    private int periodSeconds = 0;

    // Only two existing tasks allowed at a time (one running and one waiting)
    private static final ExecutorService syncAllDataExecutor = new ThreadPoolExecutor(1, 1,
            0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(1),
            (r, executor) -> {
                throw new RejectedExecutionException("Another task is running.");
            });

    private static final ExecutorService concurrentSyncDeviceDataExecutor = new ThreadPoolExecutor(2, 4,
            300L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

    private static final ConcurrentHashMap<String, Object> deviceIdentifierToTaskLock = new ConcurrentHashMap<>(128);

    @EventSubscribe(payloadKeyExpression = GatewayConstants.INTEGRATION_ID + ".integration.scheduled_data_fetch.*", eventType = ExchangeEvent.EventType.DOWN)
    public void onScheduledDataFetchPropertiesUpdate(Event<GatewayIntegrationEntities.ScheduledDataFetch> event) {
        if (event.getPayload().getPeriod() != null) {
            periodSeconds = event.getPayload().getPeriod();
        }
        if (Boolean.TRUE.equals(event.getPayload().getEnabled())) {
            stop();
            start();
        } else {
            stop();
        }
    }

    public void init() {
        start();
    }

    public void stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        log.info("timer stopped");
    }

    public void start() {
        log.info("timer starting");
        if (timer != null) {
            return;
        }
        if (periodSeconds == 0) {
            val scheduledDataFetchSettings = entityValueServiceProvider.findValuesByKey(
                    GatewayIntegrationEntities.getKey(GatewayIntegrationEntities.Fields.scheduledDataFetch),
                    GatewayIntegrationEntities.ScheduledDataFetch.class);
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
                    syncAllDataExecutor.submit(() -> {
                        try {
                            gatewayService.pullDeviceData();
                        } catch (Exception e) {
                            log.error("Error pulling device data ", e);
                        }
                    });
                } catch (RejectedExecutionException e) {
                    log.error("Task rejected: ", e);
                }
            }
        }, periodMills, periodMills);

        log.info("timer started");
    }

}
