package com.milesight.beaveriot.integrations.milesightgateway.codec.model;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import lombok.Data;

import java.util.List;

/**
 * DeviceDefObject class.
 *
 * @author simon
 * @date 2025/2/17
 */
@Data
public class DeviceDefObject {
    private String id;

    private String name;

    // Sample value
    // private String value;

    private String unit;

    private ACCESS_MODE accessMode;

    private ObjectDataType dataType; // BOOL / TEXT / ENUM / NUMBER

    private ObjectValueType valueType; // STRING / UINT8 / UINT16 / FLOAT

    private List<DeviceDefObjectEnum> values;

    private Integer maxLength;

    // Bacnet legacy fields
    // private String bacnetType;
    // private Integer bacnetUnitTypeId;
    // private String bacnetUnitType;

    // private List<String> reference;

    public enum ACCESS_MODE {
        R, RW, W,

        @JsonEnumDefaultValue
        NONE
    }
}
