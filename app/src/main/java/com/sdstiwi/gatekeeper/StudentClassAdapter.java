package com.sdstiwi.gatekeeper;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class StudentClassAdapter extends RecyclerView.Adapter<StudentClassAdapter.ViewHolder> {
    private List<StudentClassModel> classList;
    private Context context;

    public StudentClassAdapter(List<StudentClassModel> classList, Context context) {
        this.classList = classList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_student_class, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StudentClassModel classModel = classList.get(position);

        holder.className.setText(classModel.getClassName());
        holder.courseCode.setText(classModel.getCourseCode());
        holder.startTime.setText(classModel.getStartTime());
        holder.roomname.setText(classModel.getRoomname());
        holder.endTime.setText(classModel.getEndTime());
    }

    @Override
    public int getItemCount() {
        return classList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView className, courseCode, roomname, startTime, endTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            className = itemView.findViewById(R.id.classIdTextView);
            roomname=itemView.findViewById(R.id.roomname);
            courseCode = itemView.findViewById(R.id.sessionIdTextView);
            startTime = itemView.findViewById(R.id.startTimeTextView);
            endTime = itemView.findViewById(R.id.endTimeTextView);
        }
    }
}
