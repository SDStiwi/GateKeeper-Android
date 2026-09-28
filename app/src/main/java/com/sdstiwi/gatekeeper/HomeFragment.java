package com.sdstiwi.gatekeeper;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.sdstiwi.gatekeeper.R;


import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerViewSchedule, recyclerViewRooms;
    private ScheduleAdapter scheduleAdapter;
    private RoomAdapter roomAdapter;
    private DatabaseHelper dbHelper;
    private RecyclerView recyclerViewNotifications;
    private NotificationAdapter notificationAdapter;
    private List<NotificationModel> notifications = new ArrayList<>();
    private Button btnViewAttendance;
    private BluetoothHelper bluetoothHelper;



    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.dash, container, false);
        dbHelper = new DatabaseHelper(getContext());
        recyclerViewNotifications = view.findViewById(R.id.recyclerViewNotifications);
        recyclerViewNotifications.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewNotifications.setVerticalScrollBarEnabled(true);
        recyclerViewNotifications.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            recyclerViewNotifications.setVerticalScrollbarThumbDrawable(
                    ContextCompat.getDrawable(requireContext(), R.drawable.costum_scrollbar_thumb));
        }

        notificationAdapter = new NotificationAdapter(notifications, getContext(), dbHelper);
        recyclerViewNotifications.setAdapter(notificationAdapter);

        LoginActivity.startSynchronization(requireContext());

        recyclerViewSchedule = view.findViewById(R.id.recyclerViewSchedule);
        recyclerViewRooms = view.findViewById(R.id.recyclerViewRooms); // Fixed missing declaration

        TextView txtName = view.findViewById(R.id.txtName);
        TextView txtDepartment = view.findViewById(R.id.txtDepartment);
        TextView txtEmail = view.findViewById(R.id.txtEmail);

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("UserSession", getContext().MODE_PRIVATE);
        String uid = sharedPreferences.getString("uid", "");

        Users user = dbHelper.getUserProfile(uid);

        if (user != null) {
            txtName.setText(user.getFirstName() + " " + user.getLastName());
            txtEmail.setText(user.getEmail());
            txtDepartment.setText(user.getDepartment() != null ? user.getDepartment() : "N/A");
             }


        // Set up RecyclerViews
        recyclerViewSchedule.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewSchedule.setVerticalScrollBarEnabled(true);
        recyclerViewSchedule.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            recyclerViewSchedule.setVerticalScrollbarThumbDrawable(
                    ContextCompat.getDrawable(requireContext(), R.drawable.costum_scrollbar_thumb));
        }
        recyclerViewRooms.setLayoutManager(new LinearLayoutManager(getContext())); // Fixed missing layout manager
        recyclerViewRooms.setVerticalScrollBarEnabled(true);
        recyclerViewRooms.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            recyclerViewRooms.setVerticalScrollbarThumbDrawable(
                    ContextCompat.getDrawable(requireContext(), R.drawable.costum_scrollbar_thumb));
        }

        btnViewAttendance = view.findViewById(R.id.btnViewAttendance);

        btnViewAttendance.setOnClickListener(v -> {
            AtndFragment atndFragment = new AtndFragment();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, atndFragment)
                    .addToBackStack(null)
                    .commit();
        });
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        dbHelper.updateRoomStatuses(db); // updates isActive
        fetchTodaysSchedule();
        fetchTodaysRooms();
        loadRecentNotifications();
        return view;
    }

    private void fetchTodaysSchedule() {
        Log.d("HomeFragment", "fetchTodaysSchedule() called");

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("UserSession", getContext().MODE_PRIVATE);
        String teacherId = sharedPreferences.getString("uid", ""); // Use "uid" instead of "teacherId"

        if (teacherId.isEmpty()) {
            Log.e("HomeFragment", "Teacher ID not found in SharedPreferences!");
            return;
        }

        Log.d("HomeFragment", "Fetching schedule for Teacher ID: " + teacherId);

        List<Session> todaysSchedule = dbHelper.getTodaysScheduleForTeacher(teacherId);

        scheduleAdapter = new ScheduleAdapter(todaysSchedule);
        recyclerViewSchedule.setAdapter(scheduleAdapter);

        Log.d("HomeFragment", "Total sessions retrieved: " + todaysSchedule.size());
    }

    private void fetchTodaysRooms() {
        Log.d("HomeFragment", "fetchTodaysRooms() called");

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("UserSession", getContext().MODE_PRIVATE);
        String teacherId = sharedPreferences.getString("uid", "");

        if (teacherId.isEmpty()) {
            Log.e("HomeFragment", "Teacher ID not found in SharedPreferences!");
            return;
        }

        Log.d("HomeFragment", "Fetching rooms for Teacher ID: " + teacherId);

        List<Room> roomsToTeachIn = dbHelper.getRoomsToTeachInToday(teacherId);

        roomAdapter = new RoomAdapter(roomsToTeachIn);
        recyclerViewRooms.setAdapter(roomAdapter);

        Log.d("HomeFragment", "Total rooms retrieved: " + roomsToTeachIn.size());
    }

    private void loadRecentNotifications() {
        notifications.clear();  // Clear the list before loading new notifications

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("UserSession", getContext().MODE_PRIVATE);
        String studentId = sharedPreferences.getString("uid", "");

        if (studentId.isEmpty()) {
            Log.e("HomeFragment", "Student ID not found in SharedPreferences!");
            return;
        }

        Log.d("HomeFragment", "Fetching notifications for Student ID: " + studentId);

        Cursor cursor = dbHelper.getLatestNotifications(studentId, 3);

        if (cursor != null) {
            Log.d("HomeFragment", "Notifications fetched: " + cursor.getCount());

            if (cursor.getCount() > 0) {
                Log.d("HomeFragment", "Cursor has rows, moving to next...");

                // Iterate through the cursor to retrieve each notification
                while (cursor.moveToNext()) {
                    try {
                        String notificationId = cursor.getString(0);
                        String receiverId = cursor.getString(1);
                        String message = cursor.getString(2);
                        String createdAt = cursor.getString(3);
                        boolean isRead = cursor.getInt(4) == 1;
                        String classId = cursor.getString(5);
                        String senderId = cursor.getString(6);
                        String notificationType = cursor.getString(7);

                        // Create NotificationModel object
                        NotificationModel notification = new NotificationModel(
                                notificationId, receiverId, message, isRead, createdAt,
                                classId, senderId, notificationType
                        );

                        // Check if the notification is not null before adding
                        if (notification != null) {
                            notifications.add(notification);
                        } else {
                            Log.d("HomeFragment", "Notification was null, not added.");
                        }
                    } catch (Exception e) {
                        Log.e("HomeFragment", "Error reading cursor row", e);
                    }
                }
            } else {
                Log.d("HomeFragment", "No notifications found in cursor.");
            }

            cursor.close();
        } else {
            Log.d("HomeFragment", "Cursor is null.");
        }

        // Log the final list size to confirm whether notifications were added
        Log.d("HomeFragment", "Total notifications added: " + notifications.size());

        // Notify adapter that data has changed
        notificationAdapter.notifyDataSetChanged();
    }






}
