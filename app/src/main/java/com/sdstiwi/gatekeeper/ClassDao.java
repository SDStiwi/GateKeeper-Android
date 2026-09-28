package com.sdstiwi.gatekeeper;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access for the classes table.
 *
 * <p>Queries were moved here unchanged from {@link DatabaseHelper}, which still exposes them as
 * delegating methods so existing callers keep working. Connection handling stays in the helper.
 */
public class ClassDao {
    private final SQLiteOpenHelper helper;

    public ClassDao(SQLiteOpenHelper helper) {
        this.helper = helper;
    }

    // Get all classes
    public Cursor getAllClasses() {
        SQLiteDatabase db = helper.getReadableDatabase();
        return db.rawQuery("SELECT * FROM classes", null);
    }

    public ClassModel getClassById(String classId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        ClassModel classModel = null;

        Cursor cursor = db.rawQuery("SELECT * FROM classes WHERE classId = ?", new String[]{classId});

        if (cursor.moveToFirst()) {
            String className = cursor.getString(cursor.getColumnIndexOrThrow("className"));
            String courseCode = cursor.getString(cursor.getColumnIndexOrThrow("courseCode"));
            String roomId = cursor.getString(cursor.getColumnIndexOrThrow("roomId"));
            String teacherId = cursor.getString(cursor.getColumnIndexOrThrow("teacherId"));

            classModel = new ClassModel(classId, className, courseCode, roomId, teacherId);
        }

        cursor.close();
        db.close();
        return classModel;
    }

    public List<Classes> getAllClasses1() {
        List<Classes> classList = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM classes", null);

        if (cursor.moveToFirst()) {
            do {
                Classes cls = new Classes();
                cls.setClassId(cursor.getString(cursor.getColumnIndexOrThrow("classId")));
                cls.setClassName(cursor.getString(cursor.getColumnIndexOrThrow("className")));
                cls.setCourseCode(cursor.getString(cursor.getColumnIndexOrThrow("courseCode")));
                cls.setStartDate(cursor.getString(cursor.getColumnIndexOrThrow("startDate")));
                cls.setEndDate(cursor.getString(cursor.getColumnIndexOrThrow("endDate")));
                cls.setIsActive(cursor.getInt(cursor.getColumnIndexOrThrow("isActive")));
                cls.setRoomId(cursor.getString(cursor.getColumnIndexOrThrow("roomId")));
                cls.setTeacherId(cursor.getString(cursor.getColumnIndexOrThrow("teacherId")));
                cls.setSchedule(cursor.getString(cursor.getColumnIndexOrThrow("schedule")));

                classList.add(cls);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return classList;
    }

    public void insertOrUpdateClass(Classes cls) {
        SQLiteDatabase db = helper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("classId", cls.getClassId());
        values.put("className", cls.getClassName());
        values.put("courseCode", cls.getCourseCode());
        values.put("startDate", cls.getStartDate());
        values.put("endDate", cls.getEndDate());
        values.put("isActive", cls.getIsActive());
        values.put("roomId", cls.getRoomId());
        values.put("teacherId", cls.getTeacherId());
        values.put("schedule", cls.getSchedule());

        db.insertWithOnConflict("classes", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    public List<ClassItem> getTeacherClasses(String teacherId) {
        List<ClassItem> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT classId, className, teacherId, schedule, roomId FROM classes WHERE teacherId = ? AND isActive = 1",
                new String[]{teacherId});
        try {
            while (cursor.moveToNext()) {
                list.add(new ClassItem(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4)));
            }
        } finally {
            cursor.close();
            db.close();
        }
        return list;
    }
}
