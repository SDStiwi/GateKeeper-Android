package com.sdstiwi.gatekeeper;

public class StudentClassModel {
    private String sessionId;
    private String classId;
    private String className;
    private String courseCode;
    private String date;
    private String startTime;
    private String roomname;
    private String endTime;

    public StudentClassModel(String sessionId, String classId, String className, String courseCode, String date, String startTime, String endTime,String roomname) {
        this.sessionId = sessionId;
        this.classId = classId;
        this.className = className;
        this.courseCode = courseCode;
        this.roomname=roomname;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getSessionId() { return sessionId; }
    public String getClassId() { return classId; }
    public String getClassName() { return className; }

    public String getRoomname() {
        return roomname;
    }

    public String getCourseCode() { return courseCode; }
    public String getDate() { return date; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
}
