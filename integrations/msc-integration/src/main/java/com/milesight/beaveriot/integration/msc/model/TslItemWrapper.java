package com.milesight.beaveriot.integration.msc.model;


import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.cloud.sdk.client.model.TslDataSpec;
import lombok.*;

import java.util.List;

public interface TslItemWrapper {

    EntityType getEntityType();

    EntityValueType getValueType();

    String getParentId();

    String getId();

    String getName();

    TslDataSpec getDataSpec();

    AccessMod getAccessMode();

    List<TslParamWrapper> getOutputs();

}
