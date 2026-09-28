package com.sdstiwi.gatekeeper;

public class ClassItem {
    private String classId, className, teacherId, schedule, roomId;

    public ClassItem(String classId, String className, String teacherId, String schedule, String roomId) {
        this.classId = classId;
        this.className = className;
        this.teacherId = teacherId;
        this.schedule = schedule;
        this.roomId = roomId;
    }

    public String getClassId() { return classId; }
    public String getClassName() { return className; }
    public String getTeacherId() { return teacherId; }
    public String getSchedule() { return schedule; }
    public String getRoomId() { return roomId; }
}
