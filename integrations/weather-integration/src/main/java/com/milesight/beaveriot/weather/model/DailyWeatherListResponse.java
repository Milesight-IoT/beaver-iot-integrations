package com.milesight.beaveriot.weather.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @Author yuanh
 * @Description
 * @Package com.milesight.beaveriot.model
 * @Date 2024/11/20 9:28
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DailyWeatherListResponse {
    private String code;
    private List<DailyWeatherInfo> daily;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DailyWeatherInfo {
        private String fxDate;
        private String sunrise;
        private String sunset;
        private String moonrise;
        private String moonset;
        private String moonPhase;
        private String moonPhaseIcon;
        private String tempMax;
        private String tempMin;
        private String iconDay;
        private String textDay;
        private String iconNight;
        private String textNight;
        private String wind360Day;
        private String windDirDay;
        private String windScaleDay;
        private String windSpeedDay;
        private String wind360Night;
        private String windDirNight;
        private String windScaleNight;
        private String windSpeedNight;
        private String humidity;
        private String precip;
        private String pressure;
        private String vis;
        private String cloud;
        private String uvIndex;
     }
}
