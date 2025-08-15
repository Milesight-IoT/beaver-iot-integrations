package com.milesight.beaveriot.integrations.camthinkaiinference.model.response;

import com.milesight.beaveriot.integrations.camthinkaiinference.model.InferHistory;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * author: Luxb
 * create: 2025/6/20 13:43
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class BoundDeviceData extends InferHistory {
    private String deviceId;
    private String deviceName;
    private String currentModelName;
    private Long createAt;
    private String inferHistoryEntityId;
    private String inferHistoryEntityKey;

    public void fillInferHistory(InferHistory inferHistory) {
        this.modelName = inferHistory.getModelName();
        this.originImage = inferHistory.getOriginImage();
        this.resultImage = inferHistory.getResultImage();
        this.inferOutputsData = inferHistory.getInferOutputsData();
        this.inferStatus = inferHistory.getInferStatus();
        this.uplinkAt = inferHistory.getUplinkAt();
        this.inferAt = inferHistory.getInferAt();
    }
}