package com.sdstiwi.gatekeeper;

import static androidx.core.graphics.drawable.DrawableKt.toBitmap;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Creates and looks up attendance sessions and their QR codes for a teacher.
 *
 * A session is one occurrence of a class. When a teacher opens the attendance screen and the
 * current time falls inside a scheduled slot, a row is written to {@code sessions} and a QR code
 * containing the session id is stored in {@code qr_codes} so students can scan it.
 */
public class SessionManager {
    private Context context;
    private DatabaseHelper dbHelper;

    public SessionManager(Context context) {
        this.context = context;
        this.dbHelper = new DatabaseHelper(context);
    }

    /**
     * Debug helper: logs the teacher's active classes scheduled for today.
     */
    public void logTodaysClassesForTeacher(String teacherId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String currentDay = new SimpleDateFormat("EEE", Locale.ENGLISH).format(new Date()); // E.g., "Mon"

        Cursor cursor = db.rawQuery("SELECT * FROM classes WHERE teacherId = ? AND isActive = 1", new String[]{teacherId});

        Log.d("ClassLogger", "Classes for " + currentDay + ":");

        boolean found = false;
        while (cursor.moveToNext()) {
            String schedule = cursor.getString(cursor.getColumnIndexOrThrow("schedule"));
            String className = cursor.getString(cursor.getColumnIndexOrThrow("className"));

            if (schedule != null && schedule.contains(currentDay)) {
                String[] parts = schedule.split(" ");
                if (parts.length == 2) {
                    String[] days = parts[0].split(",");
                    String[] timeRange = parts[1].split("-");

                    for (String day : days) {
                        if (currentDay.equalsIgnoreCase(day)) {
                            String start = timeRange[0];
                            String end = timeRange[1];
                            Log.d("ClassLogger", "" + className + " from " + start + " to " + end);
                            found = true;
                        }
                    }
                }
            }
        }

        if (!found) {
            Log.d("ClassLogger", "No classes scheduled for today.");
        }

        cursor.close();
    }

