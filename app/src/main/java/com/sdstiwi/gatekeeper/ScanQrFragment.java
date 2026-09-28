package com.sdstiwi.gatekeeper;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ScanQrFragment extends Fragment {

    private static final String TAG = "ScanQrFragment";

    public ScanQrFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.scanqr_fragment, container, false);
        LinearLayout qrContainer = view.findViewById(R.id.qrContainer);

        SharedPreferences prefs = requireContext().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        String studentId = prefs.getString("uid", "");

        Log.d(TAG, "Student ID: " + studentId);
        LoginActivity.startSynchronization(requireContext());
        StudentQRCodeFetcher fetcher = new StudentQRCodeFetcher(requireContext());
        List<StudentQRCodeFetcher.QRCodeItem> qrCodes = fetcher.getActiveQRCodesForStudent(studentId);

        if (qrCodes.isEmpty()) {
            Log.d(TAG, "No active QR codes available.");
            TextView noQr = new TextView(getContext());
            noQr.setText("No active QR codes available.");
            noQr.setTextSize(16);
            noQr.setTextColor(Color.parseColor("#1E3A8A"));
            noQr.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.popins));
            noQr.setPadding(0, 32, 0, 0);
            qrContainer.addView(noQr);
        } else {
            Log.d(TAG, "Found " + qrCodes.size() + " active QR codes.");

            for (StudentQRCodeFetcher.QRCodeItem item : qrCodes) {
                Log.d(TAG, "Adding QR code for class: " + item.className);

                // Enhanced Container for each QR block
                LinearLayout card = new LinearLayout(getContext());
                card.setOrientation(LinearLayout.VERTICAL);
                card.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.enhanced_card_background));
                card.setPadding(40, 40, 40, 40);
                card.setElevation(12f);

                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                cardParams.setMargins(0, 0, 0, 32);
                card.setLayoutParams(cardParams);

                // --- Enhanced Header Section ---
                LinearLayout headerLayout = new LinearLayout(getContext());
                headerLayout.setOrientation(LinearLayout.HORIZONTAL);
                headerLayout.setGravity(Gravity.CENTER_VERTICAL);
                headerLayout.setPadding(0, 0, 0, 24);

                // Class Icon
                ImageView classIcon = new ImageView(getContext());
                classIcon.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_class));
                classIcon.setColorFilter(Color.parseColor("#1E3A8A"));
                LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(72, 72);
                iconParams.setMargins(0, 0, 24, 0);
                classIcon.setLayoutParams(iconParams);

                // Class Info Container
                LinearLayout classInfoLayout = new LinearLayout(getContext());
                classInfoLayout.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                classInfoLayout.setLayoutParams(infoParams);

                // Enhanced Class Label
                TextView classLabel = new TextView(getContext());
                classLabel.setText(item.className);
                classLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                classLabel.setTextColor(Color.parseColor("#1F2937"));
                classLabel.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.poppins_bold), Typeface.BOLD);
                classLabel.setPadding(0, 0, 0, 8);

                // Class Subtitle
                TextView classSubtitle = new TextView(getContext());
                classSubtitle.setText("Active session • Ready for check-in");
                classSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                classSubtitle.setTextColor(Color.parseColor("#6B7280"));
                classSubtitle.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.popins));

                // Status Badge
                TextView statusBadge = new TextView(getContext());
                statusBadge.setText("ACTIVE");
                statusBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
                statusBadge.setTextColor(Color.WHITE);
                statusBadge.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.poppins_bold), Typeface.BOLD);
                statusBadge.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.status_badge_bg));
                statusBadge.setPadding(16, 8, 16, 8);
                statusBadge.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                statusBadge.setLayoutParams(badgeParams);

                // Assemble header
                classInfoLayout.addView(classLabel);
                classInfoLayout.addView(classSubtitle);
                headerLayout.addView(classIcon);
                headerLayout.addView(classInfoLayout);
                headerLayout.addView(statusBadge);

                // --- Enhanced QR Container ---
                LinearLayout qrFraim = new LinearLayout(getContext());
                qrFraim.setOrientation(LinearLayout.VERTICAL);
                qrFraim.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.qr_container_bg));
                qrFraim.setPadding(32, 32, 32, 32);
                qrFraim.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams qrContainerParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                qrContainerParams.setMargins(0, 0, 0, 24);
                qrFraim.setLayoutParams(qrContainerParams);

                // QR Code Label
                TextView qrLabel = new TextView(getContext());
                qrLabel.setText("QR Code");
                qrLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                qrLabel.setTextColor(Color.parseColor("#374151"));
                qrLabel.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.popins), Typeface.BOLD);
                qrLabel.setGravity(Gravity.CENTER);
                qrLabel.setPadding(0, 0, 0, 16);

                // Enhanced QR ImageView with border
                FrameLayout qrFrame = new FrameLayout(getContext());
                qrFrame.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.qr_frame_bg));
                qrFrame.setPadding(16, 16, 16, 16);

                ImageView imageView = new ImageView(getContext());
                imageView.setImageBitmap(item.qrCode);
                imageView.setAdjustViewBounds(true);
                imageView.setMaxHeight(600);
                imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

                FrameLayout.LayoutParams imageParams = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                imageView.setLayoutParams(imageParams);

                qrFrame.addView(imageView);
                qrFraim.addView(qrLabel);
                qrFraim.addView(qrFrame);

                // --- Enhanced Scan Button ---
                LinearLayout buttonContainer = new LinearLayout(getContext());
                buttonContainer.setOrientation(LinearLayout.HORIZONTAL);
                buttonContainer.setGravity(Gravity.CENTER);

                Button scanButton = new Button(getContext());
                scanButton.setText("CHECK IN WITH QR");
                scanButton.setTextColor(Color.WHITE);
                scanButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                scanButton.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.poppins_bold), Typeface.BOLD);
                scanButton.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.enhanced_scan_button));
                scanButton.setPadding(48, 24, 48, 24);
                scanButton.setElevation(8f);
                scanButton.setLetterSpacing(0.1f);

                // Add ripple effect
                scanButton.setForeground(ContextCompat.getDrawable(requireContext(), R.drawable.button_ripple));

                LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                scanButton.setLayoutParams(buttonParams);

                scanButton.setOnClickListener(v -> {
                    Log.d(TAG, "Scan button clicked for class: " + item.className);

                    // Add button press animation
                    v.animate()
                            .scaleX(0.95f)
                            .scaleY(0.95f)
                            .setDuration(100)
                            .withEndAction(() -> {
                                v.animate()
                                        .scaleX(1f)
                                        .scaleY(1f)
                                        .setDuration(100);
                            });

                    markAttendance(studentId, item.className, item.qrCode, item.sessionId);
                });

                buttonContainer.addView(scanButton);

                // --- Footer Info ---
                LinearLayout footerLayout = new LinearLayout(getContext());
                footerLayout.setOrientation(LinearLayout.HORIZONTAL);
                footerLayout.setGravity(Gravity.CENTER);
                footerLayout.setPadding(0, 16, 0, 0);

                ImageView securityIcon = new ImageView(getContext());
                securityIcon.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_security));
                securityIcon.setColorFilter(Color.parseColor("#6B7280"));
                LinearLayout.LayoutParams securityIconParams = new LinearLayout.LayoutParams(32, 32);
                securityIconParams.setMargins(0, 0, 16, 0);
                securityIcon.setLayoutParams(securityIconParams);

                TextView footerText = new TextView(getContext());
                footerText.setText("Secure attendance • Live session");
                footerText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                footerText.setTextColor(Color.parseColor("#9CA3AF"));
                footerText.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.popins));

                footerLayout.addView(securityIcon);
                footerLayout.addView(footerText);

                // Assemble the complete card
                card.addView(headerLayout);
                card.addView(qrFraim);
                card.addView(buttonContainer);
                card.addView(footerLayout);

                qrContainer.addView(card);
            }
        }

        return view;
    }

    private void markAttendance(String studentId, String className, Bitmap qrCode, String sessionId) {
        DatabaseHelper dbHelper = new DatabaseHelper(requireContext());
        String result = dbHelper.recordAttendance(studentId, sessionId);

        switch (result) {
            case "RECORDED":
                Toast.makeText(requireContext(),
                        "Attendance recorded for " + className + ".", Toast.LENGTH_SHORT).show();
                break;
            case "ALREADY_CHECKED_IN":
                Toast.makeText(requireContext(),
                        "Already checked in for this session.", Toast.LENGTH_SHORT).show();
                break;
            case "QR_EXPIRED":
                Toast.makeText(requireContext(),
                        "This QR code has expired.", Toast.LENGTH_SHORT).show();
                break;
            case "SESSION_INACTIVE":
                Toast.makeText(requireContext(),
                        "Attendance is not currently open for this session.", Toast.LENGTH_SHORT).show();
                break;
            case "NOT_ENROLLED":
                Toast.makeText(requireContext(),
                        "You are not enrolled in this class.", Toast.LENGTH_SHORT).show();
                break;
            default:
                Toast.makeText(requireContext(),
                        "Unable to record attendance.", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private String fetchQRCodeIdForSession(String sessionId) {
        SQLiteDatabase db = new DatabaseHelper(requireContext()).getReadableDatabase();
        String qrCodeId = null;

        String query = "SELECT qrCodeId FROM qr_codes " +
                "WHERE targetId = ? " +
                "AND type = 'attendance' " +
                "AND isActive = 1";

        Cursor cursor = db.rawQuery(query, new String[]{sessionId});

        if (cursor.moveToFirst()) {
            qrCodeId = cursor.getString(cursor.getColumnIndex("qrCodeId"));
        } else {
            Log.d(TAG, "No QR code found for sessionId (targetId): " + sessionId);
        }

        cursor.close();
        db.close();

        return qrCodeId;
    }


    private String fetchQRCodeIdForClass(String classId) {
        SQLiteDatabase db = new DatabaseHelper(requireContext()).getReadableDatabase();
        String qrCodeId = null;

        String query = "SELECT qrCodeId FROM qr_codes WHERE classId = ?";
        Cursor cursor = db.rawQuery(query, new String[]{classId});

        if (cursor.moveToFirst()) {
            qrCodeId = cursor.getString(cursor.getColumnIndex("qrCodeId"));
        } else {
            Log.d(TAG, "No QR code found for classId: " + classId);
        }

        cursor.close();
        db.close();

        return qrCodeId;
    }

    private String fetchSessionDate(String sessionId) {
        SQLiteDatabase db = new DatabaseHelper(requireContext()).getReadableDatabase();
        String sessionDate = null;

        String query = "SELECT date FROM sessions WHERE sessionId = ?";
        Cursor cursor = db.rawQuery(query, new String[]{sessionId});

        if (cursor.moveToFirst()) {
            sessionDate = cursor.getString(cursor.getColumnIndex("date"));
        } else {
            Log.d(TAG, "No session date found for sessionId: " + sessionId);
        }

        cursor.close();
        db.close();

        return sessionDate;
    }
}
