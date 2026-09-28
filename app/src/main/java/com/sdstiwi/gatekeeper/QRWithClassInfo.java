package com.sdstiwi.gatekeeper;

import android.graphics.Bitmap;

public class QRWithClassInfo {
    public Bitmap qrCode;
    public String className;

    public QRWithClassInfo(Bitmap qrCode, String className) {
        this.qrCode = qrCode;
        this.className = className;
    }
}

