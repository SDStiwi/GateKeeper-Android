package com.sdstiwi.gatekeeper;

public class ClassModel {
    private String classId;
    private String className;
    private String courseCode;
    private String roomId;
    private String teacherId;

    public ClassModel(String classId, String className, String courseCode, String roomId, String teacherId) {
        this.classId = classId;
        this.className = className;
        this.courseCode = courseCode;
        this.roomId = roomId;
        this.teacherId = teacherId;
    }

    public String getClassId() { return classId; }
    public String getClassName() { return className; }
    public String getCourseCode() { return courseCode; }
    public String getRoomId() { return roomId; }
    public String getTeacherId() { return teacherId; }
}
