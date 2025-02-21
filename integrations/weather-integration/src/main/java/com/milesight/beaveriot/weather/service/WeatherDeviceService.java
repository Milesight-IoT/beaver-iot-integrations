package com.milesight.beaveriot.weather.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.api.ExchangeFlowExecutor;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.integration.model.event.ExchangeEvent;
import com.milesight.beaveriot.eventbus.annotations.EventSubscribe;
import com.milesight.beaveriot.eventbus.api.EventResponse;
import com.milesight.beaveriot.weather.api.WeatherApi;
import com.milesight.beaveriot.weather.constants.WeatherConstants;
import com.milesight.beaveriot.weather.model.DailyWeatherListResponse;
import com.milesight.beaveriot.weather.model.WeatherInfo;
import com.milesight.beaveriot.weather.model.WeatherSimpleInfo;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class WeatherDeviceService {
    @Autowired
    private DeviceServiceProvider deviceServiceProvider;
    @Autowired
    private ExchangeFlowExecutor exchangeFlowExecutor;
    @Autowired
    private EntityValueServiceProvider entityValueServiceProvider;

    @SneakyThrows
    @EventSubscribe(payloadKeyExpression = WeatherConstants.INTEGRATION_ID + ".device.*.get_weather", eventType = ExchangeEvent.EventType.DOWN)
    public void getWeather(ExchangeEvent event) {
        val exchangePayload = event.getPayload();
        val devices = exchangePayload.getExchangeEntities()
                .values()
                .stream()
                .map(Entity::getDeviceKey)
                .distinct()
                .map(deviceServiceProvider::findByKey)
                .filter(Objects::nonNull)
                .toList();
        if (devices.size() != 1) {
            log.warn("Invalid device number: {}", devices.size());
            return;
        }
        val device = devices.get(0);
        getWeatherServicePayload(device, exchangePayload);
    }

    @SneakyThrows
    @EventSubscribe(payloadKeyExpression = WeatherConstants.INTEGRATION_ID + ".device.*.get_weather24h", eventType = ExchangeEvent.EventType.DOWN)
    public EventResponse get_weather24h(ExchangeEvent event) {
        val exchangePayload = event.getPayload();
        val devices = exchangePayload.getExchangeEntities()
                .values()
                .stream()
                .map(Entity::getDeviceKey)
                .distinct()
                .map(deviceServiceProvider::findByKey)
                .filter(Objects::nonNull)
                .toList();
        if (devices.size() != 1) {
            log.warn("Invalid device number: {}", devices.size());
            return new EventResponse();
        }
        val device = devices.get(0);
        return EventResponse.of("weather24h", getWeather24hServicePayload(device, exchangePayload));
    }

    @SneakyThrows
    @EventSubscribe(payloadKeyExpression = WeatherConstants.INTEGRATION_ID + ".device.*.get_weather7d", eventType = ExchangeEvent.EventType.DOWN)
    public EventResponse get_weather7d(ExchangeEvent event) {
        val exchangePayload = event.getPayload();
        val devices = exchangePayload.getExchangeEntities()
                .values()
                .stream()
                .map(Entity::getDeviceKey)
                .distinct()
                .map(deviceServiceProvider::findByKey)
                .filter(Objects::nonNull)
                .toList();
        if (devices.size() != 1) {
            log.warn("Invalid device number: {}", devices.size());
            return new EventResponse();
        }
        val device = devices.get(0);
        return EventResponse.of("weather7d", getWeather7dServicePayload(device, exchangePayload));
    }

    public void getDeviceWeather(Device device) {
        String city = (String) device.getAdditional().get("city");
        WeatherInfo nowWeather = WeatherApi.getNowWeather(city);
        ExchangePayload weatherPayload = new ExchangePayload();
        device.getEntities().forEach(entity -> {
            if (entity.getIdentifier().equals("temperature")) {
                weatherPayload.put(entity.getKey(), nowWeather.getTemp());
            }
            if (entity.getIdentifier().equals("precip")) {
                weatherPayload.put(entity.getKey(), nowWeather.getPrecip());
            }
            if (entity.getIdentifier().equals("humidity")) {
                weatherPayload.put(entity.getKey(), nowWeather.getHumidity());
            }
        });
        exchangeFlowExecutor.asyncExchangeDown(weatherPayload);
    }

    public void refreshWeather() {
        List<Device> devices = Optional.ofNullable(deviceServiceProvider.findAll(WeatherConstants.INTEGRATION_ID)).orElse(Collections.emptyList());
        if (devices.isEmpty()) {
            return;
        }
        devices.forEach(device -> {
            List<Entity> entities = device.getEntities();
            for (Entity entity : entities) {
                if (entity.getIdentifier().equals("auto_refresh")) {
                    JsonNode jsonNode = entityValueServiceProvider.findValueByKey(entity.getKey());
                    if (jsonNode != null && jsonNode.asBoolean() && Boolean.TRUE.equals(jsonNode.booleanValue())) {
                        getDeviceWeather(device);
                    }
                }
            }
        });
    }

    private void getWeatherServicePayload(Device device, ExchangePayload exchangePayload) {
        val servicePayload = exchangePayload.getPayloadsByEntityType(EntityType.SERVICE);
        if (servicePayload.isEmpty()) {
            return;
        }
        getDeviceWeather(device);
    }

    private List<WeatherSimpleInfo> getWeather24hServicePayload(Device device, ExchangePayload exchangePayload) {
        val servicePayload = exchangePayload.getPayloadsByEntityType(EntityType.SERVICE);
        if (servicePayload.isEmpty()) {
            return Collections.emptyList();
        }
        String city = (String) device.getAdditional().get("city");
        return WeatherApi.get24HWeather(city);
    }

    private List<DailyWeatherListResponse.DailyWeatherInfo> getWeather7dServicePayload(Device device, ExchangePayload exchangePayload) {
        val servicePayload = exchangePayload.getPayloadsByEntityType(EntityType.SERVICE);
        if (servicePayload.isEmpty()) {
            return Collections.emptyList();
        }
        String city = (String) device.getAdditional().get("city");
        return WeatherApi.get7DWeather(city);
    }
}
