package com.milesight.beaveriot.vms.enums;

import com.milesight.beaveriot.base.enums.EnumCode;

/**
 * @Author yuanh
 * @Description
 * @Package com.milesight.beaveriot.vms.enums
 * @Date 2024/11/29 11:47
 */
public enum ObjectDetectionModel implements EnumCode {
    DETR_RESNET_50("DETR_RESNET_50","facebook/detr-resnet-50"),
    DETR_RESNET_50_DC5("DETR_RESNET_50_DC5","facebook/detr-resnet-50-dc5"),
    DETR_RESNET_101("DETR_RESNET_101","facebook/detr-resnet-101"),
    YOLOS_SMALL("YOLOS_SMALL","hustvl/yolos-small"),
    YOLOS_TINY("YOLOS_TINY","hustvl/yolos-tiny"),

        ;
    private String code;
    private String value;

    ObjectDetectionModel(String code, String value) {
        this.code = code;
        this.value = value;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getValue() {
        return value;
    }

    public static String getUrl(String code){
        try{
            ObjectDetectionModel objectDetectionModel = ObjectDetectionModel.valueOf(code);
            return objectDetectionModel.getValue();
        }catch (Exception e){
            return ObjectDetectionModel.DETR_RESNET_50.getValue();
        }
    }
}
