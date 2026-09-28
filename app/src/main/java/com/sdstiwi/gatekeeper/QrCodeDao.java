package com.sdstiwi.gatekeeper;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Base64;
import android.util.Log;
import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Data access for the qr_codes table.
 *
 * <p>Queries were moved here unchanged from {@link DatabaseHelper}, which still exposes them as
 * delegating methods so existing callers keep working. Connection handling stays in the helper.
 */
public class QrCodeDao {
    private final SQLiteOpenHelper helper;

    public QrCodeDao(SQLiteOpenHelper helper) {
        this.helper = helper;
    }

    public List<QRCodeModel> getAllQRCodes() {
        List<QRCodeModel> qrList = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM qr_codes", null);
        if (cursor.moveToFirst()) {
            do {
                QRCodeModel qr = new QRCodeModel();

                qr.setQrCodeId(cursor.getString(cursor.getColumnIndexOrThrow("qrCodeId")));
                qr.setValue(cursor.getString(cursor.getColumnIndexOrThrow("value")));
                qr.setType(cursor.getString(cursor.getColumnIndexOrThrow("type")));
                qr.setUserId(cursor.getString(cursor.getColumnIndexOrThrow("userId")));
                qr.setTargetId(cursor.getString(cursor.getColumnIndexOrThrow("targetId")));

                // Read as milliseconds from SQLite
                long createdAtMillis = cursor.getLong(cursor.getColumnIndexOrThrow("createdAt"));
                long expiresAtMillis = cursor.getLong(cursor.getColumnIndexOrThrow("expiresAt"));

                // Log raw millis for debugging
                Log.d("QRCodeSync", "Raw createdAtMillis: " + createdAtMillis);
                Log.d("QRCodeSync", "Raw expiresAtMillis: " + expiresAtMillis);

                // Convert to Firestore Timestamp using the milliseconds directly
                qr.setCreatedAt(new com.google.firebase.Timestamp(createdAtMillis / 1000, 0)); // Convert to seconds for Firestore
                qr.setExpiresAt(new com.google.firebase.Timestamp(expiresAtMillis / 1000, 0)); // Convert to seconds for Firestore

                qr.setIsActive(cursor.getInt(cursor.getColumnIndexOrThrow("isActive")) == 1);

                byte[] blob = cursor.getBlob(cursor.getColumnIndexOrThrow("qrCodeImage"));
                if (blob != null) {
                    qr.setQrCodeImageBase64(Base64.encodeToString(blob, Base64.NO_WRAP));
                }

                qrList.add(qr);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return qrList;
    }

    public List<QRCodeModel> getAllQRCodesFromLDB() {
        List<QRCodeModel> qrList = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM qr_codes", null);
        if (cursor.moveToFirst()) {
            do {
                QRCodeModel qr = new QRCodeModel();
                qr.setQrCodeId(cursor.getString(cursor.getColumnIndexOrThrow("qrCodeId")));
                qr.setValue(cursor.getString(cursor.getColumnIndexOrThrow("value")));
                qr.setType(cursor.getString(cursor.getColumnIndexOrThrow("type")));
                qr.setUserId(cursor.getString(cursor.getColumnIndexOrThrow("userId")));
                qr.setTargetId(cursor.getString(cursor.getColumnIndexOrThrow("targetId")));
                long createdAtRaw = cursor.getLong(cursor.getColumnIndexOrThrow("createdAt"));
                long expiresAtRaw = cursor.getLong(cursor.getColumnIndexOrThrow("expiresAt"));

                // Check if it's in seconds (less than year 3000)
                if (createdAtRaw < 100000000000L) {
                    createdAtRaw *= 1000L;
                    expiresAtRaw *= 1000L;
                }

                qr.setCreatedAt(new Timestamp(new Date(createdAtRaw)));
                qr.setExpiresAt(new Timestamp(new Date(expiresAtRaw)));
                qr.setIsActive(cursor.getInt(cursor.getColumnIndexOrThrow("isActive")) == 1);

                // Convert BLOB to base64
                byte[] blob = cursor.getBlob(cursor.getColumnIndexOrThrow("qrCodeImage"));
                if (blob != null) {
                    qr.setQrCodeImageBase64(Base64.encodeToString(blob, Base64.DEFAULT));
                }

                qrList.add(qr);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return qrList;
    }
}
