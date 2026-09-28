package com.sdstiwi.gatekeeper;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.icu.text.SimpleDateFormat;
import android.util.Log;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Data access for the rooms table and the room-status refresh.
 *
 * <p>Queries were moved here unchanged from {@link DatabaseHelper}, which still exposes them as
 * delegating methods so existing callers keep working. Connection handling stays in the helper.
 */
public class RoomDao {
    private final SQLiteOpenHelper helper;

    public RoomDao(SQLiteOpenHelper helper) {
        this.helper = helper;
    }

    //rooms to teach in today
    public List<Room> getRoomsToTeachInToday(String teacherId) {
        List<Room> rooms = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = null;
        Set<String> uniqueRoomIds = new HashSet<>();

        try {
            String todayDay = new SimpleDateFormat("EEE", Locale.ENGLISH).format(Calendar.getInstance().getTime()); // e.g., "Mon"
            Log.d("RoomDebug", "Today is: " + todayDay);

            String query = "SELECT DISTINCT r.roomId, r.name, r.building, r.floor, r.deviceId, r.isActive, c.schedule " +
                    "FROM rooms r " +
                    "JOIN classes c ON r.roomId = c.roomId " +
                    "WHERE c.teacherId = ?";

            cursor = db.rawQuery(query, new String[]{teacherId});
            Log.d("RoomDebug", "Query executed. Rows: " + cursor.getCount());

            if (cursor.moveToFirst()) {
                do {
                    String schedule = cursor.getString(cursor.getColumnIndexOrThrow("schedule"));
                    Log.d("RoomDebug", "Schedule: " + schedule);

                    if (schedule == null || schedule.trim().isEmpty()) continue;

                    String[] parts = schedule.split(" ");
                    if (parts.length != 2) continue;

                    String[] days = parts[0].split(",");

                    for (String day : days) {
                        if (day.equalsIgnoreCase(todayDay)) {
                            // Room details
                            String roomId = cursor.getString(cursor.getColumnIndexOrThrow("roomId"));
                            String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                            String building = cursor.getString(cursor.getColumnIndexOrThrow("building"));
                            String floor = cursor.getString(cursor.getColumnIndexOrThrow("floor"));
                            String deviceId = cursor.getString(cursor.getColumnIndexOrThrow("deviceId"));
                            int isActive = cursor.getInt(cursor.getColumnIndexOrThrow("isActive"));

                            Log.d("RoomDebug", "Match found - Room: " + name + " | Schedule: " + schedule);

                            if (!uniqueRoomIds.contains(roomId)) {
                                uniqueRoomIds.add(roomId);
                                rooms.add(new Room(roomId, name, building, floor, deviceId, isActive));
                            }
                            break; // No need to check more days for this row
                        }
                    }

                } while (cursor.moveToNext());
            } else {
                Log.d("RoomDebug", "No rooms/classes found.");
            }
        } catch (Exception e) {
            Log.e("RoomDebug", "Error retrieving rooms for today: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }

        Log.d("RoomDebug", "Total rooms to teach in today: " + rooms.size());
        return rooms;
    }

    //rooms students
    public List<Room> getRoomsToTakeToday(String studentId) {
        List<Room> rooms = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = null;
        Set<String> uniqueRoomIds = new HashSet<>();

        try {
            // Get today's day abbreviation (e.g., Mon, Tue)
            String todayDay = new SimpleDateFormat("EEE", Locale.ENGLISH).format(Calendar.getInstance().getTime());
            Log.d("ClassDebug", "Today is: " + todayDay);

            // SQL query using enrollments table and filtering by approved status
            String query = "SELECT DISTINCT r.roomId, r.name, r.building, r.floor, r.deviceId, r.isActive, c.schedule " +
                    "FROM enrollments e " +
                    "JOIN classes c ON e.classId = c.classId " +
                    "JOIN rooms r ON c.roomId = r.roomId " +
                    "WHERE e.studentId = ? AND LOWER(e.status) = 'approved' " +
                    "AND c.isActive = 1";

            cursor = db.rawQuery(query, new String[]{studentId});
            Log.d("ClassDebug", "Query executed. Rows: " + cursor.getCount());

            if (cursor.moveToFirst()) {
                do {
                    String schedule = cursor.getString(cursor.getColumnIndexOrThrow("schedule"));
                    Log.d("ClassDebug", "Schedule: " + schedule);

                    if (schedule == null || schedule.trim().isEmpty()) continue;

                    String[] parts = schedule.split(" ");
                    if (parts.length < 1) continue;

                    String[] days = parts[0].split(",");

                    for (String day : days) {
                        if (day.trim().equalsIgnoreCase(todayDay)) {
                            // Room details
                            String roomId = cursor.getString(cursor.getColumnIndexOrThrow("roomId"));
                            String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                            String building = cursor.getString(cursor.getColumnIndexOrThrow("building"));
                            String floor = cursor.getString(cursor.getColumnIndexOrThrow("floor"));
                            String deviceId = cursor.getString(cursor.getColumnIndexOrThrow("deviceId"));
                            int isActive = cursor.getInt(cursor.getColumnIndexOrThrow("isActive"));

                            Log.d("ClassDebug", "Matched Room: " + name);

                            if (!uniqueRoomIds.contains(roomId)) {
                                uniqueRoomIds.add(roomId);
                                rooms.add(new Room(roomId, name, building, floor, deviceId, isActive));
                            }
                            break;
                        }
                    }

                } while (cursor.moveToNext());
            } else {
                Log.d("ClassDebug", "No approved rooms/classes found for today.");
            }
        } catch (Exception e) {
            Log.e("ClassDebug", "Error retrieving rooms: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }

        Log.d("ClassDebug", "Total approved rooms for today: " + rooms.size());
        return rooms;
    }

    public List<Room> getAllRooms() {
        List<Room> rooms = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM rooms", null);
        if (cursor.moveToFirst()) {
            do {
                Room room = new Room();
                room.setRoomId(cursor.getString(cursor.getColumnIndexOrThrow("roomId")));
                room.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
                room.setBuilding(cursor.getString(cursor.getColumnIndexOrThrow("building")));
                room.setFloor(cursor.getString(cursor.getColumnIndexOrThrow("floor")));
                room.setDeviceId(cursor.getString(cursor.getColumnIndexOrThrow("deviceId")));
                room.setIsActive(cursor.getInt(cursor.getColumnIndexOrThrow("isActive")));

                rooms.add(room);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return rooms;
    }

    public void insertOrUpdateRoom(Room room) {
        SQLiteDatabase db = helper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("roomId", room.getRoomId());
        values.put("name", room.getName());
        values.put("building", room.getBuilding());
        values.put("floor", room.getFloor());
        values.put("deviceId", room.getDeviceId());
        values.put("isActive", room.getIsActive());

        // Check if the room already exists in the database
        Cursor cursor = db.query("rooms", null, "roomId = ?", new String[]{room.getRoomId()},
                null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            // Room exists, so update the existing record
            db.update("rooms", values, "roomId = ?", new String[]{room.getRoomId()});
            Log.d("DatabaseHelper", "Updated room: " + room.getRoomId());
        } else {
            // Room does not exist, insert a new record
            db.insert("rooms", null, values);
            Log.d("DatabaseHelper", "Inserted room: " + room.getRoomId());
        }

        if (cursor != null) {
            cursor.close();
        }

    }

    public void updateRoomStatuses(SQLiteDatabase db) {
        // Demo mode keeps a stable portfolio state so the UI can showcase both
        // connected and closed controllers regardless of the real clock.
        if (DemoConfig.DEMO_MODE) {
            db.execSQL("UPDATE rooms SET isActive = CASE WHEN roomId IN ('R1','R2') THEN 1 ELSE 0 END");
            return;
        }

        // Get current time
        String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(Calendar.getInstance().getTime());

        Log.d("RoomStatusUpdate", "Starting room status update at: " + currentTime);

        // Step 1: Set all to closed
        try {
            db.execSQL("UPDATE rooms SET isActive = 0");
            Log.d("RoomStatusUpdate", "All rooms set to CLOSED.");
        } catch (Exception e) {
            Log.e("RoomStatusUpdate", "Error setting rooms to closed: " + e.getMessage());
        }

        // Step 2: Activate only rooms with current sessions
        try {
            db.execSQL(
                    "UPDATE rooms SET isActive = 1 WHERE roomId IN (" +
                            "   SELECT c.roomId FROM sessions s " +
                            "   JOIN classes c ON s.classId = c.classId " +
                            "   WHERE strftime('%H:%M', 'now', 'localtime')  BETWEEN s.startTime AND s.endTime" +
                            ")"
            );
            Log.d("RoomStatusUpdate", "Rooms with active sessions set to AUTHORIZED.");
        } catch (Exception e) {
            Log.e("RoomStatusUpdate", "Error updating active rooms: " + e.getMessage());
        }
        logSQLiteNow(db);
        Log.d("RoomStatusUpdate", "Room status update completed at: " + currentTime);
    }

    public void logSQLiteNow(SQLiteDatabase db) {
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT strftime('%H:%M', 'now', 'localtime') ", null);
            if (cursor.moveToFirst()) {
                String sqliteNow = cursor.getString(0);
                Log.d("RoomStatusUpdate", "SQLite datetime('now'): " + sqliteNow);
            }
        } catch (Exception e) {
            Log.e("RoomStatusUpdate", "Error fetching datetime('now'): " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
