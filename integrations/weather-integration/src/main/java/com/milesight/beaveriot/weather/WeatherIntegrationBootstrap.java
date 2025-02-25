package com.milesight.beaveriot.weather;

import com.milesight.beaveriot.context.integration.bootstrap.IntegrationBootstrap;
import com.milesight.beaveriot.context.integration.model.Integration;
import com.milesight.beaveriot.weather.api.WeatherApi;
import com.milesight.beaveriot.weather.service.WeatherDeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @Author yuanh
 * @Description
 * @Package com.milesight.beaveriot
 * @Date 2024/11/19 13:58
 */
@Component
public class WeatherIntegrationBootstrap implements IntegrationBootstrap {
    @Autowired
    private WeatherDeviceService weatherDeviceService;

    @Override
    public void onPrepared(Integration integration) {
    }

    @Override
    public void onStarted(Integration integration) {
        WeatherApi.init();
    }

    @Override
    public void onDestroy(Integration integration) {

    }
}
