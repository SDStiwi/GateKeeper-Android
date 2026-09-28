package com.sdstiwi.gatekeeper;

public class Classes {
    private String classId;
    private String className;
    private String courseCode;
    private String startDate;
    private String endDate;
    private int isActive;
    private String roomId;
    private String teacherId;
    private String schedule;

    public Classes() {
        // Default constructor required for Firestore
    }

    public Classes(String classId, String className, String courseCode,
                   String startDate, String endDate, int isActive,
                   String roomId, String teacherId, String schedule) {
        this.classId = classId;
        this.className = className;
        this.courseCode = courseCode;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isActive = isActive;
        this.roomId = roomId;
        this.teacherId = teacherId;
        this.schedule = schedule;
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public int getIsActive() {
        return isActive;
    }

    public void setIsActive(int isActive) {
        this.isActive = isActive;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }

    public String getSchedule() {
        return schedule;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }
}
