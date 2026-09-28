package com.sdstiwi.gatekeeper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Data access for the attendance table, including the validated QR check-in in {@link #recordAttendance}.
 *
 * <p>Queries were moved here unchanged from {@link DatabaseHelper}, which still exposes them as
 * delegating methods so existing callers keep working. Connection handling stays in the helper.
 */
public class AttendanceDao {
    private static final String TAG = "AttendanceDao";
    private final SQLiteOpenHelper helper;

    public AttendanceDao(SQLiteOpenHelper helper) {
        this.helper = helper;
    }

    // Insert attendance record
    public boolean insertAttendance(String userId, String sessionId, String qrCodeId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("userId", userId);
        values.put("sessionId", sessionId);
        values.put("qrCodeId", qrCodeId);

        long result = db.insert("attendance", null, values);
        return result != -1;
    }

    /**
     * Validates the session/QR/enrollment state and records one attendance per student/session.
     * This is the single application entry point for attendance recording.
     */
    public String recordAttendance(String studentId, String sessionId) {
        if (studentId == null || studentId.isEmpty() || sessionId == null || sessionId.isEmpty()) {
            return "INVALID_REQUEST";
        }

        SQLiteDatabase db = helper.getWritableDatabase();
        long now = System.currentTimeMillis();
        Cursor cursor = null;
        try {
            String query = "SELECT s.startTime, s.endTime, s.attendanceEnabled, " +
                    "q.qrCodeId, q.createdAt, q.expiresAt, q.isActive " +
                    "FROM sessions s " +
                    "LEFT JOIN qr_codes q ON q.targetId = s.sessionId AND q.type = 'attendance' " +
                    "WHERE s.sessionId = ? " +
                    "ORDER BY q.createdAt DESC LIMIT 1";
            cursor = db.rawQuery(query, new String[]{sessionId});

            if (!cursor.moveToFirst()) return "INVALID_SESSION";

            long start = TimeUtils.parseTimestamp(cursor.getString(0));
            long end = TimeUtils.parseTimestamp(cursor.getString(1));
            int attendanceEnabled = cursor.getInt(2);
            String qrCodeId = cursor.getString(3);
            long qrCreated = TimeUtils.parseTimestamp(cursor.getString(4));
            long qrExpires = TimeUtils.parseTimestamp(cursor.getString(5));
            int qrActive = cursor.getInt(6);

            if (attendanceEnabled != 1 || start < 0 || end < 0 || now < start || now > end) {
                return "SESSION_INACTIVE";
            }
            if (qrCodeId == null || qrActive != 1 || qrCreated < 0 || qrExpires < 0 ||
                    now < qrCreated || now > qrExpires) {
                return "QR_EXPIRED";
            }

            Cursor enrollment = db.rawQuery(
                    "SELECT 1 FROM enrollments WHERE studentId = ? AND classId = " +
                            "(SELECT classId FROM sessions WHERE sessionId = ?) AND status = 'approved' LIMIT 1",
                    new String[]{studentId, sessionId});
            boolean enrolled = enrollment.moveToFirst();
            enrollment.close();
            if (!enrolled) return "NOT_ENROLLED";

            Cursor duplicate = db.rawQuery(
                    "SELECT 1 FROM attendance WHERE userId = ? AND sessionId = ? LIMIT 1",
                    new String[]{studentId, sessionId});
            boolean alreadyCheckedIn = duplicate.moveToFirst();
            duplicate.close();
            if (alreadyCheckedIn) return "ALREADY_CHECKED_IN";

            ContentValues values = new ContentValues();
            values.put("attendanceId", UUID.randomUUID().toString());
            values.put("userId", studentId);
            values.put("sessionId", sessionId);
            values.put("qrCodeId", qrCodeId);
            values.put("markedAt", now);
            values.put("deviceInfo", "Device Model: " + android.os.Build.MODEL);

            long result = db.insertWithOnConflict(
                    "attendance", null, values, SQLiteDatabase.CONFLICT_IGNORE);
            if (result == -1) {
                // A unique-index conflict means another check-in won the race.
                return "ALREADY_CHECKED_IN";
            }
            return "RECORDED";
        } catch (Exception e) {
            Log.e(TAG, "Failed to record attendance", e);
            return "ERROR";
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    public List<AttendanceModel> getAllAttendanceRecords() {
        List<AttendanceModel> attendanceList = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM attendance", null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String attendanceId = cursor.getString(cursor.getColumnIndexOrThrow("attendanceId"));
                String userId = cursor.getString(cursor.getColumnIndexOrThrow("userId"));
                String sessionId = cursor.getString(cursor.getColumnIndexOrThrow("sessionId"));
                String qrCodeId = cursor.getString(cursor.getColumnIndexOrThrow("qrCodeId"));
                long markedAtMillis = cursor.getLong(cursor.getColumnIndexOrThrow("markedAt"));
                String deviceInfo = cursor.getString(cursor.getColumnIndexOrThrow("deviceInfo"));

                Date markedAtDate = new Date(markedAtMillis);
                attendanceList.add(new AttendanceModel(attendanceId, userId, sessionId, qrCodeId, markedAtDate, deviceInfo));
            } while (cursor.moveToNext());

            cursor.close();
        }

        return attendanceList;
    }

    public void insertAttendanceIntoLocalDB(Context context, String attendanceId, String userId,
                                            String sessionId, String qrCodeId, long markedAt, String deviceInfo) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("attendanceId", attendanceId);
        values.put("userId", userId);
        values.put("sessionId", sessionId);
        values.put("qrCodeId", qrCodeId);
        values.put("markedAt", markedAt); // stored as TIMESTAMP or INTEGER
        values.put("deviceInfo", deviceInfo);

        long result = db.insertWithOnConflict("attendance", null, values, SQLiteDatabase.CONFLICT_IGNORE);

        if (result == -1) {
            Log.d("LocalDB", "Already exists in local DB: " + attendanceId);
        } else {
            Log.d("LocalDB", "Inserted to local DB: " + attendanceId);
        }

        db.close();
    }
}
