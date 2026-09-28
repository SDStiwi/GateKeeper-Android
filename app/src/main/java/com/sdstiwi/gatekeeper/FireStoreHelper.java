package com.sdstiwi.gatekeeper;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.List;

public class FireStoreHelper {

    private static final String TAG = "FirestoreSync";

    public static void syncUserToFirestore(Users user) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
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
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User synced: " + user.getEmail());

                    // Continue sync based on role
                    if (user.getRole().equalsIgnoreCase("student")) {
                        syncStudentEnrollments(user);
                    } else if (user.getRole().equalsIgnoreCase("teacher")) {
                        syncTeacherClasses(user);
                    }

                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "User sync failed: " + e.getMessage()));
    }

    public static void syncStudentEnrollments(Users user) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        List<Enrollement> enrollments = DatabaseHelper.getStudentEnrollments(user.getUid());

        for (Enrollement enrollment : enrollments) {
            HashMap<String, Object> enrollmentMap = new HashMap<>();
            enrollmentMap.put("studentId", enrollment.getStudentId());
            enrollmentMap.put("classId", enrollment.getClassId());
            enrollmentMap.put("status", enrollment.getStatus());

            firestore.collection("enrolledClasses")
                    .document(enrollment.getEnrollmentId())
                    .set(enrollmentMap)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Enrollment synced"))
                    .addOnFailureListener(e -> Log.e(TAG, "Enrollment sync failed: " + e.getMessage()));
        }
    }

    public static void syncTeacherClasses(Users user) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        List<ClassItem> teacherClasses = DatabaseHelper.getTeacherClasses(user.getUid());

        for (ClassItem classItem : teacherClasses) {
            HashMap<String, Object> classMap = new HashMap<>();
            classMap.put("classId", classItem.getClassId());
            classMap.put("className", classItem.getClassName());
            classMap.put("teacherId", classItem.getTeacherId());
            classMap.put("schedule", classItem.getSchedule());
            classMap.put("roomId", classItem.getRoomId());

            firestore.collection("classes")
                    .document(classItem.getClassId())
                    .set(classMap)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Class synced"))
                    .addOnFailureListener(e -> Log.e(TAG, "Class sync failed: " + e.getMessage()));
        }
    }
}
