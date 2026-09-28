package com.sdstiwi.gatekeeper;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * Rich, deterministic portfolio dataset.
 * Every app launch starts from a clean scenario so a live demo is repeatable.
 */
public final class DemoDataSeeder {
    private DemoDataSeeder() {}

    public static void seed(Context context) {
        DatabaseHelper helper = new DatabaseHelper(context);
        SQLiteDatabase db = helper.getWritableDatabase();
        String today = new SimpleDateFormat("EEE", Locale.ENGLISH).format(new Date());
        long now = System.currentTimeMillis();

        db.beginTransaction();
        try {
            // ---------- USERS ----------
            updateUser(db, "1", "John", "Doe", "Computer Networks", "Computer Science", "teacher", 1, "approved");
            updateUser(db, "2", "Alice", "Smith", "Software Engineering", "Computer Science", "student", 1, "approved");
            upsertUser(db, "6", "Emma", "Wilson", "Cybersecurity", "Computer Science", "student", 1, "approved", "emma.wilson@example.com");
            upsertUser(db, "7", "Liam", "Martin", "Artificial Intelligence", "Computer Science", "student", 1, "approved", "liam.martin@example.com");

            // Keep extra accounts to showcase registration/approval states.
            upsertUser(db, "4", "Mary", "Johnson", "Physics", "Science", "teacher", 1, "approved", "teacher2@example.com");
            upsertUser(db, "5", "Bob", "Brown", "Physics", "Science", "student", 0, "pending", "student2@example.com");

            // ---------- ROOMS ----------
            db.execSQL("UPDATE rooms SET name='Room 101', building='Main Building', floor='1st Floor', deviceId='GK-DOOR-101', isActive=1 WHERE roomId='R1'");
            db.execSQL("UPDATE rooms SET name='Room 102', building='Main Building', floor='1st Floor', deviceId='GK-DOOR-102', isActive=1 WHERE roomId='R2'");
            db.execSQL("UPDATE rooms SET name='Lab 201', building='Technology Block', floor='2nd Floor', deviceId='GK-DOOR-201', isActive=0 WHERE roomId='R3'");

            // ---------- CLASSES ----------
            // Today is intentionally used in the first and third class so the dashboard is always populated.
            setClass(db, "C1", "Distributed Systems", "RSID301", "R1", "1", today + ",Fri 09:00-10:30", 1);
            setClass(db, "C2", "Network Security", "SEC302", "R2", "1", "Thu 11:00-12:00", 1);
            setClass(db, "C3", "Cloud Computing", "CLOUD303", "R1", "1", today + ",Mon 14:00-15:30", 1);
            setClass(db, "C4", "Artificial Intelligence", "AI304", "R3", "1", "Tue 15:30-17:00", 1);
            setClass(db, "C5", "Mobile Application Development", "MOB305", "R2", "1", "Wed,Fri 11:00-12:00", 1);
            setClass(db, "C6", "Data Mining", "DM306", "R2", "4", "Tue,Thu 14:00-15:30", 1);
            setClass(db, "C7", "Human Computer Interaction", "HCI307", "R3", "4", "Wed 09:00-10:30", 1);
            setClass(db, "C8", "Computer Graphics", "CG308", "R2", "4", "Fri 14:00-15:30", 1);
            setClass(db, "C9", "Archived Networking", "NET099", "R3", "4", "Mon 08:00-09:00", 0);

            // ---------- ENROLLMENTS ----------
            db.execSQL("DELETE FROM enrollments");
            enroll(db, "2", "C1", "approved");
            enroll(db, "2", "C2", "approved");
            enroll(db, "2", "C3", "approved");
            enroll(db, "2", "C5", "approved");
            enroll(db, "6", "C1", "pending");
            enroll(db, "7", "C3", "rejected");
            enroll(db, "5", "C4", "pending");

            // ---------- SESSIONS ----------
            db.execSQL("DELETE FROM attendance");
            db.execSQL("DELETE FROM qr_codes");
            db.execSQL("DELETE FROM sessions");

            createActiveSession(db, "demo-C1", "C1");
            createActiveSession(db, "demo-C3", "C3");
            // A closed historical session demonstrates that attendance is not limited to the current class.
            String yesterday = offsetDate(-1);
            createSession(db, "demo-C5-yesterday", "C5", yesterday, "11:00", "12:00", 0);
            createSession(db, "demo-C2-next", "C2", offsetDate(1), "11:00", "12:00", 0);

            // ---------- ATTENDANCE ----------
            String qr1 = createQr(db, "demo-C1", "1", true, now - 15 * 60_000L, now + 45 * 60_000L);
            String qr3 = createQr(db, "demo-C3", "1", true, now - 5 * 60_000L, now + 70 * 60_000L);
            String qrOld = createQr(db, "demo-C5-yesterday", "1", false, now - 30 * 60 * 60_000L, now - 29 * 60 * 60_000L);

            // One enrolled student is already checked in for C1, allowing the demo to show duplicate protection.
            addAttendance(db, "att-001", "2", "demo-C1", qr1, now - 18 * 60_000L, "Android • Pixel Demo");
            // One historical record demonstrates attendance history; the active C3 session is intentionally unmarked
            // so the demo student can complete a successful check-in.
            addAttendance(db, "att-003", "2", "demo-C5-yesterday", qrOld, now - 26 * 60 * 60_000L, "Android • Pixel Demo");

            // ---------- ACCESS LOGS ----------
            db.execSQL("DELETE FROM access_logs");
            addAccessLog(db, "access-001", "2", "R1", qr1, now - 17 * 60_000L, 1);
            addAccessLog(db, "access-002", "2", "R1", qr1, now - 11 * 60_000L, 1);
            addAccessLog(db, "access-003", "7", "R3", qrOld, now - 24 * 60 * 60_000L, 0);

            // ---------- NOTIFICATIONS ----------
            db.execSQL("DELETE FROM notifications WHERE recieverId IN ('1','2','6','7','5')");

            // Teacher: enrollment workflow + attendance/access activity + read/unread mix.
            addNotification(db, "1", "6", "New enrollment request • Emma Wilson wants to join Distributed Systems (RSID301).", 0, now - 3 * 60_000L, "C1", "enrollement");
            addNotification(db, "1", "7", "Enrollment request for Cloud Computing (CLOUD303) was previously rejected.", 1, now - 45 * 60_000L, "C3", "reply");
            addNotification(db, "1", "2", "Alice Smith checked in to Distributed Systems • Room 101.", 0, now - 18 * 60_000L, "C1", "general");
            addNotification(db, "1", "6", "Emma Wilson checked in to Distributed Systems • Room 101.", 0, now - 12 * 60_000L, "C1", "general");
            addNotification(db, "1", "2", "Room 101 access granted • door controller GK-DOOR-101.", 1, now - 17 * 60_000L, "C1", "general");
            addNotification(db, "1", "7", "Access denied at Lab 201 • inactive room controller.", 1, now - 24 * 60 * 60_000L, "C4", "general");

            // Student: confirmation, upcoming class, reply notification and an informational message.
            addNotification(db, "2", "1", "Distributed Systems is ready for QR check-in • Room 101.", 0, now - 16 * 60_000L, "C1", "general");
            addNotification(db, "2", "1", "Your next class is Network Security at 11:00 • Room 102.", 0, now - 28 * 60_000L, "C2", "general");
            addNotification(db, "2", "1", "Your enrollment request for Cloud Computing (CLOUD303) was approved.", 1, now - 2 * 24 * 60 * 60_000L, "C3", "reply");
            addNotification(db, "2", "1", "Room 101 access granted. Welcome to class!", 1, now - 17 * 60_000L, "C1", "general");
            addNotification(db, "2", "1", "Tip: keep the QR code centered inside the scanner frame.", 1, now - 3 * 24 * 60 * 60_000L, "C1", "general");

            // Extra workflow examples for teacher notifications.
            addNotification(db, "5", "1", "Your enrollment request for Artificial Intelligence (AI304) is pending review.", 0, now - 20 * 60_000L, "C4", "reply");
            addNotification(db, "7", "1", "Your enrollment request for Cloud Computing (CLOUD303) was rejected.", 0, now - 2 * 24 * 60 * 60_000L, "C3", "reply");

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private static void updateUser(SQLiteDatabase db, String id, String first, String last,
                                   String speciality, String department, String role, int active, String approval) {
        db.execSQL("UPDATE users SET firstName=?, lastName=?, speciality=?, department=?, isActive=?, approvalStatus=?, role=? WHERE uid=?",
                new Object[]{first, last, speciality, department, active, approval, role, id});
    }

    private static void upsertUser(SQLiteDatabase db, String id, String first, String last, String speciality,
                                   String department, String role, int active, String approval, String email) {
        db.execSQL("INSERT OR REPLACE INTO users(uid,email,password,firstName,lastName,speciality,role,department,isActive,approvalStatus,createdAt,lastLogin) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",
                new Object[]{id, email, "password123", first, last, speciality, role, department, active, approval,
                        "2026-09-01 09:00:00", null});
    }

    private static void setClass(SQLiteDatabase db, String id, String name, String code, String room, String teacher,
                                 String schedule, int active) {
        db.execSQL("UPDATE classes SET className=?, courseCode=?, startDate='2026-09-01', endDate='2027-01-31', roomId=?, teacherId=?, schedule=?, isActive=? WHERE classId=?",
                new Object[]{name, code, room, teacher, schedule, active, id});
    }

    private static void enroll(SQLiteDatabase db, String student, String classId, String status) {
        db.execSQL("INSERT INTO enrollments(studentId,classId,status) VALUES(?,?,?)", new Object[]{student, classId, status});
    }

    private static void createActiveSession(SQLiteDatabase db, String id, String classId) {
        long start = System.currentTimeMillis() - 15 * 60_000L;
        long end = System.currentTimeMillis() + 45 * 60_000L;
        db.execSQL("INSERT INTO sessions(sessionId,classId,date,startTime,endTime,attendanceEnabled) VALUES(?,?,?,?,?,1)",
                new Object[]{id, classId, todayDate(), start, end});
    }

    private static void createSession(SQLiteDatabase db, String id, String classId, String date,
                                      String start, String end, int enabled) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH);
            long startMillis = sdf.parse(date + " " + start).getTime();
            long endMillis = sdf.parse(date + " " + end).getTime();
            db.execSQL("INSERT INTO sessions(sessionId,classId,date,startTime,endTime,attendanceEnabled) VALUES(?,?,?,?,?,?)",
                    new Object[]{id, classId, date, startMillis, endMillis, enabled});
        } catch (Exception e) {
            throw new IllegalStateException("Invalid demo session time: " + date + " " + start + "-" + end, e);
        }
    }

    private static String todayDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(new Date());
    }

    private static String createQr(SQLiteDatabase db, String sessionId, String teacherId, boolean active,
                                   long created, long expires) {
        String qrId = "qr-" + sessionId;
        try {
            JSONObject value = new JSONObject();
            value.put("type", "attendance");
            value.put("sessionId", sessionId);
            byte[] image = createQrBitmap(value.toString());
            db.execSQL("INSERT INTO qr_codes(qrCodeId,value,type,userId,targetId,createdAt,expiresAt,isActive,qrCodeImage) VALUES(?,?,?,?,?,?,?,?,?)",
                    new Object[]{qrId, value.toString(), "attendance", teacherId, sessionId, created, expires, active ? 1 : 0, image});
        } catch (Exception e) {
            db.execSQL("INSERT INTO qr_codes(qrCodeId,value,type,userId,targetId,createdAt,expiresAt,isActive) VALUES(?,?,?,?,?,?,?,?)",
                    new Object[]{qrId, "{\"type\":\"attendance\",\"sessionId\":\"" + sessionId + "\"}", "attendance", teacherId, sessionId, created, expires, active ? 1 : 0});
        }
        return qrId;
    }

    private static byte[] createQrBitmap(String value) throws Exception {
        BitMatrix matrix = new MultiFormatWriter().encode(value, BarcodeFormat.QR_CODE, 600, 600);
        Bitmap bitmap = Bitmap.createBitmap(600, 600, Bitmap.Config.RGB_565);
        for (int x = 0; x < 600; x++) {
            for (int y = 0; y < 600; y++) bitmap.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        return out.toByteArray();
    }

    private static void addAttendance(SQLiteDatabase db, String id, String user, String session,
                                      String qr, long markedAt, String device) {
        db.execSQL("INSERT INTO attendance(attendanceId,userId,sessionId,qrCodeId,markedAt,deviceInfo) VALUES(?,?,?,?,?,?)",
                new Object[]{id, user, session, qr, markedAt, device});
    }

    private static void addAccessLog(SQLiteDatabase db, String id, String user, String room, String qr,
                                     long timestamp, int granted) {
        db.execSQL("INSERT INTO access_logs(logId,userId,roomId,qrCodeId,timestamp,accessGranted) VALUES(?,?,?,?,?,?)",
                new Object[]{id, user, room, qr, timestamp, granted});
    }

    private static void addNotification(SQLiteDatabase db, String receiver, String sender, String message,
                                        int read, long created, String classId, String type) {
        db.execSQL("INSERT INTO notifications(recieverId,senderId,message,isRead,createdAt,classId,notificationType) VALUES(?,?,?,?,?,?,?)",
                new Object[]{receiver, sender, message, read, String.valueOf(created), classId, type});
    }

    private static String offsetDate(int days) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.DAY_OF_YEAR, days);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(cal.getTime());
    }
}
