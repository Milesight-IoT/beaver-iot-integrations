package com.milesight.beaveriot.integrations.milesightgateway.service;

import com.milesight.beaveriot.context.api.BlueprintLibraryResourceResolverProvider;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.integration.model.AttributeBuilder;
import com.milesight.beaveriot.context.integration.model.BlueprintDeviceModel;
import com.milesight.beaveriot.context.integration.model.BlueprintDeviceVendor;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.integrations.milesightgateway.entity.MsGwIntegrationEntities;
import com.milesight.beaveriot.integrations.milesightgateway.model.DeviceModelIdentifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * DeviceModelService class.
 *
 * @author simon
 * @date 2025/2/25
 */
@Component("milesightGatewayDeviceModelService")
@Slf4j
public class DeviceModelService {
    @Autowired
    EntityServiceProvider entityServiceProvider;

    @Autowired
    BlueprintLibraryResourceResolverProvider blueprintLibraryResourceResolverProvider;

    private void iterateDeviceModels(BiConsumer<BlueprintDeviceVendor, BlueprintDeviceModel> consumer) {
        List<BlueprintDeviceVendor> vendors = blueprintLibraryResourceResolverProvider.getDeviceVendors();
        vendors.forEach(vendor -> {
            List<BlueprintDeviceModel> models = blueprintLibraryResourceResolverProvider.getDeviceModels(vendor.getId());
            models.forEach(model -> consumer.accept(vendor, model));
        });
    }

    public Map<String, String> getDeviceModelNameToId() {
        final Map<String, String> modelIdToName = new LinkedHashMap<>();
        iterateDeviceModels((vendor, model) -> modelIdToName.put(model.getName(), new DeviceModelIdentifier(vendor.getId(), model.getId()).toString()));
        return modelIdToName;
    }

    public void syncDeviceModelListToAdd() {
        Entity deviceModelNameEntity = entityServiceProvider.findByKey(MsGwIntegrationEntities.ADD_DEVICE_GATEWAY_DEVICE_MODEL_KEY);
        Map<String, Object> attributes = deviceModelNameEntity.getAttributes();
        if (attributes == null) {
            attributes = new HashMap<>();
        }

        final Map<String, String> modelIdToName = new LinkedHashMap<>();
        iterateDeviceModels((vendor, model) -> modelIdToName.put(
                    new DeviceModelIdentifier(vendor.getId(), model.getId()).toString(),
                    model.getName() + " (" + vendor.getName() + ")"
                )
        );

        attributes.put(AttributeBuilder.ATTRIBUTE_ENUM, modelIdToName);
        deviceModelNameEntity.setAttributes(attributes);
        entityServiceProvider.save(deviceModelNameEntity);
    }
}
