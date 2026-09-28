package com.sdstiwi.gatekeeper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.icu.text.SimpleDateFormat;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import com.google.firebase.Timestamp;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SyncHelper {

    private static final String TAG = "SyncHelper";

    public static void syncAllUsersToFirestore(DatabaseHelper dbHelper) {
        List<Users> userList = dbHelper.getAllUsers();  // You must have this method

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        for (Users user : userList) {
            HashMap<String, Object> userMap = new HashMap<>();
            userMap.put("uid", user.getUid());
            userMap.put("email", user.getEmail());
            userMap.put("firstName", user.getFirstName());
            userMap.put("lastName", user.getLastName());
            userMap.put("role", user.getRole());
            userMap.put("speciality", user.getSpeciality());
            userMap.put("approvalStatus", user.getApprovalStatus());
            userMap.put("isActive", user.getIsActive());
            userMap.put("department", user.getDepartment());
            userMap.put("createdAt", user.getCreatedAt());
            userMap.put("lastLogin", user.getLastLogin());

            firestore.collection("users").document(user.getUid())
                    .set(userMap)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Synced: " + user.getEmail()))
                    .addOnFailureListener(e -> Log.e(TAG, "Sync failed: " + user.getEmail(), e));
        }
    }

    public static void syncUserToRealtimeDB(Users user) {
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        DatabaseReference usersRef = firebaseDatabase.getReference("users");

        HashMap<String, Object> userMap = new HashMap<>();
        userMap.put("uid", user.getUid());
        userMap.put("email", user.getEmail());
        userMap.put("firstName", user.getFirstName());
        userMap.put("lastName", user.getLastName());
        userMap.put("role", user.getRole());
        userMap.put("speciality", user.getSpeciality());
        userMap.put("approvalStatus", user.getApprovalStatus());
        userMap.put("isActive", user.getIsActive());
        userMap.put("department", user.getDepartment());
        userMap.put("createdAt", user.getCreatedAt());
        userMap.put("lastLogin", System.currentTimeMillis()); // Optional

        usersRef.child(user.getUid()).setValue(userMap)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "RTDB Sync Success: " + user.getEmail()))
                .addOnFailureListener(e -> Log.e(TAG, "RTDB Sync Failed: " + user.getEmail(), e));
    }


    // Firebase Authentication Sync Only
    public static void syncUserToFirebaseAuth(Users user) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        mAuth.fetchSignInMethodsForEmail(user.getEmail()).addOnCompleteListener(task -> {
            boolean userExists = task.getResult() != null &&
                    task.getResult().getSignInMethods() != null &&
                    !task.getResult().getSignInMethods().isEmpty();

            if (!userExists) {
                mAuth.createUserWithEmailAndPassword(user.getEmail(), user.getPassword())
                        .addOnSuccessListener(authResult -> {
                            FirebaseUser firebaseUser = authResult.getUser();
                            Log.d(TAG, "Auth Created: " + user.getEmail());
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Auth Creation Failed: " + user.getEmail(), e);
                        });
            } else {
                Log.d(TAG, "Auth Exists: " + user.getEmail());
            }
        });
    }


    // Sync Firestore users into SQLite
    public static void syncUsersFromFirestoreToLocal(DatabaseHelper dbHelper) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        CollectionReference usersRef = firestore.collection("users");

        usersRef.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Users user = new Users();
                        user.setUid(doc.getString("uid"));
                        user.setEmail(doc.getString("email"));
                        user.setFirstName(doc.getString("firstName"));
                        user.setLastName(doc.getString("lastName"));
                        user.setRole(doc.getString("role"));
                        user.setSpeciality(doc.getString("speciality"));
                        user.setApprovalStatus(doc.getString("approvalStatus"));
                        user.setIsActive(doc.getLong("isActive") != null ? doc.getLong("isActive").intValue() : 0);
                        user.setDepartment(doc.getString("department"));
                        user.setCreatedAt(doc.getString("createdAt"));
                        user.setLastLogin(doc.getString("lastLogin"));
                        user.setPassword("default"); // If password isn't stored, use placeholder

                        dbHelper.insertOrUpdateUser1(user);
                        Log.d("SyncHelper", "User synced: " + user.getEmail());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("SyncHelper", "Failed to sync users from Firestore", e);
                });
    }




    public static void syncUsersFromRealtimeDbToLocal(DatabaseHelper dbHelper) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("users");

        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    Users user = new Users();
                    user.setUid(userSnap.child("uid").getValue(String.class));
                    user.setEmail(userSnap.child("email").getValue(String.class));
                    user.setFirstName(userSnap.child("firstName").getValue(String.class));
                    user.setLastName(userSnap.child("lastName").getValue(String.class));
                    user.setRole(userSnap.child("role").getValue(String.class));
                    user.setSpeciality(userSnap.child("speciality").getValue(String.class));
                    user.setApprovalStatus(userSnap.child("approvalStatus").getValue(String.class));
                    Long isActiveLong = userSnap.child("isActive").getValue(Long.class);
                    user.setIsActive(isActiveLong != null ? isActiveLong.intValue() : 0);
                    user.setDepartment(userSnap.child("department").getValue(String.class));
                    user.setCreatedAt(userSnap.child("createdAt").getValue(String.class));
                    Object lastLoginObj = userSnap.child("lastLogin").getValue();
                    String lastLoginStr = "";

                    if (lastLoginObj != null) {
                        lastLoginStr = String.valueOf(lastLoginObj); // converts int, Long, etc. to String
                    }

                    user.setLastLogin(lastLoginStr);
                    user.setPassword("default"); // RealtimeDB shouldn't store passwords

                    dbHelper.insertOrUpdateUser1(user); // Your SQLite insert/update method
                    Log.d("SyncHelper", "User synced: " + user.getEmail());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("SyncHelper", "Failed to sync users from Realtime DB", error.toException());
            }
        });
    }



    public static void syncAllClassesToFirestore(DatabaseHelper dbHelper) {
        List<Classes> classList = dbHelper.getAllClasses1();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        for (Classes cls : classList) {
            HashMap<String, Object> classMap = new HashMap<>();
            classMap.put("classId", cls.getClassId());
            classMap.put("className", cls.getClassName());
            classMap.put("courseCode", cls.getCourseCode());
            classMap.put("startDate", cls.getStartDate());
            classMap.put("endDate", cls.getEndDate());
            classMap.put("isActive", cls.getIsActive());
            classMap.put("roomId", cls.getRoomId());
            classMap.put("teacherId", cls.getTeacherId());
            classMap.put("schedule", cls.getSchedule());

            firestore.collection("classes").document(cls.getClassId())
                    .set(classMap)
                    .addOnSuccessListener(aVoid -> Log.d("SyncHelper", "Synced class: " + cls.getClassId()))
                    .addOnFailureListener(e -> Log.e("SyncHelper", "Failed to sync class: " + cls.getClassId(), e));
        }
    }



    public static void syncAllClassesToRealtimeDb(DatabaseHelper dbHelper) {
        List<Classes> classList = dbHelper.getAllClasses1(); // You should have this method

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("classes");

        for (Classes cls : classList) {
            String classId = cls.getClassId();

            HashMap<String, Object> classMap = new HashMap<>();
            classMap.put("classId", cls.getClassId());
            classMap.put("className", cls.getClassName());
            classMap.put("courseCode", cls.getCourseCode());
            classMap.put("startDate", cls.getStartDate());
            classMap.put("endDate", cls.getEndDate());
            classMap.put("isActive", cls.getIsActive());
            classMap.put("roomId", cls.getRoomId());
            classMap.put("teacherId", cls.getTeacherId());
            classMap.put("schedule", cls.getSchedule());

            dbRef.child(classId).setValue(classMap)
                    .addOnSuccessListener(aVoid -> Log.d("SyncHelper", "Synced class: " + classId))
                    .addOnFailureListener(e -> Log.e("SyncHelper", "Failed to sync class: " + classId, e));
        }
    }


    public static void syncClassesFromRealtimeDbToLocal(DatabaseHelper dbHelper) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("classes");

        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot classSnap : snapshot.getChildren()) {
                    Classes cls = new Classes();
                    cls.setClassId(classSnap.child("classId").getValue(String.class));
                    cls.setClassName(classSnap.child("className").getValue(String.class));
                    cls.setCourseCode(classSnap.child("courseCode").getValue(String.class));
                    cls.setStartDate(classSnap.child("startDate").getValue(String.class));
                    cls.setEndDate(classSnap.child("endDate").getValue(String.class));
                    cls.setIsActive(classSnap.child("isActive").getValue(Integer.class));
                    cls.setRoomId(classSnap.child("roomId").getValue(String.class));
                    cls.setTeacherId(classSnap.child("teacherId").getValue(String.class));
                    cls.setSchedule(classSnap.child("schedule").getValue(String.class));

                    dbHelper.insertOrUpdateClass(cls); // Use your existing SQLite method
                }

                Log.d("SyncHelper", "Classes synced from Realtime DB to local DB.");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("SyncHelper", "Failed to sync classes from Realtime DB", error.toException());
            }
        });
    }



    public static void syncClassesFromFirestoreToLocal(DatabaseHelper dbHelper) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        firestore.collection("classes").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Classes cls = new Classes();
                        cls.setClassId(doc.getString("classId"));
                        cls.setClassName(doc.getString("className"));
                        cls.setCourseCode(doc.getString("courseCode"));
                        cls.setStartDate(doc.getString("startDate"));
                        cls.setEndDate(doc.getString("endDate"));
                        cls.setIsActive(doc.getLong("isActive") != null ? doc.getLong("isActive").intValue() : 1);
                        cls.setRoomId(doc.getString("roomId"));
                        cls.setTeacherId(doc.getString("teacherId"));
                        cls.setSchedule(doc.getString("schedule"));

                        dbHelper.insertOrUpdateClass(cls);  // Custom method to insert/update into SQLite
                    }

                    Log.d("SyncHelper", "Classes synced from Firestore to local DB.");
                })
                .addOnFailureListener(e -> Log.e("SyncHelper", "Failed to sync classes from Firestore", e));
    }

    public static void syncNotificationsToFirestore(DatabaseHelper dbHelper) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        List<NotificationModel> notifications = dbHelper.getAllNotifications();

        for (NotificationModel notification : notifications) {
            Map<String, Object> notificationMap = new HashMap<>();
            notificationMap.put("notificationId", notification.getNotificationId());
            notificationMap.put("receiverId", notification.getReceiverId());
            notificationMap.put("senderId", notification.getSender());
            notificationMap.put("message", notification.getMessage());
            notificationMap.put("isRead", notification.isRead());  // fixed here
            notificationMap.put("createdAt", notification.getCreatedAt());
            notificationMap.put("classId", notification.getClassId());
            notificationMap.put("notificationType", notification.getNotificationType());

            firestore.collection("notifications")
                    .document(notification.getNotificationId())
                    .set(notificationMap)
                    .addOnSuccessListener(aVoid ->
                            Log.d("SyncHelper", "Synced notification: " + notification.getNotificationId()))
                    .addOnFailureListener(e ->
                            Log.e("SyncHelper", "Failed to sync notification: " + notification.getNotificationId(), e));
        }
    }
    public static void syncNotificationsFromFirestore(DatabaseHelper dbHelper) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        firestore.collection("notifications")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String notificationId = doc.getString("notificationId");
                        String receiverId = doc.getString("receiverId");
                        String senderId = doc.getString("senderId");
                        String message = doc.getString("message");
                        boolean isRead = Boolean.TRUE.equals(doc.getBoolean("isRead"));
                        String createdAt = doc.getString("createdAt");
                        String classId = doc.getString("classId");
                        String notificationType = doc.getString("notificationType");

                        NotificationModel notification = new NotificationModel(
                                notificationId, receiverId, message, isRead, createdAt,
                                classId, senderId, notificationType
                        );
                        Log.d("NotifDebug", "Receiver ID: " + notification.getReceiverId());

                        dbHelper.insertOrUpdateNotification(notification);
                    }

                    Log.d("SyncHelper", "Notifications synced from Firestore to SQLite.");
                })
                .addOnFailureListener(e -> Log.e("SyncHelper", "Failed to sync notifications from Firestore", e));
    }


    public static void syncLocalNotificationsToFirebase(Context context) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        List<NotificationModel> localNotifications = dbHelper.getAllNotifications();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("notifications");

        for (NotificationModel notification : localNotifications) {
            ref.child(notification.getNotificationId()).setValue(notification)
                    .addOnSuccessListener(unused -> {
                        Log.d("Syncin", "Synced notification: " + notification.getNotificationId());
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Syncin", "Failed to sync: " + notification.getNotificationId(), e);
                    });
        }
    }
    public static void syncFirebaseNotificationsToLocal(Context context) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("notifications");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot notifSnapshot : snapshot.getChildren()) {
                    NotificationModel notification = notifSnapshot.getValue(NotificationModel.class);
                    if (notification != null) {
                        // Save or update notification in local DB
                        dbHelper.insertOrUpdateNotification(notification);
                        Log.d("Syncing", "Downloaded and saved notification: " + notification.getNotificationId());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Syncing", "Failed to download notifications", error.toException());
            }
        });
    }

    public static void syncLocalEnrollmentsToFirestore(Context context) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        List<Enrollement> localEnrollments = dbHelper.getAllEnrollments();

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        CollectionReference enrollmentRef = firestore.collection("enrollments");

        for (Enrollement enrollment : localEnrollments) {
            enrollmentRef.document(enrollment.getEnrollmentId()).set(enrollment)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("SyncFirestore", "Enrollment synced: " + enrollment.getEnrollmentId());
                    })
                    .addOnFailureListener(e -> {
                        Log.e("SyncFirestore", "Failed to sync: " + enrollment.getEnrollmentId(), e);
                    });
        }
    }
    public static void syncLocalEnrollmentsToFirebase(Context context) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        List<Enrollement> localEnrollments = dbHelper.getAllEnrollments(); // Implement this method

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("enrollments");

        for (Enrollement enrollment : localEnrollments) {
            ref.child(enrollment.getEnrollmentId()).setValue(enrollment)
                    .addOnSuccessListener(unused ->
                            Log.d("Sync", "Synced enrollment: " + enrollment.getEnrollmentId()))
                    .addOnFailureListener(e ->
                            Log.e("Sync", "Failed to sync: " + enrollment.getEnrollmentId(), e));
        }
    }
    public static void syncEnrollmentsFromFirestoreToLocal(Context context) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        DatabaseHelper dbHelper = new DatabaseHelper(context);

        firestore.collection("enrollments")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Enrollement model = doc.toObject(Enrollement.class);
                        if (model != null && model.getEnrollmentId() != null) {
                            dbHelper.insertOrUpdateEnrollment(model);
                            Log.d("SyncEnrollments", "Synced: " + model.getEnrollmentId());
                        } else {
                            Log.w("SyncEnrollments", "Invalid enrollment skipped: " + doc.getId());
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("SyncEnrollments", "Failed to sync enrollments", e));
    }

    public static void syncRoomsFromLocalToFirestore(Context context) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        DatabaseHelper dbHelper = new DatabaseHelper(context);

        List<Room> rooms = dbHelper.getAllRooms(); // Fetch from SQLite

        for (Room room : rooms) {
            if (room.getRoomId() != null) {
                firestore.collection("rooms")
                        .document(room.getRoomId())
                        .set(room)
                        .addOnSuccessListener(aVoid ->
                                Log.d("SyncRooms", "Synced to Firestore: " + room.getRoomId()))
                        .addOnFailureListener(e ->
                                Log.e("SyncRooms", "Failed to sync room: " + room.getRoomId(), e));
            } else {
                Log.w("SyncRooms", "Skipped room with null ID");
            }
        }
    }
    public static void syncRoomsToRealtimeDatabase(Context context) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference roomsRef = database.getReference("rooms");

        // Fetch the rooms from the local SQLite database
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("rooms", null, null, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            // Iterate through all the rows and create Room objects
            do {
                String roomId = cursor.getString(cursor.getColumnIndexOrThrow("roomId"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String building = cursor.getString(cursor.getColumnIndexOrThrow("building"));
                String floor = cursor.getString(cursor.getColumnIndexOrThrow("floor"));
                String deviceId = cursor.getString(cursor.getColumnIndexOrThrow("deviceId"));
                int isActive = cursor.getInt(cursor.getColumnIndexOrThrow("isActive"));

                Room room = new Room(roomId, name, building, floor, deviceId, isActive);

                // Create a map of the room data to send to Realtime Database
                Map<String, Object> roomMap = new HashMap<>();
                roomMap.put("roomId", room.getRoomId());
                roomMap.put("name", room.getName());
                roomMap.put("building", room.getBuilding());
                roomMap.put("floor", room.getFloor());
                roomMap.put("deviceId", room.getDeviceId());
                roomMap.put("isActive", room.getIsActive());

                // Upload room to Firebase Realtime Database
                roomsRef.child(room.getRoomId()) // Using roomId as key
                        .setValue(roomMap)
                        .addOnSuccessListener(aVoid -> Log.d("SyncRooms", "Room synced: " + room.getRoomId()))
                        .addOnFailureListener(e -> Log.e("SyncRooms", "Failed to sync room: " + room.getRoomId(), e));

            } while (cursor.moveToNext());
            cursor.close();
        } else {
            Log.w("SyncRooms", "No rooms found in local DB.");
        }
    }
    public static void syncRoomsFromFirestoreToLocal(Context context) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        DatabaseHelper dbHelper = new DatabaseHelper(context);

        firestore.collection("rooms")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Room room = doc.toObject(Room.class);

                        if (room != null) {
                            Log.d("RawRoomDoc", "Data: " + doc.getData());

                            // Insert or update the room in the local SQLite database
                            ContentValues values = new ContentValues();
                            values.put("roomId", room.getRoomId());
                            values.put("name", room.getName());
                            values.put("building", room.getBuilding());
                            values.put("floor", room.getFloor());
                            values.put("deviceId", room.getDeviceId());
                            values.put("isActive", room.getIsActive());

                            // Use insertOrUpdateRoom method to insert or update the room
                            dbHelper.insertOrUpdateRoom(room);
                            Log.d("SyncRooms", "Synced room: " + room.getRoomId());
                        } else {
                            Log.w("SyncRooms", "Room is null: " + doc.getId());
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("SyncRooms", "Failed to sync rooms", e));
    }



    public static void syncSessionsFromLocalToFirestore(Context context) {
        // Initialize Firestore and DatabaseHelper
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        DatabaseHelper dbHelper = new DatabaseHelper(context);

        // Get writable SQLite database
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Query to retrieve all sessions from local SQLite database
        Cursor cursor = db.query(
                "sessions", // Table name
                null, // Select all columns
                null, // No where clause
                null, // No where arguments
                null, // No group by
                null, // No having
                null // No order by
        );

        // Iterate through the cursor and push data to Firestore
        while (cursor.moveToNext()) {
            // Extract data from the cursor
            String sessionId = cursor.getString(cursor.getColumnIndex("sessionId"));
            String classId = cursor.getString(cursor.getColumnIndex("classId"));
            String date = cursor.getString(cursor.getColumnIndex("date"));
            String startTime = cursor.getString(cursor.getColumnIndex("startTime"));
            String endTime = cursor.getString(cursor.getColumnIndex("endTime"));
            int attendanceEnabled = cursor.getInt(cursor.getColumnIndex("attendanceEnabled"));

            // Create a map to store session data for Firestore
            Map<String, Object> sessionMap = new HashMap<>();
            sessionMap.put("sessionId", sessionId);
            sessionMap.put("classId", classId);
            sessionMap.put("date", date);
            sessionMap.put("startTime", startTime);
            sessionMap.put("endTime", endTime);
            sessionMap.put("attendanceEnabled", attendanceEnabled);

            // Add session data to Firestore
            firestore.collection("sessions")
                    .document(sessionId) // Use sessionId as document ID
                    .set(sessionMap)
                    .addOnSuccessListener(aVoid -> Log.d("SyncSessions", "Session synced: " + sessionId))
                    .addOnFailureListener(e -> Log.e("SyncSessions", "Failed to sync session: " + sessionId, e));
        }

        // Close the cursor
        cursor.close();
    }
    public static void syncSessionsFromLocalToRealtimeDB(Context context) {
        // Initialize Firebase Realtime Database and DatabaseHelper
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        DatabaseReference sessionsRef = firebaseDatabase.getReference("sessions");
        DatabaseHelper dbHelper = new DatabaseHelper(context);

        // Get readable SQLite database
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Query to retrieve all sessions from local SQLite database
        Cursor cursor = db.query(
                "sessions", // Table name
                null, // Select all columns
                null, // No where clause
                null, // No where arguments
                null, // No group by
                null, // No having
                null // No order by
        );

        // Iterate through the cursor and push data to Firebase Realtime Database
        while (cursor.moveToNext()) {
            // Extract data from the cursor
            String sessionId = cursor.getString(cursor.getColumnIndex("sessionId"));
            String classId = cursor.getString(cursor.getColumnIndex("classId"));
            String date = cursor.getString(cursor.getColumnIndex("date"));
            String startTime = cursor.getString(cursor.getColumnIndex("startTime"));
            String endTime = cursor.getString(cursor.getColumnIndex("endTime"));
            int attendanceEnabled = cursor.getInt(cursor.getColumnIndex("attendanceEnabled"));

            // Create a map to store session data for Firebase Realtime Database
            Map<String, Object> sessionMap = new HashMap<>();
            sessionMap.put("sessionId", sessionId);
            sessionMap.put("classId", classId);
            sessionMap.put("date", date);
            sessionMap.put("startTime", startTime);
            sessionMap.put("endTime", endTime);
            sessionMap.put("attendanceEnabled", attendanceEnabled);

            // Add session data to Firebase Realtime Database
            sessionsRef.child(sessionId) // Use sessionId as the key
                    .setValue(sessionMap)
                    .addOnSuccessListener(aVoid -> Log.d("SyncSessions", "Session synced: " + sessionId))
                    .addOnFailureListener(e -> Log.e("SyncSessions", "Failed to sync session: " + sessionId, e));
        }

        // Close the cursor
        cursor.close();
    }
    public static void syncSessionsFromFirestoreToLocal(Context context) {
        // Initialize Firestore and DatabaseHelper
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        DatabaseHelper dbHelper = new DatabaseHelper(context);

        // Query the "sessions" collection in Firestore
        firestore.collection("sessions")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();

                    // Iterate through the Firestore documents and insert into SQLite
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        // Extract data from Firestore document
                        String sessionId = document.getString("sessionId");
                        String classId = document.getString("classId");
                        String date = document.getString("date");
                        String startTime = document.getString("startTime");
                        String endTime = document.getString("endTime");
                        boolean attendanceEnabled = document.getLong("attendanceEnabled") != null &&
                                document.getLong("attendanceEnabled") == 1;


                        // Create ContentValues to insert into SQLite
                        ContentValues values = new ContentValues();
                        values.put("sessionId", sessionId);
                        values.put("classId", classId);
                        values.put("date", date);
                        values.put("startTime", startTime);
                        values.put("endTime", endTime);
                        values.put("attendanceEnabled", attendanceEnabled ? 1 : 0);

                        // Insert or update the session in SQLite
                        long rowId = db.replace("sessions", null, values);  // Use `replace` to avoid duplicates

                        if (rowId != -1) {
                            Log.d("SyncSessions", "Synced session: " + sessionId);
                        } else {
                            Log.e("SyncSessions", "Failed to sync session: " + sessionId);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("SyncSessions", "Failed to sync sessions", e));
    }


    public static void syncQRCodesToFirestore(Context context) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        List<QRCodeModel> qrCodes = dbHelper.getAllQRCodes(); // Retrieves data as it is from SQLite

        for (QRCodeModel qrCode : qrCodes) {
            // Directly log and sync the QR code without modification
            Log.d("QRCodeSync", "Before Sync - QR Code ID: " + qrCode.getQrCodeId());
            Log.d("QRCodeSync", "Before Sync - Created At: " + qrCode.getCreatedAt());
            Log.d("QRCodeSync", "Before Sync - Expires At: " + qrCode.getExpiresAt());

            // Directly store the data in Firestore
            firestore.collection("qr_codes").document(qrCode.getQrCodeId())
                    .set(qrCode)
                    .addOnSuccessListener(aVoid -> Log.d("QRCodeSync", "Synced QR code: " + qrCode.getQrCodeId()))
                    .addOnFailureListener(e -> Log.e("QRCodeSync", "Failed to sync QR code: " + qrCode.getQrCodeId(), e));
        }
    }

    // Method to convert a string or long to a Firestore Timestamp
    private static Timestamp convertToTimestamp(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof String) {
            try {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                Date date = format.parse((String) value);
                if (date != null) {
                    return new Timestamp(date);
                }
            } catch (ParseException e) {
                Log.e("QRCodeSync", "Failed to parse string to date: " + value, e);
            }
        } else if (value instanceof Long) {
            // Convert the Long to a Date object, then to a Timestamp
            Date date = new Date((Long) value);
            return new Timestamp(date);
        } else if (value instanceof Timestamp) {
            return (Timestamp) value; // Already in the correct format
        }

        return null;
    }



    // Method to convert a String to a Firestore Timestamp
    private static Timestamp convertToTimestamp(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return null;  // Return null if the string is empty or null
        }
        try {
            // Parse the dateString (assuming it's in a standard format like "yyyy-MM-dd HH:mm:ss")
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = format.parse(dateString);
            if (date != null) {
                return new Timestamp(date);
            }
        } catch (ParseException e) {
            Log.e("QRCodeSync", "Failed to parse date: " + dateString, e);
        }
        return null; // Return null if parsing fails
    }









    public static void syncQRCodesToRealtimeDB(Context context) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        DatabaseReference qrCodesRef = database.getReference("qr_codes");

        List<QRCodeModel> qrList = dbHelper.getAllQRCodes();

        for (QRCodeModel qr : qrList) {
            String qrCodeId = qr.getQrCodeId();

            Map<String, Object> qrData = new HashMap<>();
            qrData.put("value", qr.getValue());
            qrData.put("type", qr.getType());
            qrData.put("userId", qr.getUserId());
            qrData.put("targetId", qr.getTargetId());

            // Syncing the createdAt and expiresAt timestamps
            qrData.put("createdAt", qr.getCreatedAt().getSeconds()); // Firestore Timestamp to seconds
            qrData.put("expiresAt", qr.getExpiresAt().getSeconds());
            qrData.put("isActive", qr.getIsActive() ? 1 : 0);

            qrCodesRef.child(qrCodeId).setValue(qrData)
                    .addOnSuccessListener(aVoid -> Log.d("SyncQRCodes", "QR Code " + qrCodeId + " synced to Firebase Realtime DB."))
                    .addOnFailureListener(e -> Log.e("SyncQRCodes", "Failed to sync QR code " + qrCodeId, e));
        }
    }







    public static void syncQRCodesFromFirestoreToLDB(Context context) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        firestore.collection("qr_codes")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        String qrCodeId = document.getId();
                        String value = document.getString("value");
                        String type = document.getString("type");
                        String userId = document.getString("userId");
                        String targetId = document.getString("targetId");
                        boolean isActive = Boolean.TRUE.equals(document.getBoolean("isActive"));
                        String qrCodeImageBase64 = document.getString("qrCodeImageBase64");

                        Timestamp createdAt = document.getTimestamp("createdAt");
                        Timestamp expiresAt = document.getTimestamp("expiresAt");

                        long createdAtMillis = createdAt != null ? createdAt.toDate().getTime() : 0;
                        long expiresAtMillis = expiresAt != null ? expiresAt.toDate().getTime() : 0;

                        ContentValues values = new ContentValues();
                        values.put("qrCodeId", qrCodeId);
                        values.put("value", value);
                        values.put("type", type);
                        values.put("userId", userId);
                        values.put("targetId", targetId);
                        values.put("createdAt", createdAtMillis); // Store as INTEGER
                        values.put("expiresAt", expiresAtMillis); // Store as INTEGER
                        values.put("isActive", isActive ? 1 : 0);

                        if (qrCodeImageBase64 != null) {
                            byte[] decodedImage = Base64.decode(qrCodeImageBase64, Base64.DEFAULT);
                            values.put("qrCodeImage", decodedImage);
                        }

                        long result = db.replace("qr_codes", null, values);
                        if (result != -1) {
                            Log.d("SyncQRCodes", "QR code " + qrCodeId + " synced to local DB.");
                        } else {
                            Log.e("SyncQRCodes", "Failed to sync QR code: " + qrCodeId);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("SyncQRCodes", "Failed to sync QR codes", e));
    }



    public static void syncAttendanceToFirestoreAndRealtimeDB(Context context) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        FirebaseDatabase realtimeDB = FirebaseDatabase.getInstance();
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        DatabaseReference realtimeRef = realtimeDB.getReference("attendance");

        List<AttendanceModel> attendanceList = dbHelper.getAllAttendanceRecords();  // <-- You'll need to define this method in your DatabaseHelper

        for (AttendanceModel record : attendanceList) {
            String attendanceId = record.getAttendanceId();

            // Prepare data to sync
            Map<String, Object> data = new HashMap<>();
            data.put("attendanceId", attendanceId);
            data.put("userId", record.getUserId());
            data.put("sessionId", record.getSessionId());
            data.put("qrCodeId", record.getQrCodeId());
            data.put("markedAt", record.getMarkedAt().getTime()); // store timestamp as millis
            data.put("deviceInfo", record.getDeviceInfo());

            // Sync to Firestore
            firestore.collection("attendance")
                    .document(attendanceId)
                    .set(data)
                    .addOnSuccessListener(unused -> Log.d("SyncAttendance", "Synced to Firestore: " + attendanceId))
                    .addOnFailureListener(e -> Log.e("SyncAttendance", "Failed to sync to Firestore", e));

            // Sync to Realtime Database
            realtimeRef.child(attendanceId)
                    .setValue(data)
                    .addOnSuccessListener(unused -> Log.d("SyncAttendance", "Synced to RTDB: " + attendanceId))
                    .addOnFailureListener(e -> Log.e("SyncAttendance", "Failed to sync to RTDB", e));
        }
    }

    FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    public static void syncAttendanceFromFirestoreToLocal(Context context) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        firestore.collection("attendance")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        String attendanceId = document.getString("attendanceId");
                        String userId = document.getString("userId");
                        String sessionId = document.getString("sessionId");
                        String qrCodeId = document.getString("qrCodeId");
                        String deviceInfo = document.getString("deviceInfo");

                        // markedAt is a long (number), not Firestore Timestamp
                        Long markedAtLong = document.getLong("markedAt");
                        long markedAt = markedAtLong != null ? markedAtLong : System.currentTimeMillis();

                        Log.d("FirestoreData", "Fetched attendanceId: " + attendanceId);

                        if (attendanceId != null && userId != null && sessionId != null && qrCodeId != null) {
                            dbHelper.insertAttendanceIntoLocalDB(
                                    context,
                                    attendanceId,
                                    userId,
                                    sessionId,
                                    qrCodeId,
                                    markedAt,
                                    deviceInfo
                            );
                            Log.d("FirestoreSync", "Synced to SQLite: " + attendanceId);
                        } else {
                            Log.w("FirestoreSync", "Missing required fields for: " + attendanceId);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreSync", "Failed to sync from Firestore", e);
                });
    }


    public static void syncUsersFromRealtimeToFirestore() {
        DatabaseReference realtimeUsersRef = FirebaseDatabase.getInstance().getReference("users");
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        realtimeUsersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String userId = userSnapshot.getKey();
                    Map<String, Object> userMap = (Map<String, Object>) userSnapshot.getValue();

                    if (userMap == null) continue;

                    firestore.collection("users")
                            .document(userId)
                            .set(userMap)
                            .addOnSuccessListener(aVoid -> Log.d("Sync2", "User synced: " + userId))
                            .addOnFailureListener(e -> Log.e("Sync2", "Failed syncing user: " + userId, e));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Sync2", "Failed to fetch users from Realtime DB", error.toException());
            }
        });
    }


}

