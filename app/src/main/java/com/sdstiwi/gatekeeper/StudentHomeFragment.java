package com.sdstiwi.gatekeeper;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class StudentHomeFragment extends Fragment {

    private RecyclerView todayClassesRecyclerView, notificationsRecyclerView,recyclerViewRooms;
    private StudentClassAdapter classAdapter;
    private NotificationAdapter notificationAdapter;
    private RoomAdapter roomAdapter;
    private DatabaseHelper dbHelper;
    private List<StudentClassModel> todayClasses;
    private List<NotificationModel> notifications;
    private DatabaseHelper databaseHelper;
    private String studentId;
    private Button btnEnroll;
    public StudentHomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.home_fragment_student, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dbHelper = new DatabaseHelper(getContext());
        todayClassesRecyclerView = view.findViewById(R.id.recyclerViewSchedule);
        todayClassesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        todayClassesRecyclerView.setVerticalScrollBarEnabled(true);
        todayClassesRecyclerView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            todayClassesRecyclerView.setVerticalScrollbarThumbDrawable(
                    ContextCompat.getDrawable(requireContext(), R.drawable.costum_scrollbar_thumb));
        }

        notificationsRecyclerView = view.findViewById(R.id.recyclerViewNotifications);
        notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        notificationsRecyclerView.setVerticalScrollBarEnabled(true);
        notificationsRecyclerView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            notificationsRecyclerView.setVerticalScrollbarThumbDrawable(
                    ContextCompat.getDrawable(requireContext(), R.drawable.costum_scrollbar_thumb));
        }
        btnEnroll =view.findViewById(R.id.btnEnroll);
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("UserSession", getContext().MODE_PRIVATE);
        String userID = sharedPreferences.getString("uid", "");
        databaseHelper = new DatabaseHelper(requireContext());
        studentId = userID;
        LoginActivity.startSynchronization(requireContext());
        todayClasses = new ArrayList<>();
        notifications = new ArrayList<>();

        recyclerViewRooms = view.findViewById(R.id.recyclerViewRooms); // Fixed missing declaration
        recyclerViewRooms.setLayoutManager(new LinearLayoutManager(getContext())); // Fixed missing layout manager
        recyclerViewRooms.setVerticalScrollBarEnabled(true);
        recyclerViewRooms.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            recyclerViewRooms.setVerticalScrollbarThumbDrawable(
                    ContextCompat.getDrawable(requireContext(), R.drawable.costum_scrollbar_thumb));
        }
        TextView txtName = view.findViewById(R.id.txtName);
        TextView txtDepartment = view.findViewById(R.id.txtDepartment);
        TextView txtEmail = view.findViewById(R.id.txtEmail);


        Users user = dbHelper.getUserProfile(studentId);

        if (user != null) {
            txtName.setText(user.getFirstName() + " " + user.getLastName());
            txtEmail.setText(user.getEmail());
            txtDepartment.setText(user.getDepartment() != null ? user.getDepartment() : "N/A");
        }

        todayClassesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        classAdapter = new StudentClassAdapter(todayClasses, getContext());
        notificationAdapter = new NotificationAdapter(notifications, getContext(), databaseHelper);
        recyclerViewRooms.setLayoutManager(new LinearLayoutManager(getContext()));
        todayClassesRecyclerView.setAdapter(classAdapter);
        notificationsRecyclerView.setAdapter(notificationAdapter);


        // Add visibility debug logs
        Log.d("StudentHomeFragment", "Before loading data - Notification count: " + notifications.size());

        loadTodayClasses();
        loadRecentNotifications();
        fetchTodaysRooms();
        btnEnroll.setOnClickListener(v -> openEnrollFragment());
        Log.d("StudentHomeFragment", "After loading data - Notification count: " + notifications.size());
    }

    private void loadTodayClasses() {
        todayClasses.clear();

        List<Session> sessions = databaseHelper.getStudentClassesForToday(studentId); // Updated method call

        if (sessions != null && !sessions.isEmpty()) {
            Log.d("StudentHomeFragment", "Classes fetched: " + sessions.size());

            for (Session session : sessions) {
                Log.d("StudentHomeFragment", "Class: " + session.getClassName() +
                        " (" + session.getDate() + ") from " + session.getStartTime() + " to " + session.getEndTime());

                todayClasses.add(new StudentClassModel(
                        session.getSessionId(),
                        session.getClassId(),
                        session.getClassName(),
                        "", // courseCode not available from Session model? Optional field
                        session.getDate(),
                        session.getStartTime(),
                        session.getEndTime(),
                        session.getRoomName()
                ));
            }
        } else {
            Log.d("StudentHomeFragment", "No classes found for today.");
        }

        classAdapter.notifyDataSetChanged();
    }

    private void fetchTodaysRooms() {
        Log.d("StudentHomeFragment", "fetchTodaysRooms() called");



        if (studentId.isEmpty()) {
            Log.e("StudentHomeFragment", "student ID not found in SharedPreferences!");
            return;
        }

        Log.d("StudentHomeFragment", "Fetching rooms for student ID: " + studentId);

        List<Room> roomsToTeachIn = databaseHelper.getRoomsToTakeToday(studentId);

        roomAdapter = new RoomAdapter(roomsToTeachIn);
        recyclerViewRooms.setAdapter(roomAdapter);

        Log.d("StudentHomeFragment", "Total rooms retrieved: " + roomsToTeachIn.size());
    }


    private void loadRecentNotifications() {
        notifications.clear();
        Cursor cursor = databaseHelper.getLatestNotifications(studentId, 3);

        if (cursor != null) {
            Log.d("StudentHomeFragment", "Notifications fetched: " + cursor.getCount());
            while (cursor.moveToNext()) {
                String notificationId = cursor.getString(0);
                String recieverId = cursor.getString(1);
                String message = cursor.getString(2);
                String createdAt = cursor.getString(3);
                boolean isRead = cursor.getInt(4) == 1;
                String classId = cursor.getString(5);
                String senderId = cursor.getString(6);
                String notificationType = cursor.getString(7);

                Log.d("StudentHomeFragment", "Notification: ID=" + notificationId + ", Message=" + message);

                NotificationModel notification = new NotificationModel(notificationId, recieverId, message, isRead, createdAt, classId, senderId, notificationType);
                notifications.add(notification);
            }
            cursor.close();
        }

        Log.d("StudentHomeFragment", "Total notifications added: " + notifications.size());

        // Notify adapter outside the loop
        notificationAdapter.notifyDataSetChanged();
    }
    private void openEnrollFragment() {
        Fragment enrollFragment = new EnrollFragment();
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, enrollFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
