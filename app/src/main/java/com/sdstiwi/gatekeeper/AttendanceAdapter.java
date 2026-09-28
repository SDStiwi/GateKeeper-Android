package com.sdstiwi.gatekeeper;

import android.icu.text.SimpleDateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.Date;
import java.util.List;

public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.ViewHolder> {

    private List<AttendanceRecord> records;

    public AttendanceAdapter(List<AttendanceRecord> records) {
        this.records = records;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, time, sessionId;

        public ViewHolder(View view) {
            super(view);
            name = view.findViewById(R.id.attendance_name);
            time = view.findViewById(R.id.attendance_time);
            sessionId = view.findViewById(R.id.attendance_session);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.attendance_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        AttendanceRecord record = records.get(position);
        holder.name.setText(record.getStudentName());
        holder.sessionId.setText("Session ID: " + record.getSessionId());

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        holder.time.setText("Marked at: " + sdf.format(new Date(record.getMarkedAt())));
    }

    @Override
    public int getItemCount() {
        return records.size();
    }
}

