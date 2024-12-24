package com.milesight.beaveriot.weather.service;

import com.milesight.beaveriot.common.entity.IntegrationDeviceBuilder;
import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.api.ExchangeFlowExecutor;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.integration.model.event.ExchangeEvent;
import com.milesight.beaveriot.eventbus.annotations.EventSubscribe;
import com.milesight.beaveriot.eventbus.api.Event;
import com.milesight.beaveriot.weather.api.WeatherApi;
import com.milesight.beaveriot.weather.constants.WeatherConstants;
import com.milesight.beaveriot.weather.entity.WeatherDeviceEntities;
import com.milesight.beaveriot.weather.entity.WeatherIntegrationEntities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class WeatherService {
    @Autowired
    private DeviceServiceProvider deviceServiceProvider;
    @Autowired
    private ExchangeFlowExecutor exchangeFlowExecutor;
    @Autowired
    private WeatherDeviceService weatherDeviceService;

    @EventSubscribe(payloadKeyExpression = WeatherConstants.INTEGRATION_ID + ".integration.add_device.*", eventType = ExchangeEvent.EventType.DOWN)
    public void onAddDevice(Event<WeatherIntegrationEntities.AddDevice> event) {
        String deviceName = event.getPayload().getContext("device_name", "Weather Device");
        String city = event.getPayload().getCity();
        Device device = new IntegrationDeviceBuilder(WeatherConstants.INTEGRATION_ID)
                .name(deviceName)
                .identifier(city)
                .additional(Map.of("city", city))
                .build(WeatherDeviceEntities.class);
        deviceServiceProvider.save(device);
    }

    @EventSubscribe(payloadKeyExpression = WeatherConstants.INTEGRATION_ID + ".integration.delete_device", eventType = ExchangeEvent.EventType.DOWN)
    public void onDeleteDevice(Event<ExchangePayload> event) {
        Device device = (Device) event.getPayload().getContext("device");
        deviceServiceProvider.deleteById(device.getId());
    }

    @EventSubscribe(payloadKeyExpression = WeatherConstants.INTEGRATION_ID + ".integration.benchmark", eventType = ExchangeEvent.EventType.DOWN)
    // highlight-next-line
    public void doBenchmark(Event<WeatherIntegrationEntities> event) {
        String weatherInfoKey = WeatherConstants.INTEGRATION_ID + ".integration.detect_status";
        try {
            doBenchmark(weatherInfoKey);
        } catch (Exception e) {
            log.error("[Benchmark Error] " + e);
            throw new RuntimeException(e);
        } finally {
            // mark benchmark done
            ExchangePayload donePayload = new ExchangePayload();
            donePayload.put(weatherInfoKey, WeatherIntegrationEntities.DetectStatus.STANDBY.ordinal());
            exchangeFlowExecutor.syncExchangeUp(donePayload);
        }
    }

    @EventSubscribe(payloadKeyExpression = WeatherConstants.INTEGRATION_ID + ".integration.api_info.*", eventType = ExchangeEvent.EventType.DOWN)
    public void setApiInfo(Event<WeatherIntegrationEntities.ApiInfo> event) {
        WeatherApi.setAppInfo(event.getPayload());
    }

    private void doBenchmark(String weatherInfoKey) {
        // mark benchmark starting
        exchangeFlowExecutor.syncExchangeDown(new ExchangePayload(Map.of(weatherInfoKey, WeatherIntegrationEntities.DetectStatus.DETECTING.ordinal())));
        // start pinging
        List<Device> devices = deviceServiceProvider.findAll(WeatherConstants.INTEGRATION_ID);
        devices.forEach(device -> weatherDeviceService.getDeviceWeather(device));
    }
}
