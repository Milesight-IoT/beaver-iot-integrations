package com.milesight.beaveriot.weather.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
public class WeatherResponse {
    private String code;
    private WeatherInfo now;
}
