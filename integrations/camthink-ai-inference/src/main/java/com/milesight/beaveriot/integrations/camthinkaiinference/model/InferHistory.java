package com.milesight.beaveriot.integrations.camthinkaiinference.model;

import com.milesight.beaveriot.integrations.camthinkaiinference.enums.InferStatus;
import lombok.Data;

/**
 * author: Luxb
 * create: 2025/6/20 14:26
 **/
@Data
public class InferHistory {
    protected String modelName;
    protected String originImage;
    protected String resultImage;
    protected String inferOutputsData;
    protected String inferStatus;
    protected Long uplinkAt;
    protected Long inferAt;

    public InferHistory() {
        resultImage = "";
        inferOutputsData = "";
        inferStatus = InferStatus.UNKNOWN.getValue();
    }
}
