package com.milesight.beaveriot.vms;

import com.milesight.beaveriot.context.integration.bootstrap.IntegrationBootstrap;
import com.milesight.beaveriot.context.integration.model.Integration;
import com.milesight.beaveriot.vms.api.HuggingfaceApi;
import com.milesight.beaveriot.vms.api.VmsApi;
import com.milesight.beaveriot.vms.service.VmsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @Author yuanh
 * @Description
 * @Package com.milesight.beaveriot
 * @Date 2024/11/19 13:58
 */
@Component
public class VmsIntegrationBootstrap implements IntegrationBootstrap {
    @Autowired
    private VmsService vmsService;

    @Override
    public void onPrepared(Integration integration) {
    }

    @Override
    public void onStarted(Integration integration) {
        VmsApi.init();
        HuggingfaceApi.init();
        vmsService.syncDeviceList();
    }

    @Override
    public void onDestroy(Integration integration) {

    }
}
