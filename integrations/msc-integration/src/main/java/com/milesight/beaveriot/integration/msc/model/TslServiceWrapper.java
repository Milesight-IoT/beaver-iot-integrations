package com.milesight.beaveriot.integration.msc.model;

import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.cloud.sdk.client.model.TslDataSpec;
import com.milesight.cloud.sdk.client.model.TslServiceSpec;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public record TslServiceWrapper(TslServiceSpec spec) implements TslItemWrapper {

    @Override
    public EntityType getEntityType() {
        return EntityType.SERVICE;
    }

    @Override
    public EntityValueType getValueType() {
        return EntityValueType.OBJECT;
    }

    @Override
    public String getParentId() {
        return null;
    }

    @Override
    public String getId() {
        return spec.getId();
    }

    @Override
    public String getName() {
        return spec.getName();
    }

    @Override
    public TslDataSpec getDataSpec() {
        return null;
    }

    @Override
    public AccessMod getAccessMode() {
        return null;
    }

    @Override
    public List<TslParamWrapper> getOutputs() {
        if (spec.getOutputs() == null) {
            return List.of();
        }
        return spec.getOutputs()
                .stream()
                .filter(spec -> spec.getDataSpec() != null)
                .map(spec -> new TslParamWrapper(spec, this))
                .toList();
    }

}
