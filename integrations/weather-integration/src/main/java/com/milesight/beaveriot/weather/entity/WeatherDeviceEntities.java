package com.milesight.beaveriot.weather.entity;

import com.milesight.beaveriot.common.entity.EntityField;
import com.milesight.beaveriot.common.util.EntityUtils;
import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.context.integration.model.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

/**
 * @Author yuanh
 * @Description
 * @Package com.milesight.beaveriot.weather.entity
 * @Date 2024/11/25 17:32
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldNameConstants
public class WeatherDeviceEntities {
    @EntityField(identifier = "temperature", property = "temperature", accessMod = AccessMod.R, valueType = EntityValueType.LONG, unit = "°C")
    private Entity temperatureEntity;

    @EntityField(identifier = "precip", property = "precip", accessMod = AccessMod.R, valueType = EntityValueType.DOUBLE, unit = "mm/h")
    private Entity precipEntity;

    @EntityField(identifier = "humidity", property = "humidity", accessMod = AccessMod.R, valueType = EntityValueType.DOUBLE, unit = "%")
    private Entity humidityEntity;

    @EntityField(identifier = "auto_refresh", property = "auto_refresh", accessMod = AccessMod.RW, valueType = EntityValueType.BOOLEAN)
    private Entity autoRefreshEntity;

    @EntityField(identifier = "get_weather", isService = true, valueType = EntityValueType.BOOLEAN)
    private Entity getWeatherEntity;

    @EntityField(identifier = "get_weather24h", isService = true, valueType = EntityValueType.BOOLEAN)
    private Entity getWeather24hEntity;

    @EntityField(identifier = "get_weather7d", isService = true, valueType = EntityValueType.BOOLEAN)
    private Entity getWeather7dEntity;

    public static String key(String fieldName) {
        return EntityUtils.key(WeatherDeviceEntities.class, fieldName);
    }
}
