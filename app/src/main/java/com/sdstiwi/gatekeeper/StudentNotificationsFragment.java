package com.sdstiwi.gatekeeper;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import java.util.List;
public class StudentNotificationsFragment extends Fragment {

    private RecyclerView recyclerView;
    private NotificationAdapter notificationAdapter;
    private DatabaseHelper databaseHelper;

    public StudentNotificationsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);
        LoginActivity.startSynchronization(requireContext());
        // Initialize RecyclerView
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Get logged-in user ID from SharedPreferences
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("UserSession", getContext().MODE_PRIVATE);
        String userID = sharedPreferences.getString("uid", ""); // Ensure correct key

        // Fetch notifications from database
        databaseHelper = new DatabaseHelper(getActivity());
        List<NotificationModel> notifications = databaseHelper.getUserNotifications(userID);

        // Set up adapter
        // Set up adapter with required arguments
        notificationAdapter = new NotificationAdapter(notifications, getActivity(), databaseHelper);
        recyclerView.setAdapter(notificationAdapter);


        return view;
    }
}
