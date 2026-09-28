package com.sdstiwi.gatekeeper;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.text.SimpleDateFormat;
import android.util.Log;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StudentQRCodeFetcher {
    private final DatabaseHelper dbHelper;

    public StudentQRCodeFetcher(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public void deactivateExpiredQRCodes(SQLiteDatabase db) {
        long currentTimeMillis = System.currentTimeMillis();

        String updateQuery = "UPDATE qr_codes SET isActive = 0 WHERE expiresAt < ? AND isActive = 1";

        db.execSQL(updateQuery, new Object[]{currentTimeMillis});
    }

    public List<QRCodeItem> getActiveQRCodesForStudent(String studentId) {
        List<QRCodeItem> qrCodeList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        deactivateExpiredQRCodes(db);

        // All comparisons use the device epoch clock. Time zones are only used for display.
        long currentTimeMillis = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Log.d("QRCodeTime", "Current time: " + sdf.format(new Date(currentTimeMillis)) +
                " (" + currentTimeMillis + " ms)");

        String query = "SELECT classes.className, qr_codes.qrCodeImage, sessions.sessionId, " +
                "sessions.startTime, sessions.endTime " +
                "FROM qr_codes " +
                "JOIN sessions ON qr_codes.targetId = sessions.sessionId " +
                "JOIN classes ON sessions.classId = classes.classId " +
                "JOIN enrollments ON enrollments.classId = classes.classId " +
                "WHERE enrollments.studentId = ? " +
                "AND enrollments.status = 'approved' " +
                "AND qr_codes.type = 'attendance' " +
                "AND qr_codes.createdAt <= ? " +       // QR created before current time
                "AND qr_codes.expiresAt >= ? " +
                "AND qr_codes.isActive = 1 " +
                "AND sessions.attendanceEnabled = 1 " +
                "AND sessions.startTime <= ? " +
                "AND sessions.endTime >= ?";

        Cursor cursor = db.rawQuery(query, new String[]{
                studentId,
                String.valueOf(currentTimeMillis),
                String.valueOf(currentTimeMillis),
                String.valueOf(currentTimeMillis),
                String.valueOf(currentTimeMillis)
        });

        while (cursor.moveToNext()) {
            String className = cursor.getString(cursor.getColumnIndexOrThrow("className"));
            byte[] imageBlob = cursor.getBlob(cursor.getColumnIndexOrThrow("qrCodeImage"));
            Bitmap qrCode = decodeQRCode(imageBlob);
            String sessionId = cursor.getString(cursor.getColumnIndexOrThrow("sessionId"));
            long startTime = TimeUtils.parseTimestamp(cursor.getString(cursor.getColumnIndexOrThrow("startTime")));
            long endTime = TimeUtils.parseTimestamp(cursor.getString(cursor.getColumnIndexOrThrow("endTime")));

            // Log session start and end time in readable format
            String startFormatted = sdf.format(new Date(startTime));
            String endFormatted = sdf.format(new Date(endTime));

            Log.d("QRCodeLog", "Student ID: " + studentId +
                    ", Class: " + className +
                    ", Session ID: " + sessionId +
                    ", Start Time: " + startFormatted +
                    ", End Time: " + endFormatted);

            qrCodeList.add(new QRCodeItem(className, qrCode, sessionId));
        }

        cursor.close();
        db.close();

        return qrCodeList;
    }


    private Bitmap decodeQRCode(byte[] blob) {
        return BitmapFactory.decodeByteArray(blob, 0, blob.length);
    }

    // QRCodeItem model
    public static class QRCodeItem {
        public String className;
        public Bitmap qrCode;
        public String sessionId;

        public QRCodeItem(String className, Bitmap qrCode, String sessionId) {
            this.className = className;
            this.qrCode = qrCode;
            this.sessionId = sessionId;
        }
    }
}
