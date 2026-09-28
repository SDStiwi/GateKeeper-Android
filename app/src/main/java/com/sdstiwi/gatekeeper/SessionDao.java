package com.sdstiwi.gatekeeper;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.icu.text.SimpleDateFormat;
import android.util.Log;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Data access for the class schedules and timetables for teachers and students (derived from classes, enrollments and rooms).
 *
 * <p>Queries were moved here unchanged from {@link DatabaseHelper}, which still exposes them as
 * delegating methods so existing callers keep working. Connection handling stays in the helper.
 */
public class SessionDao {
    private final SQLiteOpenHelper helper;

    public SessionDao(SQLiteOpenHelper helper) {
        this.helper = helper;
    }

    //today schedule teacher
    public List<Session> getTodaysScheduleForTeacher(String teacherId) {
        List<Session> todaysSchedule = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = null;

        try {
            String todayDay = new SimpleDateFormat("EEE", Locale.ENGLISH).format(new java.util.Date()
); // e.g., "Mon"
            String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new java.util.Date()
            );

            Log.d("ScheduleDebug", "Fetching schedule for teacher ID: " + teacherId);
            Log.d("ScheduleDebug", "Today is: " + todayDay + " (" + todayDate + ")");

            String query = "SELECT classId, className, schedule, IFNULL(r.name, 'N/A') AS roomName " +
                    "FROM classes c " +
                    "LEFT JOIN rooms r ON c.roomId = r.roomId " +
                    "WHERE c.teacherId = ?";

            cursor = db.rawQuery(query, new String[]{teacherId});
            Log.d("ScheduleDebug", "Query executed. Rows found: " + cursor.getCount());

            if (cursor.moveToFirst()) {
                do {
                    String classId = cursor.getString(cursor.getColumnIndex("classId"));
                    String className = cursor.getString(cursor.getColumnIndex("className"));
                    String schedule = cursor.getString(cursor.getColumnIndex("schedule"));
                    String roomName = cursor.getString(cursor.getColumnIndex("roomName"));

                    Log.d("ScheduleDebug", "Class found: " + className + " | Schedule: " + schedule + " | Room: " + roomName);

                    if (schedule == null || schedule.trim().isEmpty()) {
                        Log.w("ScheduleDebug", "Schedule is empty for class: " + className);
                        continue;
                    }

                    String[] dayTimeParts = schedule.split(" ");
                    if (dayTimeParts.length != 2) {
                        Log.w("ScheduleDebug", "Invalid schedule format for class: " + className);
                        continue;
                    }

                    String[] days = dayTimeParts[0].split(",");
                    String timeRange = dayTimeParts[1];

                    Log.d("ScheduleDebug", "Days: " + Arrays.toString(days) + " | Time Range: " + timeRange);

                    for (String day : days) {
                        if (day.equalsIgnoreCase(todayDay)) {
                            String[] times = timeRange.split("-");
                            if (times.length == 2) {
                                String startTime = times[0];
                                String endTime = times[1];

                                Log.d("ScheduleDebug", "Match found for today! Class: " + className +
                                        " | Time: " + startTime + " - " + endTime);

                                todaysSchedule.add(new Session(
                                        null, // No sessionId from this method
                                        classId,
                                        todayDate,
                                        startTime,
                                        endTime,
                                        0, // Default attendanceEnabled
                                        className,
                                        roomName
                                ));
                            } else {
                                Log.w("ScheduleDebug", "Invalid time range in schedule: " + timeRange);
                            }
                            break; // Stop checking other days for this class
                        }
                    }

                } while (cursor.moveToNext());
            } else {
                Log.d("ScheduleDebug", "No classes found for teacher.");
            }

        } catch (Exception e) {
            Log.e("ScheduleDebug", "Error parsing today's schedule: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }

        Log.d("ScheduleDebug", "Total sessions added for today: " + todaysSchedule.size());
        return todaysSchedule;
    }

    //weekly timeTable for teacher
    public List<Session> getWeeklyTimetableForTeacher(String teacherId) {
        List<Session> weeklyTimetable = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = null;

        try {
            // Query to fetch the teacher's classes with their weekly schedule
            String query = "SELECT c.classId, c.className, c.schedule, r.name AS roomName " +
                    "FROM classes c " +
                    "JOIN rooms r ON c.roomId = r.roomId " +
                    "WHERE c.teacherId = ?";

            cursor = db.rawQuery(query, new String[]{teacherId});

            if (cursor.moveToFirst()) {
                do {
                    // Get column indices safely
                    int classIdIndex = cursor.getColumnIndex("classId");
                    int classNameIndex = cursor.getColumnIndex("className");
                    int scheduleIndex = cursor.getColumnIndex("schedule");
                    int roomNameIndex = cursor.getColumnIndex("roomName");

                    // Ensure valid column indices
                    if (classIdIndex == -1 || classNameIndex == -1 || scheduleIndex == -1 || roomNameIndex == -1) {
                        Log.e("DatabaseHelper", "Column index error in getWeeklyTimetableForTeacher()");
                        continue;
                    }

                    // Retrieve data
                    String classId = cursor.getString(classIdIndex);
                    String className = cursor.getString(classNameIndex);
                    String schedule = cursor.getString(scheduleIndex);  // schedule format: "Tue,Thu 09:00-10:30"
                    String roomName = cursor.getString(roomNameIndex);

                    Log.d("DatabaseHelper", "Class Found - ID: " + classId + ", Name: " + className +
                            ", Schedule: " + schedule + ", Room: " + roomName);

                    // Split the schedule string into days and times (e.g., "Tue,Thu 09:00-10:30")
                    String[] scheduleParts = schedule.split(" ");
                    if (scheduleParts.length == 2) {
                        String[] days = scheduleParts[0].split(","); // "Tue,Thu" -> ["Tue", "Thu"]
                        String timeRange = scheduleParts[1]; // "09:00-10:30"

                        // Split the time range into start and end times
                        String[] times = timeRange.split("-");
                        if (times.length == 2) {
                            String startTime = times[0];
                            String endTime = times[1];

                            // Create a session for each day in the schedule
                            for (String day : days) {
                                // Create a new session for each day
                                Session newSession = new Session(
                                        classId,
                                        classId,  // Assuming classId is the identifier for the session as well
                                        day,
                                        startTime,
                                        endTime,
                                        1,  // Assuming attendanceEnabled is always 1 for the retrieved sessions
                                        className,
                                        roomName
                                );

                                weeklyTimetable.add(newSession);
                            }
                        }
                    }

                } while (cursor.moveToNext());
            } else {
                Log.d("DatabaseHelper", "No classes found for this teacher.");
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error retrieving weekly timetable: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
            db.close(); // Ensure database is closed
        }

        return weeklyTimetable;
    }

    public List<Session> getStudentClassesForToday(String studentId) {
        List<Session> todaysSchedule = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = null;

        try {
            String todayDay = new SimpleDateFormat("EEE", Locale.ENGLISH).format(new java.util.Date()); // e.g., "Mon"
            String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new java.util.Date());

            Log.d("StudentSchedule", "Fetching schedule for student ID: " + studentId);
            Log.d("StudentSchedule", "Today is: " + todayDay + " (" + todayDate + ")");

            String query = "SELECT c.classId, c.className, c.schedule, IFNULL(r.name, 'N/A') AS roomName " +
                    "FROM classes c " +
                    "JOIN enrollments e ON c.classId = e.classId " +
                    "LEFT JOIN rooms r ON c.roomId = r.roomId " +
                    "WHERE e.studentId = ? " +
                    "AND e.status = 'approved' " +
                    "AND c.isActive = 1";

            cursor = db.rawQuery(query, new String[]{studentId});
            Log.d("StudentSchedule", "Query executed. Rows found: " + cursor.getCount());

            if (cursor.moveToFirst()) {
                do {
                    String classId = cursor.getString(cursor.getColumnIndex("classId"));
                    String className = cursor.getString(cursor.getColumnIndex("className"));
                    String schedule = cursor.getString(cursor.getColumnIndex("schedule"));
                    String roomName = cursor.getString(cursor.getColumnIndex("roomName"));

                    if (schedule == null || schedule.trim().isEmpty()) {
                        Log.w("StudentSchedule", "Empty schedule for class: " + className);
                        continue;
                    }

                    String[] dayTimeParts = schedule.split(" ");
                    if (dayTimeParts.length != 2) {
                        Log.w("StudentSchedule", "Invalid schedule format for class: " + className);
                        continue;
                    }

                    String[] days = dayTimeParts[0].split(",");
                    String timeRange = dayTimeParts[1];

                    for (String day : days) {
                        if (day.equalsIgnoreCase(todayDay)) {
                            String[] times = timeRange.split("-");
                            if (times.length == 2) {
                                String startTime = times[0];
                                String endTime = times[1];

                                Log.d("StudentSchedule", "Match for today: " + className +
                                        " | Time: " + startTime + " - " + endTime);

                                todaysSchedule.add(new Session(
                                        null, // no sessionId
                                        classId,
                                        todayDate,
                                        startTime,
                                        endTime,
                                        0, // attendanceEnabled
                                        className,
                                        roomName
                                ));
                            } else {
                                Log.w("StudentSchedule", "Invalid time range: " + timeRange);
                            }
                            break;
                        }
                    }

                } while (cursor.moveToNext());
            }

        } catch (Exception e) {
            Log.e("StudentSchedule", "Error: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }

        Log.d("StudentSchedule", "Total sessions for today: " + todaysSchedule.size());
        return todaysSchedule;
    }

    //student weekly time table
    public List<Session> getWeeklyScheduleForStudent(String studentId) {
        List<Session> weeklySchedule = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = null;

        try {
            // Fetch all approved classes the student is enrolled in
            String query = "SELECT c.classId, c.className, c.schedule, r.name AS roomName " +
                    "FROM classes c " +
                    "JOIN enrollments e ON c.classId = e.classId " +
                    "LEFT JOIN rooms r ON c.roomId = r.roomId " +
                    "WHERE e.studentId = ? AND e.status = 'approved' AND c.isActive = 1";

            cursor = db.rawQuery(query, new String[]{studentId});

            if (cursor.moveToFirst()) {
                do {
                    String classId = cursor.getString(cursor.getColumnIndex("classId"));
                    String className = cursor.getString(cursor.getColumnIndex("className"));
                    String schedule = cursor.getString(cursor.getColumnIndex("schedule"));
                    String roomName = cursor.getString(cursor.getColumnIndex("roomName"));

                    if (schedule == null || schedule.trim().isEmpty()) continue;

                    String[] parts = schedule.split(" ");
                    if (parts.length != 2) continue;

                    String[] days = parts[0].split(",");
                    String[] times = parts[1].split("-");
                    if (times.length != 2) continue;

                    String startTime = times[0];
                    String endTime = times[1];

                    for (String day : days) {
                        weeklySchedule.add(new Session(
                                null, // sessionId not used here
                                classId,
                                day, // Use the weekday name as "date"
                                startTime,
                                endTime,
                                1, // attendanceEnabled
                                className,
                                roomName != null ? roomName : "N/A"
                        ));
                    }

                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error in getWeeklyScheduleForStudent: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }

        return weeklySchedule;
    }
}
