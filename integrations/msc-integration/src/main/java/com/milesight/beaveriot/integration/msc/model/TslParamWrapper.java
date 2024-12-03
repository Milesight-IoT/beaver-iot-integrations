package com.milesight.beaveriot.integration.msc.model;

import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.integration.msc.util.MscTslUtils;
import com.milesight.cloud.sdk.client.model.TslDataSpec;
import com.milesight.cloud.sdk.client.model.TslParamSpec;
import lombok.*;

import java.util.List;

public record TslParamWrapper(TslParamSpec spec, TslItemWrapper functionItem) implements TslItemWrapper {

    @Override
    public EntityType getEntityType() {
        return functionItem.getEntityType();
    }

    @Override
    public EntityValueType getValueType() {
        if (spec.getDataSpec() == null) {
            throw new NullPointerException("Data spec is null");
        }
        return MscTslUtils.convertDataTypeToEntityValueType(spec.getDataSpec().getDataType());
    }

    @Override
    public String getParentId() {
        if (spec.getDataSpec() != null && spec.getDataSpec().getParentId() != null) {
            return spec.getDataSpec().getParentId();
        }
        return functionItem().getId();
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
        return spec.getDataSpec();
    }

    @Override
    public AccessMod getAccessMode() {
        return null;
    }

    @Override
    public List<TslParamWrapper> getOutputs() {
        return null;
    }

}
