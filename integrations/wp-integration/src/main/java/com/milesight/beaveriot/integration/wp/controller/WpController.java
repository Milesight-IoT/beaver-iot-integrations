package com.milesight.beaveriot.integration.wp.controller;

import com.milesight.beaveriot.base.response.ResponseBody;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import com.milesight.beaveriot.integration.wp.model.WpMeetingRequest;
import com.milesight.beaveriot.integration.wp.model.WpMeetingResponse;
import com.milesight.beaveriot.integration.wp.service.WpMeetingRoomService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 */
@Slf4j
@RestController
@RequestMapping("/meeting")
public class WpController {


    @Autowired
    private WpMeetingRoomService wpMeetingRoomService;

    @PostMapping
        public ResponseBody<WpMeetingResponse> addMeeting(@RequestBody WpMeetingRequest wpMeetingRequest) {
        return ResponseBuilder.success(wpMeetingRoomService.addMeetingRoom(wpMeetingRequest, null));
    }

}
