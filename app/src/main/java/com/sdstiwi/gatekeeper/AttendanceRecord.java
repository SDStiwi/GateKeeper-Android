package com.sdstiwi.gatekeeper;

public class AttendanceRecord {
    private String attendanceId;
    private String userId;
    private String sessionId;
    private String studentName;
    private long markedAt;

    // Getters and setters...

    public String getAttendanceId() { return attendanceId; }
    public void setAttendanceId(String id) { this.attendanceId = id; }

    public String getUserId() { return userId; }
    public void setUserId(String id) { this.userId = id; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String id) { this.sessionId = id; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String name) { this.studentName = name; }

    public long getMarkedAt() { return markedAt; }
    public void setMarkedAt(long markedAt) { this.markedAt = markedAt; }
}
