package com.sdstiwi.gatekeeper;

import java.util.Date;

public class AttendanceModel {
    private String attendanceId;
    private String userId;
    private String sessionId;
    private String qrCodeId;
    private Date markedAt;
    private String deviceInfo;

    public AttendanceModel() {}

    public AttendanceModel(String attendanceId, String userId, String sessionId, String qrCodeId, Date markedAt, String deviceInfo) {
        this.attendanceId = attendanceId;
        this.userId = userId;
        this.sessionId = sessionId;
        this.qrCodeId = qrCodeId;
        this.markedAt = markedAt;
        this.deviceInfo = deviceInfo;
    }

    public String getAttendanceId() { return attendanceId; }
    public String getUserId() { return userId; }
    public String getSessionId() { return sessionId; }
    public String getQrCodeId() { return qrCodeId; }
    public Date getMarkedAt() { return markedAt; }
    public String getDeviceInfo() { return deviceInfo; }
}

