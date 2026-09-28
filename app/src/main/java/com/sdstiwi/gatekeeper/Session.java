package com.sdstiwi.gatekeeper;

public class Session {
    private String sessionId;
    private String classId;
    private String date;
    private String startTime;
    private String endTime;
    private int attendanceEnabled;
    private String className; // New field
    private String roomName;  // New field

    // Updated Constructor
    public Session(String sessionId, String classId, String date, String startTime, String endTime, int attendanceEnabled, String className, String roomName) {
        this.sessionId = sessionId;
        this.classId = classId;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.attendanceEnabled = attendanceEnabled;
        this.className = className;
        this.roomName = roomName;
    }

    // Getters
    public String getSessionId() { return sessionId; }
    public String getClassId() { return classId; }
    public String getDate() { return date; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public int getAttendanceEnabled() { return attendanceEnabled; }
    public String getClassName() { return className; }
    public String getRoomName() { return roomName; }

    // Setters
    public void setClassName(String className) { this.className = className; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    @Override
    public String toString() {
        return "Session ID: " + sessionId + ", Class: " + className + ", Date: " + date +
                ", Start: " + startTime + ", End: " + endTime + ", Room: " + roomName;
    }
}
