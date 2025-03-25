package com.milesight.beaveriot.integrations.milesightgateway.model;

import com.milesight.beaveriot.integrations.milesightgateway.codec.model.DeviceDef;
import com.milesight.beaveriot.integrations.milesightgateway.codec.model.DeviceResourceInfo;
import lombok.Data;

/**
 * DeviceCodecData class.
 *
 * @author simon
 * @date 2025/3/17
 */
@Data
public class DeviceCodecData {
    DeviceResourceInfo resourceInfo;

    DeviceDef def;

    String decoderStr;

    String encoderStr;
}
