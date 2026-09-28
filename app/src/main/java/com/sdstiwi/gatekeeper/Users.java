package com.sdstiwi.gatekeeper;

public class Users {
    private String uid, email, firstName, lastName, password, role, speciality, approvalStatus, department, createdAt, lastLogin;
    private int isActive;
    public Users() {
        // Required empty constructor for Firebase
    }

    // Constructor to initialize all fields
    public Users(String uid, String email, String firstName, String lastName, String password,
                 String role, String speciality, String approvalStatus, int isActive,
                 String department, String createdAt, String lastLogin) {
        this.uid = uid;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.password = password;
        this.role = role;
        this.speciality = speciality;
        this.approvalStatus = approvalStatus;
        this.isActive = isActive;
        this.department = department;
        this.createdAt = createdAt;
        this.lastLogin = lastLogin;
    }

    // Getters and Setters for each field
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getSpeciality() { return speciality; }
    public void setSpeciality(String speciality) { this.speciality = speciality; }

    public String getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(String approvalStatus) { this.approvalStatus = approvalStatus; }

    public int getIsActive() { return isActive; }
    public void setIsActive(int isActive) { this.isActive = isActive; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getLastLogin() { return lastLogin; }
    public void setLastLogin(String lastLogin) { this.lastLogin = lastLogin; }
}
