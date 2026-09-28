package com.sdstiwi.gatekeeper;

import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.Calendar;
import java.util.List;

public class TimeTableFragment extends Fragment {

    private TableLayout tableLayout;
    private List<Session> weeklySessions;

    private static final String TAG = "TimeTableFragment"; // For logging

    public TimeTableFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_timetable, container, false);
        LoginActivity.startSynchronization(requireContext());
        tableLayout = rootView.findViewById(R.id.tableLayoutTimetable);

        // Fetch weekly timetable data (example, pass the teacherId)
        DatabaseHelper dbHelper = new DatabaseHelper(getContext());
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("UserSession", getContext().MODE_PRIVATE);
        String userID = sharedPreferences.getString("uid", "");
        String teacherId = userID; // Replace with dynamic teacherId
        weeklySessions = dbHelper.getWeeklyTimetableForTeacher(teacherId);

        Log.d(TAG, "Fetched weekly timetable for teacher ID: " + teacherId);
        Log.d(TAG, "Total sessions fetched: " + (weeklySessions != null ? weeklySessions.size() : 0));

        // Give every slot a clean "Free" default look, then fill in real sessions
        resetTimetable();
        populateTimetable(weeklySessions);
        highlightToday();

        return rootView;
    }

    /** Resets every non-header, non-time cell to a neutral "Free" state. */
    private void resetTimetable() {
        for (int row = 1; row < tableLayout.getChildCount(); row++) {
            TableRow tableRow = (TableRow) tableLayout.getChildAt(row);
            for (int col = 1; col < tableRow.getChildCount(); col++) {
                TextView cell = (TextView) tableRow.getChildAt(col);
                cell.setText("Free");
                cell.setTypeface(cell.getTypeface(), Typeface.NORMAL);
                cell.setTextColor(ContextCompat.getColor(requireContext(), R.color.nav_unselected));
                cell.setBackground(TimetableColorUtil.freeBackground(requireContext()));
            }
        }
    }

    /** Tints today's header column so the current day stands out at a glance. */
    private void highlightToday() {
        int column = getColumnIndexForDay(currentDayAbbreviation());
        if (column == -1 || tableLayout.getChildCount() == 0) {
            return;
        }
        TableRow headerRow = (TableRow) tableLayout.getChildAt(0);
        if (column >= headerRow.getChildCount()) {
            return;
        }
        TextView headerCell = (TextView) headerRow.getChildAt(column);
        headerCell.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.secondary_color));
        headerCell.setText(headerCell.getText() + " •");
    }

    private String currentDayAbbreviation() {
        int dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        switch (dayOfWeek) {
            case Calendar.MONDAY: return "Mon";
            case Calendar.TUESDAY: return "Tue";
            case Calendar.WEDNESDAY: return "Wed";
            case Calendar.THURSDAY: return "Thu";
            case Calendar.FRIDAY: return "Fri";
            default: return ""; // weekend: nothing to highlight
        }
    }

    private void populateTimetable(List<Session> weeklySessions) {
        if (weeklySessions == null || weeklySessions.isEmpty()) {
            Log.d(TAG, "No sessions available to populate.");
            return;
        }

        // Iterate through all the time slots and populate the table
        for (Session session : weeklySessions) {
            String day = session.getDate(); // "Mon", "Tue", etc.
            String room = session.getRoomName();
            String className = session.getClassName(); // e.g., "Math 101"
            String time = session.getStartTime() + "-" + session.getEndTime(); // e.g., "09:00-10:00"

            Log.d(TAG, "Session details - Day: " + day + ", Time: " + time + ", Class: " + className);

            // Find the correct row based on time slot (row 2 for "09:00-10:00", etc.)
            int rowIndex = getRowIndexForTime(time);
            if (rowIndex != -1) {
                // Find the column for the correct day (Mon, Tue, etc.)
                int columnIndex = getColumnIndexForDay(day);
                if (columnIndex != -1) {
                    // Get the TextView for the cell and style it for this course
                    TableRow row = (TableRow) tableLayout.getChildAt(rowIndex);
                    TextView cell = (TextView) row.getChildAt(columnIndex);
                    applySessionStyle(cell, className, room);

                    Log.d(TAG, "Populated class: " + className + " at row " + rowIndex + ", column " + columnIndex);
                } else {
                    Log.w(TAG, "Invalid column index for day: " + day);
                }
            } else {
                Log.w(TAG, "Invalid row index for time slot: " + time);
            }
        }
    }

    /** Applies a course-colored background and a bold title / muted room two-line label to a cell. */
    private void applySessionStyle(TextView cell, String className, String room) {
        int accentColor = TimetableColorUtil.colorFor(requireContext(), className);
        cell.setBackground(TimetableColorUtil.sessionBackground(requireContext(), accentColor));

        String fullText = className + "\n" + room;
        SpannableString styled = new SpannableString(fullText);
        styled.setSpan(new StyleSpan(Typeface.BOLD), 0, className.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        styled.setSpan(new ForegroundColorSpan(accentColor), 0, className.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        styled.setSpan(new RelativeSizeSpan(0.85f), className.length(), fullText.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        styled.setSpan(new ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.text_secondary)),
                className.length(), fullText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        cell.setText(styled);
    }

    private int getRowIndexForTime(String time) {
        // Map the time to the row index dynamically
        if (time.equals("09:00-10:00") || time.equals("09:00-10:30")) {
            return 1; // Row index 1 for 09:00-10:30
        } else if (time.equals("10:00-11:00") || time.equals("10:30-11:30")) {
            return 2; // Row index 2 for 10:00-11:30
        } else if (time.equals("11:00-12:00") || time.equals("11:00-12:30")) {
            return 3; // Row index 3 for 11:00-12:30
        } else if (time.equals("14:00-15:30") || time.equals("14:00-15:00")) {
            return 4; // Row index 4 for the afternoon slot
        } else if (time.equals("15:30-17:00")) {
            return 5; // Row index 5 for the late afternoon slot
        }
        // Add more cases if needed
        else {
            return -1; // Return -1 if no matching row
        }
    }

    private int getColumnIndexForDay(String day) {
        // Map the day to the correct column (Mon -> 1, Tue -> 2, etc.)
        switch (day) {
            case "Mon":
                return 1; // Column index 1 (Monday)
            case "Tue":
                return 2; // Column index 2 (Tuesday)
            case "Wed":
                return 3; // Column index 3 (Wednesday)
            case "Thu":
                return 4; // Column index 4 (Thursday)
            case "Fri":
                return 5; // Column index 5 (Friday)
            default:
                return -1;
        }
    }
}
