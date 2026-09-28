package com.sdstiwi.gatekeeper;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access for the notifications table.
 *
 * <p>Queries were moved here unchanged from {@link DatabaseHelper}, which still exposes them as
 * delegating methods so existing callers keep working. Connection handling stays in the helper.
 */
public class NotificationDao {
    private final SQLiteOpenHelper helper;

    public NotificationDao(SQLiteOpenHelper helper) {
        this.helper = helper;
    }

    public boolean isNotificationAlreadySent(String receiverId, String classId, String status) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM notifications WHERE recieverId = ? AND classId = ? AND message LIKE ?",
                new String[]{receiverId, classId, "%was " + status + "%"});

        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    // Insert notification using NotificationModel
    public void insertNotification(NotificationModel notification) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("recieverId", notification.getReceiverId());
        values.put("senderId", notification.getSender());
        values.put("message", notification.getMessage());
        values.put("isRead", notification.isRead() ? 1 : 0);
        values.put("createdAt", notification.getCreatedAt());
        values.put("classId", notification.getClassId());
        values.put("notificationType", notification.getNotificationType());  // Add this

        db.insert("notifications", null, values);
        db.close();
    }

    // Fetch notifications as List<NotificationModel>
    public List<NotificationModel> getUserNotifications(String receiverId) {
        List<NotificationModel> notifications = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT notificationId, recieverId, senderId, message, isRead, createdAt, classId, notificationType FROM notifications WHERE recieverId = ?", new String[]{receiverId});

        if (cursor.moveToFirst()) {
            do {
                NotificationModel notification = new NotificationModel(
                        cursor.getString(0), // notificationId
                        cursor.getString(1), // receiverId
                        cursor.getString(3), // message
                        cursor.getInt(4) == 1, // isRead (convert int to boolean)
                        cursor.getString(5), // createdAt
                        cursor.getString(6), // classId
                        cursor.getString(2), // senderId
                        cursor.getString(7)  // notificationType
                );
                notifications.add(notification);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return notifications;
    }

    // Mark notifications as read
    // Mark a single notification as read
    public void markNotificationAsRead(String notificationId) {
        SQLiteDatabase db = helper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("isRead", 1); // Mark as read

        db.update("notifications", values, "notificationId=?", new String[]{notificationId});
        db.close();
    }

    // Mark all notifications as read for a user
    public void markNotificationsAsReadAll(String userId) {
        if (userId == null || userId.isEmpty()) return; // Prevent empty queries

        SQLiteDatabase db = helper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("isRead", 1); // Mark all as read

        db.update("notifications", values, "recieverId=?", new String[]{userId});
        db.close();
    }

    public Cursor getLatestNotifications(String studentId, int limit) {
        SQLiteDatabase db = helper.getReadableDatabase();

        String query = "SELECT notificationId, recieverId, message, createdAt, isRead, classId, senderId, notificationType " +
                "FROM notifications " +
                "WHERE recieverId = ? " +
                "ORDER BY createdAt DESC " +
                "LIMIT ?";

        Log.d("DatabaseQuery", "Executing query: " + query);
        Log.d("DatabaseQuery", "With parameters: studentId = " + studentId + ", limit = " + limit);

        Cursor cursor = db.rawQuery(query, new String[]{studentId, String.valueOf(limit)});

        if (cursor != null) {
            Log.d("DatabaseQuery", "Rows fetched: " + cursor.getCount());
        } else {
            Log.d("DatabaseQuery", "Cursor is null");
        }

        return cursor;
    }

    public List<NotificationModel> getAllNotifications() {
        List<NotificationModel> notifications = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM notifications", null);

        if (cursor.moveToFirst()) {
            do {
                NotificationModel notification = new NotificationModel();
                notification.setNotificationId(cursor.getString(cursor.getColumnIndexOrThrow("notificationId")));
                notification.setRecieverId(cursor.getString(cursor.getColumnIndexOrThrow("recieverId")));
                notification.setSenderId(cursor.getString(cursor.getColumnIndexOrThrow("senderId")));
                notification.setMessage(cursor.getString(cursor.getColumnIndexOrThrow("message")));
                notification.setRead(cursor.getInt(cursor.getColumnIndexOrThrow("isRead")) == 1);
                notification.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow("createdAt")));
                notification.setClassId(cursor.getString(cursor.getColumnIndexOrThrow("classId")));
                notification.setNotificationType(cursor.getString(cursor.getColumnIndexOrThrow("notificationType")));

                notifications.add(notification);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return notifications;
    }

    public void insertOrUpdateNotification(NotificationModel notification) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("notificationId", notification.getNotificationId());
        values.put("recieverId", notification.getReceiverId());
        values.put("senderId", notification.getSender());
        values.put("message", notification.getMessage());
        values.put("isRead", notification.isRead() ? 1 : 0);
        values.put("createdAt", notification.getCreatedAt());
        values.put("classId", notification.getClassId());
        values.put("notificationType", notification.getNotificationType());

        db.insertWithOnConflict("notifications", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }
}
