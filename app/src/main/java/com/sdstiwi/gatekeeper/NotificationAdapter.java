package com.sdstiwi.gatekeeper;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.sdstiwi.gatekeeper.R;


import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
    private List<NotificationModel> notificationList;
    private Context context;
    private DatabaseHelper databaseHelper;

    public NotificationAdapter(List<NotificationModel> notificationList, Context context, DatabaseHelper databaseHelper) {
        this.notificationList = notificationList;
        this.context = context;
        this.databaseHelper = databaseHelper;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationModel notification = notificationList.get(position);

        holder.messageTextView.setText(notification.getMessage());

        // Format timestamp
        String formattedDate = formatTimestamp(notification.getCreatedAt());
        holder.timestampTextView.setText(formattedDate);

        // Set default background color
        int backgroundColor;
        int textColor;

        switch (notification.getNotificationType()) {
            case "enrollement":
                backgroundColor = context.getResources().getColor(R.color.light_blue);
                textColor = context.getResources().getColor(R.color.black);

                if (!notification.isRead()) {
                    holder.acceptButton.setVisibility(View.VISIBLE);
                    holder.rejectButton.setVisibility(View.VISIBLE);
                    holder.acceptButton.setOnClickListener(v -> handleEnrollment(notification, true, position, holder));
                    holder.rejectButton.setOnClickListener(v -> handleEnrollment(notification, false, position, holder));
                } else {
                    holder.acceptButton.setVisibility(View.GONE);
                    holder.rejectButton.setVisibility(View.GONE);
                    holder.messageTextView.append("\n\nAlready responded");
                }
                break;

            case "reply":
                backgroundColor = context.getResources().getColor(R.color.replyBg);
                textColor = context.getResources().getColor(R.color.replyText);
                holder.acceptButton.setVisibility(View.GONE);
                holder.rejectButton.setVisibility(View.GONE);
                break;

            case "general":
                backgroundColor = context.getResources().getColor(R.color.generalNotificationBg);
                textColor = context.getResources().getColor(R.color.generalNotificationText);
                holder.acceptButton.setVisibility(View.GONE);
                holder.rejectButton.setVisibility(View.GONE);
                break;

            default:
                backgroundColor = context.getResources().getColor(R.color.defaultNotificationBg);
                textColor = context.getResources().getColor(R.color.defaultNotificationText);
                holder.acceptButton.setVisibility(View.GONE);
                holder.rejectButton.setVisibility(View.GONE);
                break;
        }

        // Apply styles
        holder.itemView.setBackgroundColor(backgroundColor);
        holder.messageTextView.setTextColor(textColor);
        holder.timestampTextView.setTextColor(textColor);

        // General/reply notifications become read when the user opens them.
        // Enrollment requests stay unread until the teacher responds.
        if (!"enrollement".equals(notification.getNotificationType())) {
            holder.itemView.setOnClickListener(v -> {
                if (!notification.isRead()) {
                    databaseHelper.markNotificationAsRead(notification.getNotificationId());
                    notification.setRead(true);
                    notifyItemChanged(position);
                }
            });
        } else {
            holder.itemView.setOnClickListener(null);
        }
    }

    private void handleEnrollment(NotificationModel notification, boolean isAccepted, int position, ViewHolder holder) {
        String studentId = notification.getSender();
        String classId = notification.getClassId();

        // Fetch class details
        ClassModel classModel = databaseHelper.getClassById(classId);
        if (classModel == null) {
            Toast.makeText(context, "Class not found!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Fetch enrollment ID
        String enrollmentId = databaseHelper.getEnrollmentId(studentId, classId);
        if (enrollmentId == null) {
            Toast.makeText(context, "Enrollment record not found!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update enrollment status
        String newStatus = isAccepted ? "approved" : "rejected";
        boolean success = databaseHelper.updateEnrollmentStatus(enrollmentId, newStatus, studentId, classModel);

        if (success) {
            // Mark notification as read in database
            databaseHelper.markNotificationAsRead(notification.getNotificationId());

            // Hide buttons and update UI
            holder.acceptButton.setVisibility(View.GONE);
            holder.rejectButton.setVisibility(View.GONE);
            holder.messageTextView.append("\n\nRequest " + newStatus);

            // Update the notification object locally
            notification.setRead(true);
            notification.setMessage(notification.getMessage() + "\n\nRequest " + newStatus);

            // Notify RecyclerView about data change
            notifyItemChanged(position);

            // Prevent duplicate notifications
            if (!databaseHelper.isNotificationAlreadySent(studentId, classId, newStatus)) {
                String message = "Your enrollment request for " + classModel.getClassName() + " (" + classModel.getCourseCode() + ") was " + newStatus + ".";
                String type = "reply";

                NotificationModel responseNotification = new NotificationModel(
                        null, studentId, message, false, String.valueOf(System.currentTimeMillis()), classId, classModel.getTeacherId(), type
                );

                databaseHelper.insertNotification(responseNotification);
            }

            Toast.makeText(context, isAccepted ? "Request approved!" : "Request rejected!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Failed to update enrollment.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView, timestampTextView;
        Button acceptButton, rejectButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
            acceptButton = itemView.findViewById(R.id.acceptButton);
            rejectButton = itemView.findViewById(R.id.rejectButton);
        }
    }

    // Helper method to format timestamps
    private String formatTimestamp(String timestamp) {
        try {
            long timeInMillis = Long.parseLong(timestamp);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            return sdf.format(timeInMillis);
        } catch (Exception e) {
            return "Unknown Date";
        }
    }
}
