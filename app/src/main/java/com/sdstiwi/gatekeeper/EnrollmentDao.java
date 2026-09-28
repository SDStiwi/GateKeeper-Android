package com.sdstiwi.gatekeeper;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access for the enrollments table: enrollment requests and their approval status.
 *
 * <p>Queries were moved here unchanged from {@link DatabaseHelper}, which still exposes them as
 * delegating methods so existing callers keep working. Connection handling stays in the helper.
 */
public class EnrollmentDao {
    private final SQLiteOpenHelper helper;
    private final NotificationDao notificationDao;

    public EnrollmentDao(SQLiteOpenHelper helper) {
        this.helper = helper;
        this.notificationDao = new NotificationDao(helper);
    }

    // Get all enrolled students for a class
    public Cursor getEnrolledStudents(String classId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        return db.rawQuery("SELECT * FROM users WHERE userId IN (SELECT userId FROM enrollment WHERE classId = ?)", new String[]{classId});
    }

     //enroll a student
    public boolean enrollStudent(String studentId, String classId, String teacherId) {
        SQLiteDatabase db = helper.getWritableDatabase();

        // Check if the student is already enrolled or has a pending request
        Cursor cursor = db.rawQuery("SELECT * FROM enrollments WHERE studentId = ? AND classId = ?",
                new String[]{studentId, classId});
        String studentEmail = null;
        String className = null;

 // Fetch student email
        Cursor cursor1 = db.rawQuery("SELECT email FROM users WHERE UID = ?", new String[]{studentId});
        if (cursor1.moveToFirst()) {
            studentEmail = cursor1.getString(0); // Get the first column (email)
        }
        cursor1.close(); // Always close the cursor

 // Fetch class name
        Cursor cursor2 = db.rawQuery("SELECT className FROM classes WHERE classId = ?", new String[]{classId});
        if (cursor2.moveToFirst()) {
            className = cursor2.getString(0); // Get the first column (className)
        }
        cursor2.close(); // Always close the cursor

        if (cursor.getCount() > 0) {
            cursor.close();
            return false;  // Student already enrolled or pending
        }
        cursor.close();

        // Insert enrollment request
        ContentValues values = new ContentValues();
        values.put("studentId", studentId);
        values.put("classId", classId);
        values.put("status", "pending");  // Enrollment starts as pending
        String type = "enrollement";
        long result = db.insertWithOnConflict(
                "enrollments", null, values, SQLiteDatabase.CONFLICT_IGNORE);

        if (result != -1) {
            // Create a NotificationModel object
            NotificationModel notification = new NotificationModel(
                    null, // notificationId is auto-generated
                    teacherId,
                    "New enrollment request for class : " + className +"From "+studentEmail,
                    false, // Unread notification
                    String.valueOf(System.currentTimeMillis()), // Timestamp as createdAt
                    classId,
                    studentId,
                    type
            );

            // Insert notification using the new method
            notificationDao.insertNotification(notification);

            db.close();
            return true;
        } else {
            db.close();
            return false;
        }
    }

    public String getEnrollmentId(String studentId, String classId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        String enrollmentId = null;

        Cursor cursor = db.rawQuery("SELECT enrollmentId FROM enrollments WHERE studentId = ? AND classId = ?",
                new String[]{studentId, classId});

        if (cursor.moveToFirst()) {
            enrollmentId = cursor.getString(0);
        }

        cursor.close();
        db.close();
        return enrollmentId;
    }

    public boolean updateEnrollmentStatus(String enrollmentId, String newStatus, String studentId, ClassModel classModel) {
        SQLiteDatabase db = helper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("status", newStatus);
        String type = "reply";
        int rowsAffected = db.update("enrollments", values, "enrollmentId=?", new String[]{enrollmentId});

        if (rowsAffected > 0) {

            return true;
        } else {
            db.close();
            return false;
        }
    }

    public List<Enrollement> getAllEnrollments() {
        List<Enrollement> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM enrollments", null);

        if (cursor.moveToFirst()) {
            do {
                String id = cursor.getString(cursor.getColumnIndexOrThrow("enrollmentId"));
                String studentId = cursor.getString(cursor.getColumnIndexOrThrow("studentId"));
                String classId = cursor.getString(cursor.getColumnIndexOrThrow("classId"));
                String status = cursor.getString(cursor.getColumnIndexOrThrow("status"));

                Enrollement enrollment = new Enrollement(id, studentId, classId, status);
                list.add(enrollment);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public void insertOrUpdateEnrollment(Enrollement model) {
        SQLiteDatabase db = helper.getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put("enrollmentId", model.getEnrollmentId());
            values.put("studentId", model.getStudentId());
            values.put("classId", model.getClassId());
            values.put("status", model.getStatus());

            // The logical identity is student + class, not enrollmentId.
            int updated = db.update(
                    "enrollments",
                    values,
                    "studentId = ? AND classId = ?",
                    new String[]{model.getStudentId(), model.getClassId()});

            if (updated == 0) {
                db.insertWithOnConflict(
                        "enrollments", null, values, SQLiteDatabase.CONFLICT_IGNORE);
            }
        } finally {
            db.close();
        }
    }

    /** Legacy Firebase sync adapters. Data comes from the local SQLite source of truth. */
    public List<Enrollement> getStudentEnrollments(String studentId) {
        List<Enrollement> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT enrollmentId, studentId, classId, status FROM enrollments WHERE studentId = ?",
                new String[]{studentId});
        try {
            while (cursor.moveToNext()) {
                list.add(new Enrollement(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3)));
            }
        } finally {
            cursor.close();
            db.close();
        }
        return list;
    }
}
