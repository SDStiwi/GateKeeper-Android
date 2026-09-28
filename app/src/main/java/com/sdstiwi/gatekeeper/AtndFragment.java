package com.sdstiwi.gatekeeper;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import java.util.Date;
import java.util.Locale;

public class AtndFragment extends Fragment {
    private LinearLayout attendanceContainer;

    public AtndFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_attendance, container, false);
        attendanceContainer = rootView.findViewById(R.id.attendanceContainer);

        SharedPreferences prefs = requireContext().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        String teacherId = prefs.getString("uid", "");
        LoginActivity.startSynchronization(requireContext());
        loadTodayAttendance(teacherId);

        return rootView;
    }

    private void loadTodayAttendance(String teacherId) {
        attendanceContainer.removeAllViews();

        DatabaseHelper dbHelper = new DatabaseHelper(requireContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        String query = "SELECT s.sessionId, s.startTime, s.endTime, c.className AS className " +
                "FROM sessions s " +
                "JOIN classes c ON s.classId = c.classId " +
                "WHERE DATE(s.date) = ? AND c.teacherId = ?";

        Cursor sessionCursor = db.rawQuery(query, new String[]{today, teacherId});
        if (sessionCursor.moveToFirst()) {
            do {
                String sessionId = sessionCursor.getString(sessionCursor.getColumnIndexOrThrow("sessionId"));
                String className = sessionCursor.getString(sessionCursor.getColumnIndexOrThrow("className"));
                String startTime = sessionCursor.getString(sessionCursor.getColumnIndexOrThrow("startTime"));
                String endTime = sessionCursor.getString(sessionCursor.getColumnIndexOrThrow("endTime"));

                // Create session card layout
                // Enhanced Card Container
                LinearLayout cardLayout = new LinearLayout(getContext());
                cardLayout.setOrientation(LinearLayout.VERTICAL);
                cardLayout.setBackgroundResource(R.drawable.enhanced_attendance_card);
                cardLayout.setPadding(40, 40, 40, 40);
                cardLayout.setElevation(12f);

                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                cardParams.setMargins(0, 0, 0, 24);
                cardLayout.setLayoutParams(cardParams);

// Enhanced Header Section
                LinearLayout headerLayout = new LinearLayout(getContext());
                headerLayout.setOrientation(LinearLayout.HORIZONTAL);
                headerLayout.setGravity(Gravity.CENTER_VERTICAL);
                headerLayout.setBackgroundResource(R.drawable.card_header_bg);
                headerLayout.setPadding(24, 20, 24, 20);
                LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                headerParams.setMargins(0, 0, 0, 24);
                headerLayout.setLayoutParams(headerParams);

// Class Icon
                ImageView classIcon = new ImageView(getContext());
                classIcon.setImageResource(R.drawable.ic_class_session);
                classIcon.setColorFilter(Color.WHITE);
                LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(48, 48);
                iconParams.setMargins(0, 0, 20, 0);
                classIcon.setLayoutParams(iconParams);

// Session Info Container
                LinearLayout sessionInfoLayout = new LinearLayout(getContext());
                sessionInfoLayout.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams sessionInfoParams = new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                sessionInfoLayout.setLayoutParams(sessionInfoParams);

// Enhanced Session Title
                TextView sessionTitle = new TextView(getContext());
                sessionTitle.setText(className);
                sessionTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                sessionTitle.setTextColor(Color.WHITE);
                sessionTitle.setTypeface(ResourcesCompat.getFont(getContext(), R.font.poppins_bold));
                sessionTitle.setPadding(0, 0, 0, 8);

// Session Time
                TextView sessionTime = new TextView(getContext());
                sessionTime.setText(startTime + " - " + endTime);
                sessionTime.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                sessionTime.setTextColor(Color.WHITE);
                sessionTime.setTypeface(ResourcesCompat.getFont(getContext(), R.font.popins));
                sessionTime.setAlpha(0.9f);

// Status Badge
                TextView statusBadge = new TextView(getContext());
                statusBadge.setText("COMPLETED");
                statusBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
                statusBadge.setTextColor(Color.WHITE);
                statusBadge.setTypeface(ResourcesCompat.getFont(getContext(), R.font.poppins_bold));
                statusBadge.setBackgroundResource(R.drawable.status_complete_bg);
                statusBadge.setPadding(16, 8, 16, 8);
                statusBadge.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                statusBadge.setLayoutParams(badgeParams);

// Assemble header
                sessionInfoLayout.addView(sessionTitle);
                sessionInfoLayout.addView(sessionTime);
                headerLayout.addView(classIcon);
                headerLayout.addView(sessionInfoLayout);
                headerLayout.addView(statusBadge);
                cardLayout.addView(headerLayout);

// Enhanced Attendance Section
                LinearLayout attendanceSection = new LinearLayout(getContext());
                attendanceSection.setOrientation(LinearLayout.VERTICAL);
                attendanceSection.setBackgroundResource(R.drawable.attendance_section_bg);
                attendanceSection.setPadding(24, 24, 24, 24);

// Section Header
                LinearLayout sectionHeaderLayout = new LinearLayout(getContext());
                sectionHeaderLayout.setOrientation(LinearLayout.HORIZONTAL);
                sectionHeaderLayout.setGravity(Gravity.CENTER_VERTICAL);
                sectionHeaderLayout.setPadding(0, 0, 0, 20);

                ImageView attendanceIcon = new ImageView(getContext());
                attendanceIcon.setImageResource(R.drawable.ic_people);
                attendanceIcon.setColorFilter(Color.parseColor("#1E3A8A"));
                LinearLayout.LayoutParams attendanceIconParams = new LinearLayout.LayoutParams(32, 32);
                attendanceIconParams.setMargins(0, 0, 16, 0);
                attendanceIcon.setLayoutParams(attendanceIconParams);

                TextView sectionHeader = new TextView(getContext());
                sectionHeader.setText("Students Present");
                sectionHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                sectionHeader.setTextColor(Color.parseColor("#1F2937"));
                sectionHeader.setTypeface(ResourcesCompat.getFont(getContext(), R.font.popins));

                sectionHeaderLayout.addView(attendanceIcon);
                sectionHeaderLayout.addView(sectionHeader);
                attendanceSection.addView(sectionHeaderLayout);

// Get students who attended
                String attendanceQuery = "SELECT u.firstName, u.lastName FROM attendance a " +
                        "JOIN users u ON a.userId = u.uid " +
                        "WHERE a.sessionId = ?";
                Cursor attCursor = db.rawQuery(attendanceQuery, new String[]{sessionId});

                int studentCount = 0;
                if (attCursor.moveToFirst()) {
                    do {
                        String firstName = attCursor.getString(attCursor.getColumnIndexOrThrow("firstName"));
                        String lastName = attCursor.getString(attCursor.getColumnIndexOrThrow("lastName"));

                        // Enhanced Student Item
                        LinearLayout studentItemLayout = new LinearLayout(getContext());
                        studentItemLayout.setOrientation(LinearLayout.HORIZONTAL);
                        studentItemLayout.setGravity(Gravity.CENTER_VERTICAL);
                        studentItemLayout.setBackgroundResource(R.drawable.student_item_bg);
                        studentItemLayout.setPadding(20, 16, 20, 16);
                        LinearLayout.LayoutParams studentItemParams = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        studentItemParams.setMargins(0, 0, 0, 12);
                        studentItemLayout.setLayoutParams(studentItemParams);

                        // Student Avatar
                        TextView studentAvatar = new TextView(getContext());
                        studentAvatar.setText(String.valueOf(firstName.charAt(0)).toUpperCase());
                        studentAvatar.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                        studentAvatar.setTextColor(Color.WHITE);
                        studentAvatar.setTypeface(ResourcesCompat.getFont(getContext(), R.font.poppins_bold));
                        studentAvatar.setBackgroundResource(R.drawable.student_avatar_bg);
                        studentAvatar.setGravity(Gravity.CENTER);
                        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(48, 48);
                        avatarParams.setMargins(0, 0, 16, 0);
                        studentAvatar.setLayoutParams(avatarParams);

                        // Student Info
                        LinearLayout studentInfoLayout = new LinearLayout(getContext());
                        studentInfoLayout.setOrientation(LinearLayout.VERTICAL);
                        LinearLayout.LayoutParams studentInfoParams = new LinearLayout.LayoutParams(
                                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                        studentInfoLayout.setLayoutParams(studentInfoParams);

                        TextView studentName = new TextView(getContext());
                        studentName.setText(firstName + " " + lastName);
                        studentName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                        studentName.setTextColor(Color.parseColor("#1F2937"));
                        studentName.setTypeface(ResourcesCompat.getFont(getContext(), R.font.popins));

                        TextView attendanceTime = new TextView(getContext());
                        attendanceTime.setText("Present");
                        attendanceTime.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                        attendanceTime.setTextColor(Color.parseColor("#10B981"));
                        attendanceTime.setTypeface(ResourcesCompat.getFont(getContext(), R.font.popins));

                        // Check mark icon
                        ImageView checkIcon = new ImageView(getContext());
                        checkIcon.setImageResource(R.drawable.ic_check_circle);
                        checkIcon.setColorFilter(Color.parseColor("#10B981"));
                        LinearLayout.LayoutParams checkParams = new LinearLayout.LayoutParams(24, 24);
                        checkIcon.setLayoutParams(checkParams);

                        studentInfoLayout.addView(studentName);
                        studentInfoLayout.addView(attendanceTime);
                        studentItemLayout.addView(studentAvatar);
                        studentItemLayout.addView(studentInfoLayout);
                        studentItemLayout.addView(checkIcon);
                        attendanceSection.addView(studentItemLayout);

                        studentCount++;
                    } while (attCursor.moveToNext());
                } else {
                    // Enhanced Empty State
                    LinearLayout emptyStateLayout = new LinearLayout(getContext());
                    emptyStateLayout.setOrientation(LinearLayout.VERTICAL);
                    emptyStateLayout.setGravity(Gravity.CENTER);
                    emptyStateLayout.setPadding(40, 40, 40, 40);

                    ImageView emptyIcon = new ImageView(getContext());
                    emptyIcon.setImageResource(R.drawable.ic_no_attendance);
                    emptyIcon.setColorFilter(Color.parseColor("#D1D5DB"));
                    LinearLayout.LayoutParams emptyIconParams = new LinearLayout.LayoutParams(64, 64);
                    emptyIconParams.setMargins(0, 0, 0, 16);
                    emptyIcon.setLayoutParams(emptyIconParams);

                    TextView emptyTitle = new TextView(getContext());
                    emptyTitle.setText("No Attendance Recorded");
                    emptyTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                    emptyTitle.setTextColor(Color.parseColor("#6B7280"));
                    emptyTitle.setTypeface(ResourcesCompat.getFont(getContext(), R.font.popins));
                    emptyTitle.setGravity(Gravity.CENTER);
                    emptyTitle.setPadding(0, 0, 0, 8);

                    TextView emptySubtitle = new TextView(getContext());
                    emptySubtitle.setText("No students marked attendance for this session.");
                    emptySubtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                    emptySubtitle.setTextColor(Color.parseColor("#9CA3AF"));
                    emptySubtitle.setTypeface(ResourcesCompat.getFont(getContext(), R.font.popins));
                    emptySubtitle.setGravity(Gravity.CENTER);

                    emptyStateLayout.addView(emptyIcon);
                    emptyStateLayout.addView(emptyTitle);
                    emptyStateLayout.addView(emptySubtitle);
                    attendanceSection.addView(emptyStateLayout);
                }
                attCursor.close();

// Add attendance count footer
                if (studentCount > 0) {
                    LinearLayout footerLayout = new LinearLayout(getContext());
                    footerLayout.setOrientation(LinearLayout.HORIZONTAL);
                    footerLayout.setGravity(Gravity.CENTER);
                    footerLayout.setBackgroundResource(R.drawable.footer_bg);
                    footerLayout.setPadding(20, 16, 20, 16);
                    LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    footerParams.setMargins(0, 16, 0, 0);
                    footerLayout.setLayoutParams(footerParams);

                    TextView countText = new TextView(getContext());
                    countText.setText("Total Present: " + studentCount + " student" + (studentCount != 1 ? "s" : ""));
                    countText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                    countText.setTextColor(Color.parseColor("#1E3A8A"));
                    countText.setTypeface(ResourcesCompat.getFont(getContext(), R.font.popins));

                    footerLayout.addView(countText);
                    attendanceSection.addView(footerLayout);
                }

                cardLayout.addView(attendanceSection);
                attendanceContainer.addView(cardLayout);} while (sessionCursor.moveToNext());
        } else {
            TextView noSession = new TextView(getContext());
            noSession.setText("No sessions for today.");
            noSession.setTextSize(18);
            noSession.setTextColor(getResources().getColor(android.R.color.darker_gray));
            noSession.setPadding(16, 32, 16, 16);
            attendanceContainer.addView(noSession);
        }

        sessionCursor.close();
        db.close();
    }
    }


