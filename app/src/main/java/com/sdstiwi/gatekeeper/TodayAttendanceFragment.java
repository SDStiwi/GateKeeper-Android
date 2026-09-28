package com.sdstiwi.gatekeeper;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TodayAttendanceFragment extends Fragment {

    private RecyclerView recyclerView;
    private AttendanceAdapter adapter;

    public TodayAttendanceFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_today_attendance, container, false);
        recyclerView = view.findViewById(R.id.recyclerViewAttendance);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        List<String> sessionIds = fetchSessionsForToday();
        List<AttendanceRecord> attendanceList = fetchAttendanceForSessions(sessionIds);

        adapter = new AttendanceAdapter(attendanceList);
        recyclerView.setAdapter(adapter);
        LoginActivity.startSynchronization(requireContext());
        return view;
    }

    private String getTodayDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(new Date());
    }

    private List<String> fetchSessionsForToday() {
        List<String> sessionIds = new ArrayList<>();
        String today = getTodayDate();
        SQLiteDatabase db = new DatabaseHelper(requireContext()).getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT sessionId FROM sessions WHERE date = ?", new String[]{today});
        while (cursor.moveToNext()) {
            sessionIds.add(cursor.getString(cursor.getColumnIndex("sessionId")));
        }
        cursor.close();
        db.close();
        return sessionIds;
    }

    private List<AttendanceRecord> fetchAttendanceForSessions(List<String> sessionIds) {
        List<AttendanceRecord> records = new ArrayList<>();
        if (sessionIds.isEmpty()) return records;

        SQLiteDatabase db = new DatabaseHelper(requireContext()).getReadableDatabase();
        String placeholders = TextUtils.join(",", sessionIds).replaceAll("[^,]+", "?");
        String query = "SELECT a.attendanceId, a.userId, a.markedAt, " +
                "(u.firstName || ' ' || u.lastName) AS studentName, a.sessionId " +
                "FROM attendance a JOIN users u ON a.userId = u.uid " +
                "WHERE a.sessionId IN (" + placeholders + ")";

        Cursor cursor = db.rawQuery(query, sessionIds.toArray(new String[0]));
        while (cursor.moveToNext()) {
            AttendanceRecord record = new AttendanceRecord();
            record.setAttendanceId(cursor.getString(cursor.getColumnIndex("attendanceId")));
            record.setUserId(cursor.getString(cursor.getColumnIndex("userId")));
            record.setStudentName(cursor.getString(cursor.getColumnIndex("studentName")));
            record.setMarkedAt(cursor.getLong(cursor.getColumnIndex("markedAt")));
            record.setSessionId(cursor.getString(cursor.getColumnIndex("sessionId")));
            records.add(record);
        }

        cursor.close();
        db.close();
        return records;
    }
}