    /**
     * Creates today's session for the teacher if a class is running right now and none exists yet.
     *
     * Schedules are stored as {@code "Mon,Wed 08:00-10:00"}. The current weekday and time are
     * matched against that pattern; at most one session per class per day is created.
     *
     * In demo mode this delegates to {@link #createDemoSessionForTeacher(String)}, which ignores
     * the real clock so the app can be shown at any time.
     *
     * @param teacherId uid of the signed-in teacher
     * @return the QR bitmap for the newly created session, or {@code null} if nothing was created
     */
    public Bitmap checkAndCreateSessionForTeacher(String teacherId) {
        if (DemoConfig.DEMO_MODE) {
            return createDemoSessionForTeacher(teacherId);
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        SQLiteDatabase writableDb = dbHelper.getWritableDatabase();

        String currentDay = new SimpleDateFormat("EEE", Locale.ENGLISH).format(new Date()); // E.g., "Mon"
        String currentTime = new SimpleDateFormat("HH:mm", Locale.ENGLISH).format(new Date());
        Log.d("SessionManager", "Checking current time " + currentTime);

        Cursor cursor = db.rawQuery("SELECT * FROM classes WHERE teacherId = ? AND isActive = 1", new String[]{teacherId});

        while (cursor.moveToNext()) {
            String schedule = cursor.getString(cursor.getColumnIndexOrThrow("schedule"));
            String classId = cursor.getString(cursor.getColumnIndexOrThrow("classId"));
            String className = cursor.getString(cursor.getColumnIndexOrThrow("className"));

            if (schedule != null && schedule.contains(currentDay)) {
                // Parse "Mon,Wed 08:00-10:00"
                String[] parts = schedule.split(" ");
                String[] days = parts[0].split(",");
                String[] timeParts = parts[1].split("-");

                String startTime = timeParts[0];
                String endTime = timeParts[1];

                for (String day : days) {
                    if (currentDay.equalsIgnoreCase(day)) {
                        Log.d("SessionManager", "Upcoming session for class '" + className + "' today from " + startTime + " to " + endTime);

                        if (isTimeNowWithin(startTime, endTime, currentTime)) {
                            if (!isSessionAlreadyCreated(db, classId)) {
                                String sessionId = UUID.randomUUID().toString();
                                long sessionStartMillis;
                                long sessionEndMillis;
                                try {
                                    sessionStartMillis = TimeUtils.combineTodayWithTime(startTime);
                                    sessionEndMillis = TimeUtils.combineTodayWithTime(endTime);
                                } catch (ParseException e) {
                                    Log.e("SessionManager", "Invalid class schedule: " + schedule, e);
                                    continue;
                                }

                                writableDb.execSQL("INSERT INTO sessions (sessionId, classId, date, startTime, endTime, attendanceEnabled) VALUES (?, ?, ?, ?, ?, 1)",
                                        new Object[]{sessionId, classId, TimeUtils.today(), sessionStartMillis, sessionEndMillis});

                                Log.d("SessionManager", "Created session: " + sessionId + " for class: " + className);

                                // Generate QR Code for the session and save to the database
                                generateQRCodeForSession(sessionId, writableDb);
                                return generateQRCodeBitmap("attendance-" + sessionId);
                            } else {
                                Log.d("SessionManager", "Session already exists today for class: " + className);
                            }
                        } else {
                            Log.d("SessionManager", "It's not yet time for class '" + className + "'. Scheduled from " + startTime + " to " + endTime);
                        }
                    }
                }
            }
        }
        cursor.close();
        return null;
    }

    /**
     * Demo-mode variant: takes the teacher's first active class and gives it a 30-minute session
     * starting now, replacing any earlier demo session that has already ended.
     *
     * @return the QR bitmap for that session, or {@code null} if the teacher has no active class
     */
    private Bitmap createDemoSessionForTeacher(String teacherId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        Cursor classCursor = db.rawQuery(
                "SELECT classId, className, schedule FROM classes " +
                "WHERE teacherId = ? AND isActive = 1 ORDER BY classId LIMIT 1",
                new String[]{teacherId});

        if (!classCursor.moveToFirst()) {
            classCursor.close();
            return null;
        }

        String classId = classCursor.getString(0);
        classCursor.getString(1); // class name is used by the UI query later
        classCursor.close();

        String sessionId = "demo-" + classId;
        Cursor existing = db.rawQuery(
                "SELECT startTime, endTime FROM sessions WHERE sessionId = ?",
                new String[]{sessionId});
        boolean exists = existing.moveToFirst();
        long existingEnd = exists ? TimeUtils.parseTimestamp(existing.getString(1)) : -1L;
        existing.close();

        if (exists && existingEnd > 0 && existingEnd < System.currentTimeMillis()) {
            db.delete("qr_codes", "targetId = ?", new String[]{sessionId});
            db.delete("sessions", "sessionId = ?", new String[]{sessionId});
            exists = false;
        }

        if (!exists) {
            long now = System.currentTimeMillis();
            long end = now + 30 * 60 * 1000L;
            db.execSQL(
                    "INSERT INTO sessions (sessionId, classId, date, startTime, endTime, attendanceEnabled) " +
                    "VALUES (?, ?, ?, ?, ?, 1)",
                    new Object[]{sessionId, classId, TimeUtils.today(), now, end});
            generateQRCodeForSession(sessionId, db);
        }

        Cursor qrCursor = db.rawQuery(
                "SELECT value FROM qr_codes WHERE targetId = ? ORDER BY createdAt DESC LIMIT 1",
                new String[]{sessionId});

        String qrValue = null;
        if (qrCursor.moveToFirst()) qrValue = qrCursor.getString(0);
        qrCursor.close();

        return qrValue == null ? null : generateQRCodeBitmap(qrValue);
    }

    /**
     * Inclusive check that {@code now} lies between {@code start} and {@code end}.
     *
     * All arguments use 24-hour {@code HH:mm}. Ranges that cross midnight are not supported.
     */
    private boolean isTimeNowWithin(String start, String end, String now) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
            Date nowTime = sdf.parse(now);
            Date startTime = sdf.parse(start);
            Date endTime = sdf.parse(end);

            return !nowTime.before(startTime) && !nowTime.after(endTime);
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Returns whether a session for this class already exists for today's date.
     */
    private boolean isSessionAlreadyCreated(SQLiteDatabase db, String classId) {
        Cursor cursor = db.rawQuery(
                "SELECT * FROM sessions WHERE classId = ? AND date(date) = date('now')",
                new String[]{classId});
        boolean exists = cursor.moveToFirst();
        Log.d("SessionManager", "Session already exists for classId " + classId + "? " + exists);
        cursor.close();
        return exists;
    }


    /**
     * Builds the QR payload (a JSON object with the type and session id), renders it to a PNG
     * and stores it in {@code qr_codes} so it can be shown again without regenerating it.
     */
    private void generateQRCodeForSession(String sessionId, SQLiteDatabase db) {
       
        JSONObject qrObject = new JSONObject();
        try {
            qrObject.put("type", "attendance");
            qrObject.put("sessionId", sessionId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String qrCodeValue = qrObject.toString(); // Final QR value will be like: {"type":"attendance","sessionId":"1234"}
        // Using context to get SharedPreferences instead of getActivity()
        SharedPreferences sharedPreferences = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        String teacherId = sharedPreferences.getString("uid", "");

        // Generate QR Code Bitmap
        Bitmap qrCodeBitmap = generateQRCodeBitmap(qrCodeValue);

        // Convert Bitmap to byte array (BLOB) to store in the database
        byte[] qrCodeImage = convertBitmapToByteArray(qrCodeBitmap);

        // Persist timestamps as epoch milliseconds. Do not manually apply a timezone offset.
        long createdAtMillis = System.currentTimeMillis();
        long expiryTimeMillis = createdAtMillis + 30 * 60 * 1000L;

        // QR validity is based on this absolute expiry timestamp.
        String qrCodeId = UUID.randomUUID().toString();
        db.execSQL("INSERT INTO qr_codes (qrCodeId, value, type, userId, targetId, createdAt, expiresAt, qrCodeImage) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                new Object[]{
                        qrCodeId,
                        qrCodeValue,
                        "attendance", // Type for attendance QR code
                        teacherId, // Assuming teacherId is available in this context
                        sessionId, // Link it to the session
                        createdAtMillis, // Store the absolute epoch timestamp
                        expiryTimeMillis, // Store the absolute epoch expiry timestamp
                        qrCodeImage // Store the QR code image as a BLOB
                });

        Log.d("SessionManager", "Generated and saved QR Code: " + qrCodeId + " with expiry: " + expiryTimeMillis + " and createdAt: " + createdAtMillis);
    }




    private Bitmap convertBitMatrixToBitmap(com.google.zxing.common.BitMatrix bitMatrix) {
        int width = bitMatrix.getWidth();
        int height = bitMatrix.getHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
        }
        return bitmap;
    }


    // Generate the QR Code Bitmap using ZXing library
    private Bitmap generateQRCodeBitmap(String value) {
        try {
            MultiFormatWriter writer = new MultiFormatWriter();
            Hashtable<EncodeHintType, String> hints = new Hashtable<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            com.google.zxing.common.BitMatrix bitMatrix = writer.encode(value, BarcodeFormat.QR_CODE, 512, 512, hints);
            Bitmap bitmap = convertBitMatrixToBitmap(bitMatrix);
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Convert Bitmap to byte array (for storing in the database)
    private byte[] convertBitmapToByteArray(Bitmap bitmap) {
        if (bitmap != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            return stream.toByteArray();
        }
        return null;
    }

    public String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }



    /**
     * Finds the QR code the teacher should currently be showing.
     *
     * A QR is valid only while the QR itself is active and unexpired, and its session has
     * attendance enabled and is between its start and end time.
     *
     * @return the QR image with its class name, or {@code null} if no session is currently valid
     */
    public QRWithClassInfo getValidQRCodeWithClassForTeacher(String teacherId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        long currentTimeMillis = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Log.d("TimeDebug", "Current time: " + sdf.format(new Date(currentTimeMillis)) +
                " (" + currentTimeMillis + " ms)");

        // A QR is displayed only while its session is active and the QR itself is unexpired.
        String query = "SELECT qr.qrCodeImage, c.className, s.startTime " +
                "FROM qr_codes qr " +
                "JOIN sessions s ON qr.targetId = s.sessionId " +
                "JOIN classes c ON s.classId = c.classId " +
                "WHERE c.teacherId = ? " +
                "AND qr.createdAt <= ? " +
                "AND qr.expiresAt >= ? " +
                "AND qr.isActive = 1 " +
                "AND s.attendanceEnabled = 1 " +
                "AND s.startTime <= ? " +
                "AND s.endTime >= ? " +
                "ORDER BY s.startTime DESC " +
                "LIMIT 1";

        Cursor cursor = null;
        QRWithClassInfo qrWithClassInfo = null;

        try {
            Log.d("QRDebug", "Executing time-filtered query");

            cursor = db.rawQuery(query, new String[]{teacherId,
                    String.valueOf(currentTimeMillis), String.valueOf(currentTimeMillis),
                    String.valueOf(currentTimeMillis), String.valueOf(currentTimeMillis)});

            if (cursor != null && cursor.moveToFirst()) {
                byte[] qrBytes = cursor.getBlob(cursor.getColumnIndexOrThrow("qrCodeImage"));
                String className = cursor.getString(cursor.getColumnIndexOrThrow("className"));
                long sessionStartMillis = TimeUtils.parseTimestamp(cursor.getString(cursor.getColumnIndexOrThrow("startTime")));

                // Format session start time for logging
                String sessionStartFormatted = sdf.format(new Date(sessionStartMillis));

                Log.d("QRDebug", "Valid session found! Class: " + className +
                        ", Session Start: " + sessionStartFormatted +
                        " (" + sessionStartMillis + " ms)");

                // Decode the QR code and return the result
                qrWithClassInfo = new QRWithClassInfo(BitmapFactory.decodeByteArray(qrBytes, 0, qrBytes.length), className);
            } else {
                Log.d("QRDebug", "No active session found. QR expired or not generated.");
            }
        } catch (Exception e) {
            Log.e("QRDebug", "Error fetching valid QR code for teacher: ", e);
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }

        return qrWithClassInfo;
    }





    private long getStartOfTodayInMillis() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private long getEndOfTodayInMillis() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }



    public static void logAllQRCodeTimes(Context context) {
        // Create a local DBHelper instance
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT qrCodeId, createdAt, expiresAt FROM qr_codes", null);

        if (cursor != null && cursor.moveToFirst()) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

            do {
                String qrCodeId = cursor.getString(0);
                long createdAtMillis = cursor.getLong(1);
                long expiresAtMillis = cursor.getLong(2);

                String createdAtFormatted = sdf.format(new Date(createdAtMillis));
                String expiresAtFormatted = sdf.format(new Date(expiresAtMillis));

                Log.d("QRCodeTimes", "QR ID: " + qrCodeId +
                        " | Created At: " + createdAtFormatted +
                        " | Expires At: " + expiresAtFormatted);

            } while (cursor.moveToNext());

            cursor.close();
        } else {
            Log.d("QRCodeTimes", "No QR codes found in database.");
        }





        db.close();
    }



}
