package com.milesight.beaveriot.integration.msc.model;

import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.integration.msc.util.MscTslUtils;
import com.milesight.cloud.sdk.client.model.TslDataSpec;
import com.milesight.cloud.sdk.client.model.TslPropertySpec;
import lombok.*;

import java.util.List;

public record TslPropertyWrapper(TslPropertySpec spec) implements TslItemWrapper {

    @Override
    public EntityType getEntityType() {
        return EntityType.PROPERTY;
    }

    @Override
    public EntityValueType getValueType() {
        return MscTslUtils.convertDataTypeToEntityValueType(spec.getDataSpec().getDataType());
    }

    @Override
    public String getParentId() {
        return spec.getDataSpec().getParentId();
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
        return AccessMod.valueOf(spec.getAccessMode().name());
    }

    @Override
    public List<TslParamWrapper> getParams() {
        return null;
    }

}
