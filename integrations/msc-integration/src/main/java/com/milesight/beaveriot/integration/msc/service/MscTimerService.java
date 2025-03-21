package com.milesight.beaveriot.integration.msc.service;

import com.milesight.msc.sdk.utils.TimeUtils;
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;


@Slf4j
@Service
public class MscTimerService {

    private final ConcurrentSkipListMap<Long, ConcurrentHashMap<String, Task>> epochSecondsToTasks = new ConcurrentSkipListMap<>();

    private final ConcurrentHashMap<String, Task> tenantIdToTask = new ConcurrentHashMap<>();

    private final Object schedulerLock = new Object();

    private Timer timer;

    private void ensureTimer() {
        // ensure timer exists and not cancelled
        synchronized (schedulerLock) {
            if (timer == null) {
                val t = new Timer("MscTimerService");
                t.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            scanAndRunTasks();
                        } catch (Exception e) {
                            log.error("Schedule task execution error", e);
                        }
                    }
                }, 0, 5000);
                timer = t;
            }
        }
    }

    private void scanAndRunTasks() {
        var now = TimeUtils.currentTimeSeconds();
        List<Task> tasks;
        synchronized (schedulerLock) {
            var headMap = epochSecondsToTasks.headMap(now, true);
            tasks = headMap.values()
                    .stream()
                    .map(Map::values)
                    .flatMap(Collection::stream)
                    .toList();
            headMap.clear();
            tasks.forEach(task -> {
                tenantIdToTask.remove(task.tenantId);
                scheduleTask(task.action, task.tenantId, task.periodSecond, task.periodSecond);
            });
        }
        tasks.forEach(Task::run);
    }

    public void scheduleTask(Runnable action, String tenantId, Long delaySecond, Long periodSecond) {
        var task = new Task(tenantId, delaySecond, periodSecond, action);
        scheduleTask(task);
    }

    public void scheduleTask(Task task) {
        synchronized (schedulerLock) {
            cancelSchedule(task.tenantId);
            task.executionEpochSecond = TimeUtils.currentTimeSeconds() + task.delaySecond;
            var executionEpochSecond = task.executionEpochSecond;
            if (task.isExecuted() || executionEpochSecond < 0) {
                log.info("given task is invalid: {}", task.tenantId);
                return;
            }
            tenantIdToTask.put(task.tenantId, task);
            epochSecondsToTasks.computeIfAbsent(executionEpochSecond, k -> new ConcurrentHashMap<>())
                    .put(task.tenantId, task);

            ensureTimer();
            log.info("schedule task: {} {}", task.tenantId, executionEpochSecond);
        }
    }

    public boolean isScheduled(String tenantId) {
        return tenantIdToTask.containsKey(tenantId);
    }

    public void cancelTask(String tenantId) {
        synchronized (schedulerLock) {
            cancelSchedule(tenantId);
            tenantIdToTask.remove(tenantId);
            log.info("cancel task: {}", tenantId);
        }
    }

    public void stop(String tenantId) {
        synchronized (schedulerLock) {
            cancelSchedule(tenantId);
            tenantIdToTask.remove(tenantId);
            log.info("cancel task: {}", tenantId);
        }
    }

    private void cancelSchedule(String tenantId) {
        var existingTask = tenantIdToTask.get(tenantId);
        if (existingTask == null) {
            return;
        }
        var taskIdToTask = epochSecondsToTasks.get(existingTask.executionEpochSecond);
        if (taskIdToTask != null) {
            taskIdToTask.remove(tenantId);
        }
    }

    public void clear() {
        synchronized (schedulerLock) {
            tenantIdToTask.clear();
            epochSecondsToTasks.clear();
            timer.cancel();
            timer = null;
            log.info("cancel all tasks.");
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Task {

        private String tenantId;

        private long delaySecond;

        private long periodSecond;

        private long executionEpochSecond = -1;

        private boolean executed = false;

        private Runnable action;

        public Task(String tenantId, long delaySecond, long periodSecond, Runnable action) {
            this.tenantId = tenantId;
            this.delaySecond = delaySecond;
            this.periodSecond = periodSecond;
            this.action = action;
        }

        public void run() {
            this.action.run();
        }

    }

}
