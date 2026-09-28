package com.sdstiwi.gatekeeper;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.sdstiwi.gatekeeper.R;

import com.sdstiwi.gatekeeper.Session;

import java.util.List;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder> {

    private List<Session> sessionList;

    public ScheduleAdapter(List<Session> sessionList) {
        this.sessionList = sessionList;
    }

    @Override
    public ScheduleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_schedule, parent, false);
        return new ScheduleViewHolder(view);
    }


    @Override
    public void onBindViewHolder(ScheduleViewHolder holder, int position) {
        Session session = sessionList.get(position);
        holder.sessionIdTextView.setText(session.getSessionId());
        holder.classIdTextView.setText(session.getClassName());
        holder.roomname.setText(session.getRoomName());
        holder.startTimeTextView.setText(session.getStartTime() != null ? session.getStartTime() : "N/A");
        holder.endTimeTextView.setText(session.getEndTime() != null ? session.getEndTime() : "N/A");
    }


    @Override
    public int getItemCount() {
        return sessionList.size();
    }

    public static class ScheduleViewHolder extends RecyclerView.ViewHolder {
        TextView sessionIdTextView;
        TextView classIdTextView;
        TextView startTimeTextView;
        TextView endTimeTextView;
        TextView roomname;
        public ScheduleViewHolder(View itemView) {
            super(itemView);
            sessionIdTextView = itemView.findViewById(R.id.sessionIdTextView);
            classIdTextView = itemView.findViewById(R.id.classIdTextView);
            roomname = itemView.findViewById(R.id.roomname);
            startTimeTextView = itemView.findViewById(R.id.startTimeTextView);
            endTimeTextView = itemView.findViewById(R.id.endTimeTextView);
        }
    }
}
