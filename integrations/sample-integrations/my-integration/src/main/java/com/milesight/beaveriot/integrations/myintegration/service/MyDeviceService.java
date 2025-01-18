package com.milesight.beaveriot.integrations.myintegration.service;

import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.integration.model.*;
import com.milesight.beaveriot.context.integration.model.event.ExchangeEvent;
import com.milesight.beaveriot.context.integration.wrapper.AnnotatedEntityWrapper;
import com.milesight.beaveriot.context.integration.wrapper.AnnotatedTemplateEntityWrapper;
import com.milesight.beaveriot.eventbus.annotations.EventSubscribe;
import com.milesight.beaveriot.eventbus.api.Event;
import com.milesight.beaveriot.integrations.myintegration.entity.MyDeviceEntities;
import com.milesight.beaveriot.integrations.myintegration.entity.MyIntegrationEntities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class MyDeviceService {
    @Autowired
    private DeviceServiceProvider deviceServiceProvider;

    @Autowired
    private EntityValueServiceProvider entityValueServiceProvider;

    public static final String INTEGRATION_ID = "demo";

    @EventSubscribe(payloadKeyExpression = INTEGRATION_ID + ".integration.add_device.*", eventType = ExchangeEvent.EventType.CALL_SERVICE)
    public void onAddDevice(Event<MyIntegrationEntities.AddDevice> event) {
        MyIntegrationEntities.AddDevice addDevice = event.getPayload();
        String deviceName = addDevice.getAddDeviceName();
        String ip = event.getPayload().getIp();
        String identifier = ip.replace(".", "_");
        Device device = new DeviceBuilder(INTEGRATION_ID)
                .name(deviceName)
                .identifier(identifier)
                .additional(Map.of("ip", ip))
                .entities(()-> new AnnotatedTemplateEntityBuilder(INTEGRATION_ID, identifier).build(MyDeviceEntities.class))
                .build();

        deviceServiceProvider.save(device);
    }

    @EventSubscribe(payloadKeyExpression = INTEGRATION_ID + ".integration.delete_device", eventType = ExchangeEvent.EventType.CALL_SERVICE)
    public void onDeleteDevice(Event<MyIntegrationEntities.DeleteDevice> event) {
        Device device = event.getPayload().getDeletedDevice();
        deviceServiceProvider.deleteById(device.getId());
    }

    @EventSubscribe(payloadKeyExpression = INTEGRATION_ID + ".integration.benchmark", eventType = ExchangeEvent.EventType.CALL_SERVICE)
    // highlight-next-line
    public void doBenchmark(Event<MyIntegrationEntities> event) {
        // mark benchmark starting
        new AnnotatedEntityWrapper<MyIntegrationEntities>()
                .saveValue(MyIntegrationEntities::getDetectStatus, (long) MyIntegrationEntities.DetectStatus.DETECTING.ordinal())
                .publishSync();

        // start pinging
        final int timeout = 5000;
        List<Device> devices = deviceServiceProvider.findAll(INTEGRATION_ID);
        AtomicReference<Long> activeCount = new AtomicReference<>(0L);
        AtomicReference<Long> inactiveCount = new AtomicReference<>(0L);
        Long startTimestamp = System.currentTimeMillis();
        devices.forEach(device -> {
            boolean isSuccess = false;
            try {
                String ip = (String) device.getAdditional().get("ip");
                InetAddress inet = InetAddress.getByName(ip);
                if (inet.isReachable(timeout)) {
                    isSuccess = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            int deviceStatus = MyDeviceEntities.DeviceStatus.OFFLINE.ordinal();
            if (isSuccess) {
                activeCount.updateAndGet(v -> v + 1);
                deviceStatus = MyDeviceEntities.DeviceStatus.ONLINE.ordinal();
            } else {
                inactiveCount.updateAndGet(v -> v + 1);
            }

            // Device have only one entity
            new AnnotatedTemplateEntityWrapper<MyDeviceEntities>(device.getIdentifier())
                    .saveValue(MyDeviceEntities::getStatus, (long) deviceStatus)
                    .publishSync();
        });
        Long endTimestamp = System.currentTimeMillis();

        // mark benchmark done
        AnnotatedEntityWrapper<MyIntegrationEntities> wrapper = new AnnotatedEntityWrapper<>();
        wrapper
                .saveValue(MyIntegrationEntities::getDetectStatus, (long) MyIntegrationEntities.DetectStatus.STANDBY.ordinal())
                .publishSync();

        // send report event
        new AnnotatedEntityWrapper<MyIntegrationEntities.DetectReport>().saveValues(Map.of(
                MyIntegrationEntities.DetectReport::getConsumedTime, endTimestamp - startTimestamp,
                MyIntegrationEntities.DetectReport::getOnlineCount, activeCount.get(),
                MyIntegrationEntities.DetectReport::getOfflineCount, inactiveCount.get()
        )).publishSync();
    }

    @EventSubscribe(payloadKeyExpression = INTEGRATION_ID + ".integration.detect_report.*", eventType = ExchangeEvent.EventType.REPORT_EVENT)
    public void listenDetectReport(Event<MyIntegrationEntities.DetectReport> event) {
        System.out.println("[Get-Report] " + event.getPayload()); // do something with this report
    }
}
