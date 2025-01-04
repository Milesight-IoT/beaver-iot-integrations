package com.milesight.beaveriot.integration.wp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WpMeeting {
//    "schedule": {
//            "conferenceRecordId": 245698,
//            "startTime": 1732710600,
//            "endTime": 1732716000,
//            "originalStartTime": 1732710600,
//            "originalEndTime": 1732716000,
//            "allowCheckIn": false,
//            "shouldCheckIn": false,
//            "allowCheckOut": false,
//            "status": "NOT_STARTED",
//            "subject": "图像会议室的预约",
//            "meetingRoomName": "图像会议室",
//            "meetingRoomId": 2551,
//            "meetingRoomType": "common",
//            "buildingId": 1300,
//            "floorId": 2210,
//            "conferenceId": 249275,
//            "host": {
//                "memberId": 73825,
//                "name": "test",
//                "email": "linzy@milesight.com",
//                "role": "HOST",
//                "isResource": false,
//                "accessibleRealSchedule": false,
//                "userId": 78902,
//                "hasCheckined": false
//            },
//            "notificationTime": 15,
//            "willStartReminderMinutes": [
//                15
//            ],
//            "scheduleType": "CONFERENCE",
//            "canBeExtended": false,
//            "roomExpired": false,
//            "checkInStatistics": false
//        },

    private Integer conferenceRecordId;
    private String subject;
    private String meetingRoomName;
    private String meetingRoomId;
    private String meetingId;
    private String firstStartTime;
    private String lastEndTime;
    private Long createTime;
    private String startDate;
    private String startTime;
}
