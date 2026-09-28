package com.sdstiwi.gatekeeper;

import android.graphics.Bitmap;

public class QRCodeItem {
    String className;
    Bitmap qrCode;
    String sessionId; // Add sessionId here to store the session identifier

    public QRCodeItem(String className, Bitmap qrCode, String sessionId) {
        this.className = className;
        this.qrCode = qrCode;
        this.sessionId = sessionId; // Initialize sessionId
    }
}
