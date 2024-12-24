package com.milesight.beaveriot.weather.api;

import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.milesight.beaveriot.common.util.MilesightHttpUtil;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.support.SpringContext;
import com.milesight.beaveriot.weather.constants.WeatherConstants;
import com.milesight.beaveriot.weather.entity.WeatherIntegrationEntities;
import com.milesight.beaveriot.weather.model.*;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * @Author yuanh
 * @Description
 * @Package com.milesight.beaveriot.weather.api
 * @Date 2024/11/20 9:15
 */
@Slf4j
@UtilityClass
public class WeatherApi {
    private static String KEY = "";
    private static String URL = "https://devapi.qweather.com";
    private static String LANG = "cn";

    public static void init() {
        EntityValueServiceProvider entityValueServiceProvider = SpringContext.getBean(EntityValueServiceProvider.class);
        String weatherInfoId = WeatherConstants.INTEGRATION_ID + ".integration.api_info.";
        String urlId = weatherInfoId + "api_url";
        String keyId = weatherInfoId + "api_key";
        String langId = weatherInfoId + "api_lang";
        Map<String, JsonNode> jsonNodeMap = entityValueServiceProvider.findValuesByKeys(List.of(urlId, keyId, langId));
        jsonNodeMap.forEach((key, value) -> {
            if (value == null || value.isNull()) {
                return;
            }
            if (key.equals(urlId)) {
                URL = value.textValue();
            }
            if (key.equals(keyId)) {
                KEY = value.textValue();
            }
            if (key.equals(langId)) {
                LANG = value.textValue();
            }
        });
    }

    public static synchronized void setAppInfo(WeatherIntegrationEntities.ApiInfo appInfo) {
        KEY = appInfo.getApiKey();
        URL = appInfo.getApiUrl();
        LANG = appInfo.getApiLang();
    }

    private static String getNowWeatherUrl(String city) {
        return URL + "/v7/weather/now?location=" + city + "&key=" + KEY + "&lang=" + LANG;
    }

    private static String get24HWeatherUrl(String city) {
        return URL + "/v7/weather/24h?location=" + city + "&key=" + KEY + "&lang=" + LANG;
    }

    private static String get7DWeatherUrl(String city) {
        return URL + "/v7/weather/7d?location=" + city + "&key=" + KEY + "&lang=" + LANG;
    }

    public static WeatherInfo getNowWeather(String city) {
        try {
            String url = getNowWeatherUrl(city);
            String body = MilesightHttpUtil.get(url, 5000, null, null);
            WeatherResponse response = JSONUtil.toBean(body, WeatherResponse.class);
            if (!response.getCode().equals("200")) {
                log.debug("Failed to retrieve weather information, body: {}", body);
                throw new IllegalArgumentException("Failed to retrieve weather information");
            }
            return response.getNow();
        } catch (Exception e) {
            log.error("Failed to retrieve weather information", e);
            throw e;
        }
    }

    public static List<WeatherSimpleInfo> get24HWeather(String city) {
        try {
            String url = get24HWeatherUrl(city);
            String body = MilesightHttpUtil.get(url, 5000, null, null);
            WeatherListResponse response = JSONUtil.toBean(body, WeatherListResponse.class);
            if (!response.getCode().equals("200")) {
                log.debug("Failed to retrieve weather information, body: {}", body);
                throw new IllegalArgumentException("Failed to retrieve weather information");
            }
            return initWeatherList(response.getHourly());
        } catch (Exception e) {
            log.error("Failed to retrieve weather information", e);
            throw e;
        }
    }

    public static List<DailyWeatherListResponse.DailyWeatherInfo> get7DWeather(String city) {
        try {
            String url = get7DWeatherUrl(city);
            String body = MilesightHttpUtil.get(url, 5000, null, null);
            DailyWeatherListResponse response = JSONUtil.toBean(body, DailyWeatherListResponse.class);
            if (!response.getCode().equals("200")) {
                log.debug("Failed to retrieve weather information, body: {}", body);
                throw new IllegalArgumentException("Failed to retrieve weather information");
            }
            return response.getDaily();
        } catch (Exception e) {
            log.error("Failed to retrieve weather information", e);
            throw e;
        }
    }

    private static List<WeatherSimpleInfo> initWeatherList(List<WeatherSimpleInfo> weatherSimpleInfoList) {
        for (WeatherSimpleInfo weatherSimpleInfo : weatherSimpleInfoList) {
            OffsetDateTime dateTime = OffsetDateTime.parse(weatherSimpleInfo.getFxTime(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            String hour = String.format("%02d", dateTime.getHour());
            String minute = String.format("%02d", dateTime.getMinute());
            weatherSimpleInfo.setFxTime(hour + ":" + minute);
        }
        return weatherSimpleInfoList;
    }
}
