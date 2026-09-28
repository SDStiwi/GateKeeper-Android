package com.sdstiwi.gatekeeper;

public class Enrollement {
    private String enrollmentId, studentId, classId, status;
    public Enrollement() {}
    public Enrollement(String enrollmentId, String studentId, String classId, String status) {
        this.enrollmentId = enrollmentId;
        this.studentId = studentId;
        this.classId = classId;
        this.status = status;
    }

    // Getters and setters
    public String getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(String enrollmentId) { this.enrollmentId = enrollmentId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getClassId() { return classId; }
    public void setClassId(String classId) { this.classId = classId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

}
