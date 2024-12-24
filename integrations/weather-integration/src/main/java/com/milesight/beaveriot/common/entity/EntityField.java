package com.milesight.beaveriot.common.entity;

import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Author yuanh
 * @Description
 * @Package com.milesight.beaveriot.weather.entity
 * @Date 2024/11/25 16:04
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface EntityField {
    String identifier();

    String property() default "";

    AccessMod accessMod() default AccessMod.R;

    EntityValueType valueType();

    String unit() default "";

    boolean isService() default false;
}
