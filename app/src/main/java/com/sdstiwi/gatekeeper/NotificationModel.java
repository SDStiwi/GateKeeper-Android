package com.sdstiwi.gatekeeper;

public class NotificationModel {
    private String notificationId;
    private String recieverId;
    private String senderId;
    private String message;
    private boolean isRead;
    private String createdAt;
    private String classId;
    private String notificationType;  // Add this field




    public NotificationModel(){}
    // Constructor
    public NotificationModel(String notificationId, String recieverId, String message, boolean isRead, String createdAt,
                             String classId, String senderId, String notificationType) {
        this.notificationId = notificationId;
        this.recieverId = recieverId;
        this.message = message;
        this.isRead = isRead;
        this.createdAt = createdAt;
        this.classId = classId;
        this.senderId = senderId;
        this.notificationType = notificationType;
    }




    // Getters and Setters
    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }

    public String getReceiverId() { return recieverId; }
    public void setreceiverId(String receiverId) { this.recieverId = receiverId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getClassId() { return classId; }
    public void setClassId(String classId) { this.classId = classId; }

    public String getSender() { return senderId; }
    public void setSender(String senderId) { this.senderId = senderId; }

    // Getter and Setter for notificationType
    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }
    public void setRecieverId(String recieverId) {
        this.recieverId = recieverId;
    }

}

