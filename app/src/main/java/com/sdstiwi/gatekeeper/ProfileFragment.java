package com.sdstiwi.gatekeeper;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.sdstiwi.gatekeeper.R;

import androidx.fragment.app.Fragment;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ProfileFragment extends Fragment {

    private Button btnLogout;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        TextView txtName = view.findViewById(R.id.txtName);
        TextView txtEmail = view.findViewById(R.id.txtEmail);
        TextView txtSpeciality = view.findViewById(R.id.txtSpeciality);
        TextView txtDepartment = view.findViewById(R.id.txtDepartment);
        TextView txtCreatedAt = view.findViewById(R.id.txtCreatedAt);
        LoginActivity.startSynchronization(requireContext());

        SharedPreferences preferences = getActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        String uid = preferences.getString("uid", "");

        DatabaseHelper dbHelper = new DatabaseHelper(getContext());
        Users user = dbHelper.getUserProfile(uid);

        if (user != null) {
            txtName.setText(user.getFirstName() + " " + user.getLastName());
            txtEmail.setText(user.getEmail());
            txtSpeciality.setText(user.getSpeciality() != null ? user.getSpeciality() : "N/A");
            txtDepartment.setText(user.getDepartment() != null ? user.getDepartment() : "N/A");
            txtCreatedAt.setText(user.getCreatedAt() != null ? user.getCreatedAt() : "Not available");


        }

        // Set the logout button click listener
        btnLogout = view.findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> logoutUser());

        return view;
    }

    private String formatLastLogin(String lastLogin) {
        // Assuming lastLogin is in the format "yyyy-MM-dd HH:mm:ss"
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

        try {
            Date date = dateFormat.parse(lastLogin);
            if (date != null) {
                return timeFormat.format(date);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return "N/A"; // Return this if parsing fails
    }

    private void logoutUser() {
        // Clear the saved user session from SharedPreferences
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserSession", requireContext().MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear(); // Clear all stored session data
        editor.apply();

        // Redirect the user to the login screen
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        startActivity(intent);
        requireActivity().finish(); // Close the current activity
    }
}
