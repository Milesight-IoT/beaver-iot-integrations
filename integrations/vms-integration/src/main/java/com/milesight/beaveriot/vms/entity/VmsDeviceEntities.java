package com.milesight.beaveriot.vms.entity;

import com.milesight.beaveriot.common.entity.EntityField;
import com.milesight.beaveriot.common.util.EntityUtils;
import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.vms.constants.VmsConstants;
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
public class VmsDeviceEntities {
    @EntityField(identifier = VmsConstants.DeviceEntity.ONLINE, property = VmsConstants.DeviceEntity.ONLINE, accessMod = AccessMod.R, valueType = EntityValueType.BOOLEAN)
    private Entity onlineEntity;

    @EntityField(identifier = VmsConstants.DeviceEntity.GET_HLS_URL, property = VmsConstants.DeviceEntity.GET_HLS_URL, valueType = EntityValueType.BOOLEAN, isService = true)
    private Entity hlsUrlEntity;

    @EntityField(identifier = VmsConstants.DeviceEntity.SYNC, property = VmsConstants.DeviceEntity.SYNC, valueType = EntityValueType.BOOLEAN, isService = true)
    private Entity syncEntity;

    public static String key(String fieldName) {
        return EntityUtils.key(VmsDeviceEntities.class, fieldName);
    }
}
