package com.milesight.beaveriot.integration.aws.controller;

import com.milesight.beaveriot.base.response.ResponseBody;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import com.milesight.beaveriot.integration.aws.model.parser.PluginResponseData;
import com.milesight.beaveriot.integration.aws.model.parser.SearchPluginRequest;
import com.milesight.beaveriot.integration.aws.model.parser.BatchDeletePluginRequest;
import com.milesight.beaveriot.integration.aws.model.parser.PluginRequest;
import com.milesight.beaveriot.integration.aws.service.PluginService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 插件控制器
 */
@Slf4j
@RestController
@RequestMapping("/plugin")
public class PluginController {

    @Autowired
    private PluginService pluginService;

    @PostMapping
    public ResponseBody<Object> createPlugin(@RequestBody PluginRequest pluginRequest) {
        Boolean result = pluginService.createPlugin(pluginRequest);
        if (Boolean.FALSE.equals(result)) {
            return ResponseBuilder.fail("Failed to create product", "Failed to create product");
        }
        return ResponseBuilder.success();
    }


    @PostMapping("/search")
    public ResponseBody<Page<PluginResponseData>> search(@RequestBody SearchPluginRequest searchPluginRequest) {
        return ResponseBuilder.success(pluginService.searchPlugin(searchPluginRequest));
    }

    @PostMapping("/batch-delete")
    public ResponseBody<Void> batchDelete(@RequestBody BatchDeletePluginRequest batchDeletePluginRequest) {
        pluginService.batchDeletePlugins(batchDeletePluginRequest.getPluginIdList());
        return ResponseBuilder.success();
    }

}
