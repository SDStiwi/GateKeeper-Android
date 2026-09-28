package com.sdstiwi.gatekeeper;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomnavigation.BottomNavigationItemView;

public class TeacherDashboard extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_main);

        // Guard this activity so a student session can never open the teacher shell.
        android.content.SharedPreferences session = getSharedPreferences("UserSession", MODE_PRIVATE);
        String role = session.getString("role", "");
        if (!"teacher".equalsIgnoreCase(role)) {
            android.widget.Toast.makeText(this, "Teacher dashboard requires a teacher account.", android.widget.Toast.LENGTH_SHORT).show();
            startActivity(new android.content.Intent(this, LoginActivity.class));
            finish();
            return;
        }

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Set the default fragment to Home
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment()).commit();
        }

        // Set listener for navigation item selection
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            // Handle fragment switching
            switch (item.getItemId()) {
                case R.id.nav_home:
                    selectedFragment = new HomeFragment();
                    break;
                case R.id.nav_timetable:
                    selectedFragment = new TimeTableFragment();
                    break;
                case R.id.nav_notifications:
                    selectedFragment = new NotificationsFragment();
                    break;
                case R.id.nav_profile:
                    selectedFragment = new ProfileFragment();
                    break;
                case R.id.nav_attendance:
                    selectedFragment = new AttendanceFragment();
                    break;
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment).commit();
            }

            // Animate the BottomNavigationView on item selection
            animateBubbleEffect(bottomNavigationView, item);

            return true;
        });
    }

    // Method to animate the bubble effect and elevation
    private void animateBubbleEffect(BottomNavigationView bottomNavigationView, MenuItem item) {
        // Loop through all items and reset their elevation and background
        for (int i = 0; i < bottomNavigationView.getMenu().size(); i++) {
            MenuItem menuItem = bottomNavigationView.getMenu().getItem(i);

            // Get the BottomNavigationItemView for each menu item
            BottomNavigationItemView itemView = (BottomNavigationItemView) bottomNavigationView.findViewById(menuItem.getItemId());

            if (itemView != null) {
                // Reset the background and elevation for all items
                itemView.setBackground(null); // Reset background
                itemView.setTranslationY(0f); // Reset elevation
            }
        }


        }
    }

