package com.milesight.beaveriot.integrations.milesightgateway.mqtt.model;

import com.milesight.beaveriot.context.integration.model.DeviceStatus;
import com.milesight.beaveriot.pubsub.api.message.RemoteBroadcastMessage;
import lombok.*;

/**
 * GatewayActiveMessage class.
 *
 * @author simon
 * @date 2025/11/13
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayActiveMessage extends RemoteBroadcastMessage {
    private String eui;
    private DeviceStatus status;
}
