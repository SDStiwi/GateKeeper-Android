package com.sdstiwi.gatekeeper;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access for the users table: registration, login and profile lookups, plus the Firebase sync upsert.
 *
 * <p>Queries were moved here unchanged from {@link DatabaseHelper}, which still exposes them as
 * delegating methods so existing callers keep working. Connection handling stays in the helper.
 */
public class UserDao {
    private final SQLiteOpenHelper helper;

    public UserDao(SQLiteOpenHelper helper) {
        this.helper = helper;
    }

    // Insert or update user data in SQLite
    public boolean insertOrUpdateUser(String uid, String email, String password, String firstName, String lastName,
                                      String speciality, String role, String department, String approvalStatus, int isActive) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("uid", uid);
        values.put("email", email);
        values.put("password", password);
        values.put("firstName", firstName);
        values.put("lastName", lastName);
        values.put("speciality", speciality);
        values.put("role", role);
        values.put("department", department);
        values.put("approvalStatus", approvalStatus);
        values.put("isActive", isActive);

        // Use the current timestamp for createdAt and lastLogin
        values.put("createdAt", System.currentTimeMillis());  // Current time in milliseconds
        values.put("lastLogin", (String) null);  // Initially set to null

        Cursor existing = db.rawQuery("SELECT 1 FROM users WHERE LOWER(email) = LOWER(?) LIMIT 1", new String[]{email});
        boolean emailExists = existing.moveToFirst();
        existing.close();
        if (emailExists) return false;

        long result = db.insert("users", null, values);
        return result != -1;
    }

    // Update last login timestamp
    public boolean updateLastLogin(String uid) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();

        // Use current timestamp (milliseconds)
        values.put("lastLogin", System.currentTimeMillis());

        int rowsUpdated = db.update("users", values, "uid = ?", new String[]{uid});
        return rowsUpdated > 0;
    }

    // Retrieve user details by email
    public Users getUserByEmail(String email) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.rawQuery("SELECT * FROM users WHERE email = ?", new String[]{email});

            if (cursor != null && cursor.moveToFirst()) {
                Users user = new Users(
                        cursor.getString(cursor.getColumnIndexOrThrow("uid")),
                        cursor.getString(cursor.getColumnIndexOrThrow("email")),
                        cursor.getString(cursor.getColumnIndexOrThrow("firstName")),
                        cursor.getString(cursor.getColumnIndexOrThrow("lastName")),
                        cursor.getString(cursor.getColumnIndexOrThrow("password")),
                        cursor.getString(cursor.getColumnIndexOrThrow("role")),
                        cursor.getString(cursor.getColumnIndexOrThrow("speciality")),
                        cursor.getString(cursor.getColumnIndexOrThrow("approvalStatus")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("isActive")),
                        cursor.getString(cursor.getColumnIndexOrThrow("department")),
                        cursor.getString(cursor.getColumnIndexOrThrow("createdAt")),
                        cursor.getString(cursor.getColumnIndexOrThrow("lastLogin"))
                );
                return user;
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error retrieving user by email: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return null;  // Return null if no user is found or there is an error
    }

    // Retrieve user details by UID
    public Users getUserByUid(String uid) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.rawQuery("SELECT * FROM users WHERE uid = ?", new String[]{uid});

            if (cursor != null && cursor.moveToFirst()) {
                Users user = new Users(
                        cursor.getString(cursor.getColumnIndexOrThrow("uid")),
                        cursor.getString(cursor.getColumnIndexOrThrow("email")),
                        cursor.getString(cursor.getColumnIndexOrThrow("firstName")),
                        cursor.getString(cursor.getColumnIndexOrThrow("lastName")),
                        cursor.getString(cursor.getColumnIndexOrThrow("password")),
                        cursor.getString(cursor.getColumnIndexOrThrow("role")),
                        cursor.getString(cursor.getColumnIndexOrThrow("speciality")),
                        cursor.getString(cursor.getColumnIndexOrThrow("approvalStatus")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("isActive")),
                        cursor.getString(cursor.getColumnIndexOrThrow("department")),
                        cursor.getString(cursor.getColumnIndexOrThrow("createdAt")),
                        cursor.getString(cursor.getColumnIndexOrThrow("lastLogin"))
                );
                return user;
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error retrieving user by UID: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return null;  // Return null if no user is found or there is an error
    }

    public Users getUserProfile(String uid) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Users user = null;

        Cursor cursor = db.rawQuery("SELECT * FROM users WHERE uid = ?", new String[]{uid});
        if (cursor != null && cursor.moveToFirst()) {
            user = new Users(
                    cursor.getString(cursor.getColumnIndexOrThrow("uid")),
                    cursor.getString(cursor.getColumnIndexOrThrow("email")),
                    cursor.getString(cursor.getColumnIndexOrThrow("firstName")),
                    cursor.getString(cursor.getColumnIndexOrThrow("lastName")),
                    cursor.getString(cursor.getColumnIndexOrThrow("password")),
                    cursor.getString(cursor.getColumnIndexOrThrow("role")),
                    cursor.getString(cursor.getColumnIndexOrThrow("speciality")),
                    cursor.getString(cursor.getColumnIndexOrThrow("approvalStatus")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("isActive")),
                    cursor.getString(cursor.getColumnIndexOrThrow("department")),
                    cursor.getString(cursor.getColumnIndexOrThrow("createdAt")),
                    cursor.getString(cursor.getColumnIndexOrThrow("lastLogin"))
            );
            cursor.close();
        }

        return user;
    }

    public List<Users> getAllUsers() {
        List<Users> userList = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM users", null);

        if (cursor.moveToFirst()) {
            do {
                Users user = new Users();
                user.setUid(cursor.getString(cursor.getColumnIndexOrThrow("uid")));
                user.setEmail(cursor.getString(cursor.getColumnIndexOrThrow("email")));
                user.setFirstName(cursor.getString(cursor.getColumnIndexOrThrow("firstName")));
                user.setLastName(cursor.getString(cursor.getColumnIndexOrThrow("lastName")));
                user.setRole(cursor.getString(cursor.getColumnIndexOrThrow("role")));
                user.setSpeciality(cursor.getString(cursor.getColumnIndexOrThrow("speciality")));
                user.setApprovalStatus(cursor.getString(cursor.getColumnIndexOrThrow("approvalStatus")));
                user.setIsActive(cursor.getInt(cursor.getColumnIndexOrThrow("isActive")));
                user.setDepartment(cursor.getString(cursor.getColumnIndexOrThrow("department")));
                user.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow("createdAt")));
                user.setLastLogin(cursor.getString(cursor.getColumnIndexOrThrow("lastLogin")));

                userList.add(user);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return userList;
    }

    public void insertOrUpdateUser1(Users user) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("uid", user.getUid());
        values.put("email", user.getEmail());
        values.put("password", user.getPassword()); // Might be a placeholder
        values.put("firstName", user.getFirstName());
        values.put("lastName", user.getLastName());
        values.put("speciality", user.getSpeciality());
        values.put("role", user.getRole());
        values.put("department", user.getDepartment());
        values.put("isActive", user.getIsActive());
        values.put("approvalStatus", user.getApprovalStatus());
        values.put("createdAt", user.getCreatedAt());
        values.put("lastLogin", user.getLastLogin());

        long rowId = db.insertWithOnConflict("users", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        Log.d("DatabaseHelper", "Inserted/Updated row ID: " + rowId);
    }
}
