package com.sdstiwi.gatekeeper;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sdstiwi.gatekeeper.R;

import java.util.List;

public class ClassAdapter extends RecyclerView.Adapter<ClassAdapter.ViewHolder> {
    private List<ClassModel> classList;
    private Context context;
    private DatabaseHelper databaseHelper;

    public ClassAdapter(List<ClassModel> classList, Context context, DatabaseHelper databaseHelper) {
        this.classList = classList;
        this.context = context;
        this.databaseHelper = databaseHelper;
    }

    public void updateList(List<ClassModel> newList) {
        classList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_class, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ClassModel classModel = classList.get(position);
        holder.className.setText(classModel.getClassName());
        holder.courseCode.setText("Course: " + classModel.getCourseCode());

        // Get the teacher's ID from the class model
        String teacherId = classModel.getTeacherId();
        String classId = classModel.getClassId();

        holder.enrollButton.setOnClickListener(v -> {
            enrollStudent(getCurrentStudentId(), classId, teacherId);
        });
    }
    private String getCurrentStudentId() {
        SharedPreferences sharedPreferences = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        return sharedPreferences.getString("uid", ""); // Replace "uid" with the actual key storing student ID
    }

    private void enrollStudent(String studentId, String classId, String teacherId) {
        boolean success = databaseHelper.enrollStudent(studentId, classId, teacherId);

        if (success) {
            Toast.makeText(context, "Enrollment request sent!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Enrollment request failed!", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public int getItemCount() {
        return classList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView className, courseCode;
        Button enrollButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            className = itemView.findViewById(R.id.className);
            courseCode = itemView.findViewById(R.id.courseCode);
            enrollButton = itemView.findViewById(R.id.enrollButton);
        }
    }



}
