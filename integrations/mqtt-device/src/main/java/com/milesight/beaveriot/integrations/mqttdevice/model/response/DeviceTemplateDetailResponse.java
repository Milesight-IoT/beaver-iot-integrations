package com.milesight.beaveriot.integrations.mqttdevice.model.response;

import com.milesight.beaveriot.context.model.DeviceTemplateModel;
import com.milesight.beaveriot.context.model.response.DeviceTemplateResponseData;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * author: Luxb
 * create: 2025/6/16 8:49
 **/
@EqualsAndHashCode(callSuper = true)
@Setter
@Getter
public class DeviceTemplateDetailResponse extends DeviceTemplateInfoResponse {
    private DeviceTemplateModel.Definition.Input inputSchema;
    private DeviceTemplateModel.Definition.Output outputSchema;

    protected DeviceTemplateDetailResponse(DeviceTemplateResponseData deviceTemplateResponseData, String topic,
                                           DeviceTemplateModel.Definition.Input inputSchema,
                                           DeviceTemplateModel.Definition.Output outputSchema) {
        super(deviceTemplateResponseData, topic);
        this.inputSchema = inputSchema;
        this.outputSchema = outputSchema;
    }

    public static DeviceTemplateDetailResponse build(DeviceTemplateResponseData deviceTemplateResponseData, String topic,
                                                     DeviceTemplateModel.Definition.Input inputSchema,
                                                     DeviceTemplateModel.Definition.Output outputSchema) {
        return new DeviceTemplateDetailResponse(deviceTemplateResponseData, topic, inputSchema, outputSchema);
    }
}
