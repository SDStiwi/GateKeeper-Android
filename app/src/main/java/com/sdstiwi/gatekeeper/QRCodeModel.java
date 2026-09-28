package com.sdstiwi.gatekeeper;

import com.google.firebase.Timestamp;

public class QRCodeModel {
    private String qrCodeId;
    private String value;
    private String type; // "door_access" or "attendance"
    private String userId;
    private String targetId;
    private Timestamp createdAt;
    private Timestamp expiresAt;
    private boolean isActive;
    private String qrCodeImageBase64; // Base64 encoded string of the QR image

    // Empty constructor required for Firestore
    public QRCodeModel() {}

    public QRCodeModel(String qrCodeId, String value, String type, String userId, String targetId,
                       Timestamp createdAt, Timestamp expiresAt, boolean isActive, String qrCodeImageBase64) {
        this.qrCodeId = qrCodeId;
        this.value = value;
        this.type = type;
        this.userId = userId;
        this.targetId = targetId;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.isActive = isActive;
        this.qrCodeImageBase64 = qrCodeImageBase64;
    }

    public String getQrCodeId() {
        return qrCodeId;
    }

    public void setQrCodeId(String qrCodeId) {
        this.qrCodeId = qrCodeId;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Timestamp expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }

    public String getQrCodeImageBase64() {
        return qrCodeImageBase64;
    }

    public void setQrCodeImageBase64(String qrCodeImageBase64) {
        this.qrCodeImageBase64 = qrCodeImageBase64;
    }
}
