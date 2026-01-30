package com.milesight.beaveriot.integration.msc.service;

import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.api.EntityTemplateServiceProvider;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.DeviceStatus;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Slf4j
@Service
public class MscDeviceStatusService {

    private static final String IDENTIFIER_DEVICE_STATUS = "@status";

    @Autowired
    private EntityValueServiceProvider entityValueServiceProvider;

    @Autowired
    EntityServiceProvider entityServiceProvider;

    @Autowired
    EntityTemplateServiceProvider entityTemplateServiceProvider;

    /**
     * Update device status by directly saving the status value via EntityValueServiceProvider.
     * This method bypasses the deviceStatusServiceProvider.online/offline methods
     * and directly updates the device status entity.
     *
     * <p>Special handling:
     * <ul>
     *   <li>If deviceStatus is null and the device has never been seen (no existing value),
     *       the status remains unset to preserve the "never seen" state</li>
     *   <li>If deviceStatus is null but the device has been seen before,
     *       it will be marked as OFFLINE</li>
     *   <li>If the new status is the same as the existing status, no update is performed</li>
     * </ul>
     *
     * @param device       the device to update, must not be null
     * @param deviceStatus the new status (ONLINE/OFFLINE), can be null (see special handling above)
     */
    public void updateDeviceStatus(Device device, @Nullable DeviceStatus deviceStatus) {
        try {
            var statusEntityKey = device.getKey() + "." + IDENTIFIER_DEVICE_STATUS;
            var existValue = (String) entityValueServiceProvider.findValueByKey(statusEntityKey);

            if (deviceStatus == null) {
                if (existValue == null) {
                    // keep never seen
                    return;
                }
                deviceStatus = DeviceStatus.OFFLINE;
            }

            if (Objects.equals(deviceStatus.name(), existValue)) {
                return;
            }

            // ensure entity exists
            if (device.getEntities().stream()
                    .noneMatch(entity -> IDENTIFIER_DEVICE_STATUS.equals(entity.getIdentifier()))) {

                var entityTemplate = entityTemplateServiceProvider.findByKey(IDENTIFIER_DEVICE_STATUS);
                if (entityTemplate == null) {
                    log.warn("Device status entity template not found: device={}", device.getKey());
                    return;
                }

                var statusEntity = entityTemplate.toEntity(device.getIntegrationId(), device.getKey());
                entityServiceProvider.save(statusEntity);
            }

            var payload = ExchangePayload.create(statusEntityKey, deviceStatus.name());
            entityValueServiceProvider.saveValuesAndPublishSync(payload);
            log.debug("Device status updated: device={}, status={}", device.getKey(), deviceStatus);
        } catch (Exception e) {
            log.error("Failed to update device status: device={}, status={}", device.getKey(), deviceStatus, e);
        }
    }
}
