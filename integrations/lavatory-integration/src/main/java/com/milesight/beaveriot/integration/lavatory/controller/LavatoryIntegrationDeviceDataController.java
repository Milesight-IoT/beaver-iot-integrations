//package com.milesight.beaveriot.integration.lavatory.controller;
//
//import com.milesight.beaveriot.integration.lavatory.model.request.LavatoryRequest;
//import com.milesight.beaveriot.integration.lavatory.service.LavatoryDeviceService;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.List;
//
///**
// *
// */
//@Slf4j
//@RestController
//@RequestMapping("/public/integration/lavatory/device-data")
//public class LavatoryIntegrationDeviceDataController {
//
//    @Autowired
//    private LavatoryDeviceService lavatoryDeviceService;
//
//    @PostMapping("/list")
//    public void list(@RequestBody List<LavatoryRequest> requests) {
//        lavatoryDeviceService.list(requests);
//    }
//
//}
