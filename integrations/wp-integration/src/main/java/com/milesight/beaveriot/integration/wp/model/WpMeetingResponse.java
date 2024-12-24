package com.milesight.beaveriot.integration.wp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WpMeetingResponse {
//    {"data":{"id":249376,"subject":"111","scheduleSetting":{"startTime":"01:00","duration":30,"notificationTime":15,"willStartReminderMinutes":[15],"scheduleMode":"ONCE","simpleRepeatType":"NEVER","startDate":"2024-11-28","syncToThirdParty":true,"timezone":"UTC+8 Asia/Shanghai","enableDst":true,"firstStartTime":1732726800,"lastEndTime":1732730400},"host":{"memberId":73825,"name":"test","email":"linzy@milesight.com","role":"HOST","isResource":false,"accessibleRealSchedule":false,"userId":78902},"participants":[],"meetingRoom":{"id":2566,"name":"111","buildingName":"test","connected":false,"expired":false,"type":"common","buildingId":1300,"meetingRoomSettings":{"id":2512,"meetingRoomId":2566,"enterpriseId":101741,"restrictInWorkingHours":true,"allDay":true,"minStartTime":"00:00","maxEndTime":"00:00","minDuration":30,"maxDuration":180,"allowToBookBefore":365,"repeatable":true,"checkInRequired":true,"checkInNoticeTime":5,"checkInPermission":"Only Organizer","autoRelease":true,"autoReleaseTime":10,"checkOutPermission":"Only Organizer","checkInStatistics":false,"displayBooking":true,"autoCheckOut":true,"autoCheckOutDuration":15,"lightOnCheckIn":false,"lightOffCheckOut":false,"lightOnOffWork":false,"accessControlOn":false,"automaticCheckInWhenPeopleDetected":false,"automaticExtendWhenPeopleDetected":false}},"schedule":{"conferenceRecordId":245794,"startTime":1732726800,"endTime":1732728600,"originalStartTime":1732726800,"originalEndTime":1732728600,"allowCheckIn":false,"shouldCheckIn":false,"allowCheckOut":false,"status":"NOT_STARTED","subject":"111","meetingRoomName":"111","meetingRoomId":2566,"meetingRoomType":"common","buildingId":1300,"conferenceId":249376,"host":{"memberId":73825,"name":"test","email":"linzy@milesight.com","role":"HOST","isResource":false,"accessibleRealSchedule":false,"userId":78902,"hasCheckined":false},"notificationTime":15,"willStartReminderMinutes":[15],"scheduleType":"CONFERENCE","canBeExtended":false,"roomExpired":false,"checkInStatistics":false},"meetingService":false,"createVisitorRecord":false,"checkInStatistics":false,"visitType":0,"allowSelfRegistration":false,"conferenceReserveSource":"WEB_GRID"},"status":"Success","requestId":"59fd4722b9aeae5c8d4bc2c4000885c6"}

    private Integer code;
    private String errorCode;
    private String message;


}
