package com.milesight.beaveriot.vms.controller;

import com.milesight.beaveriot.base.response.ResponseBody;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import com.milesight.beaveriot.vms.constants.VmsConstants;
import com.milesight.beaveriot.vms.model.ImageObjectDetectionRequest;
import com.milesight.beaveriot.vms.service.VmsImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author yuanh
 * @Description
 * @Package com.milesight.beaveriot.vms.controller
 * @Date 2024/11/26 17:51
 */
@RestController
@RequestMapping("/" + VmsConstants.INTEGRATION_ID)
public class VmsController {

    @Autowired
    private VmsImageService vmsImageService;

    /**
     * 图片解析
     *
     * @return
     */
    @PostMapping("/object-detection")
    public ResponseBody<Object> objectDetection(@RequestBody ImageObjectDetectionRequest request) {
        try {
            return ResponseBuilder.success(vmsImageService.objectDetection(request));
        } catch (Exception e) {
            return ResponseBuilder.fail("500", e.getMessage());
        }
    }
}
