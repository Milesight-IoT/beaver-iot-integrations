package com.milesight.beaveriot.common.util;

import com.milesight.beaveriot.common.entity.EntityField;
import lombok.experimental.UtilityClass;

import java.lang.reflect.Field;

/**
 * @Author yuanh
 * @Description
 * @Package com.milesight.beaveriot.common.util
 * @Date 2024/11/25 17:47
 */
@UtilityClass
public class EntityUtils {
    /**
     * 获取指定类的字段上标注的 @EntityField 注解的 identifier 值。
     *
     * @param clazz     目标类
     * @param fieldName 字段名称
     * @return identifier 值，如果字段不存在或没有 @EntityField 注解，则返回 null
     */
    public static String key(Class<?> clazz, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            if (field.isAnnotationPresent(EntityField.class)) {
                EntityField entityField = field.getAnnotation(EntityField.class);
                return entityField.identifier();
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();  // 或者使用日志记录
        }
        return null;
    }
}
