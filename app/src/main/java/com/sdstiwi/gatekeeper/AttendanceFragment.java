package com.sdstiwi.gatekeeper;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.fragment.app.Fragment;

import com.google.zxing.integration.android.IntentIntegrator;

import org.json.JSONException;
import org.json.JSONObject;

public class AttendanceFragment extends Fragment {
    private ImageView qrCodeImageView;
    private Handler qrCheckHandler = new Handler();
    private Runnable qrCheckRunnable;
    private BluetoothHelper bluetoothHelper;
    private final String esp32DeviceAddress = DemoConfig.ESP32_DEVICE_ADDRESS;

    public AttendanceFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_qrcode, container, false);
        LoginActivity.startSynchronization(requireContext());
        Context context = requireContext();
        bluetoothHelper = new BluetoothHelper(context);

        // Get teacherId from SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        String teacherId = prefs.getString("uid", "");

        // Log today's classes properly
        SessionManager sessionManager = new SessionManager(context);
        sessionManager.logTodaysClassesForTeacher(teacherId); // Use correct ID here

        // Disconnect button logic
       // Button btnDisconnect = rootView.findViewById(R.id.btn_disconnect);
        //btnDisconnect.setOnClickListener(v -> {
           // bluetoothHelper.disconnect();
            //Log.d("BluetoothConnection", "Disconnect button clicked. Bluetooth disconnected.");
       // });

        // QR scan logic
        qrCodeImageView = rootView.findViewById(R.id.qrCodeImageView);
        TextView classNameTextView = rootView.findViewById(R.id.classNameTextView);

        Button btnScanQRCode = rootView.findViewById(R.id.btnScanQRCode);
        btnScanQRCode.setOnClickListener(v -> {
            qrCodeImageView.setDrawingCacheEnabled(true);
            qrCodeImageView.buildDrawingCache();
            Bitmap qrBitmap = qrCodeImageView.getDrawingCache();

            if (qrBitmap != null) {
                String decodedText = decodeQRCode(qrBitmap);
                if (decodedText != null) {
                    Toast.makeText(context, "Decoded QR: " + decodedText, Toast.LENGTH_SHORT).show();
                    handleQRCode(decodedText);
                } else {
                    Toast.makeText(context, "Failed to decode QR code.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "QR image not available.", Toast.LENGTH_SHORT).show();
            }
        });

        // Door close button
        Button btnCloseDoor = rootView.findViewById(R.id.btnCloseDoor);
        btnCloseDoor.setOnClickListener(v -> closeDoor());

        // Load and display existing QR code (if any)
        QRWithClassInfo info = sessionManager.getValidQRCodeWithClassForTeacher(teacherId);
        if (info != null) {
            qrCodeImageView.setImageBitmap(info.qrCode);
            classNameTextView.setText("Class: " + info.className);
        }

        // ⏱ Auto-check every 60 seconds
        qrCheckRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d("SessionManagerCall", "Periodic QR check triggered.");
                QRWithClassInfo autoInfo = sessionManager.getValidQRCodeWithClassForTeacher(teacherId);
                if (autoInfo != null) {
                    qrCodeImageView.setImageBitmap(autoInfo.qrCode);
                    classNameTextView.setText("Class: " + autoInfo.className);
                } else {
                    Bitmap newQr = sessionManager.checkAndCreateSessionForTeacher(teacherId);
                    if (newQr != null) {
                        qrCodeImageView.setImageBitmap(newQr);
                        classNameTextView.setText("Class: (new QR)");
                        Toast.makeText(context, "Auto: QR generated and displayed.", Toast.LENGTH_SHORT).show();
                    }
                }

                // Re-run every 60 seconds
                qrCheckHandler.postDelayed(this, 60000);
            }
        };

        // Start periodic check
        qrCheckHandler.post(qrCheckRunnable);

        return rootView;
    }

    // Method to close the door by sending the "LOCK" command
    private void closeDoor() {
        if (DemoConfig.DEMO_MODE) {
            Toast.makeText(getContext(), "Demo: door locked successfully", Toast.LENGTH_SHORT).show();
            return;
        }

        if (bluetoothHelper == null) {
            Toast.makeText(getContext(), "Bluetooth not available.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (esp32DeviceAddress == null || esp32DeviceAddress.trim().isEmpty()) {
            Toast.makeText(getContext(), "ESP32 Bluetooth address is not configured.", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean connected = bluetoothHelper.connectToDevice(esp32DeviceAddress);
        if (!connected) {
            Toast.makeText(getContext(), "Door: ESP32 unavailable.", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean sent = bluetoothHelper.sendData("LOCK");
        Toast.makeText(getContext(), sent ? "Door: lock command sent." : "Door: lock command failed.", Toast.LENGTH_SHORT).show();
    }

    private String decodeQRCode(Bitmap bitmap) {
        int[] intArray = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(intArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        com.google.zxing.LuminanceSource source = new com.google.zxing.RGBLuminanceSource(bitmap.getWidth(), bitmap.getHeight(), intArray);
        com.google.zxing.BinaryBitmap binaryBitmap = new com.google.zxing.BinaryBitmap(new com.google.zxing.common.HybridBinarizer(source));
        com.google.zxing.Reader reader = new com.google.zxing.MultiFormatReader();
        try {
            com.google.zxing.Result result = reader.decode(binaryBitmap);
            return result.getText();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void handleQRCode(String decodedText) {
        Log.d("QRCodeScan", "Decoded Text: " + decodedText);

        if (DemoConfig.DEMO_MODE) {
            String type = decodedText.toLowerCase().contains("close") ? "close" : "attendance";
            String message = "close".equals(type)
                    ? "Demo: door lock simulated"
                    : "Demo: QR decoded. Use the student Check-In screen to record attendance.";
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            return;
        }

        try {
            decodedText = decodedText.trim();
            Log.d("QRCodeScan", "Trimmed Decoded Text: " + decodedText);

            String type = "";

            if (decodedText.startsWith("{") && decodedText.endsWith("}")) {
                try {
                    JSONObject jsonObject = new JSONObject(decodedText);
                    type = jsonObject.getString("type").toLowerCase();
                    Log.d("QRCodeScan", "Parsed JSON successfully. Type: " + type);
                } catch (JSONException e) {
                    Log.e("QRCodeScan", "Failed to parse JSON. Treating as simple text.");
                    type = decodedText.split("-")[0].toLowerCase();
                }
            } else {
                type = decodedText.split("-")[0].toLowerCase();
            }

            Log.d("QRCodeScan", "Extracted Type: " + type);

            if (bluetoothHelper == null) {
                Log.e("QRCodeScan", "BluetoothHelper not initialized.");
                Toast.makeText(getContext(), "Bluetooth not available.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (esp32DeviceAddress == null || esp32DeviceAddress.trim().isEmpty()) {
                Toast.makeText(getContext(), "ESP32 Bluetooth address is not configured.", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean connected = bluetoothHelper.connectToDevice(esp32DeviceAddress);
            if (!connected) {
                Log.e("QRCodeScan", "Failed to connect to ESP32_Lock.");
                Toast.makeText(getContext(), "Door: ESP32 unavailable.", Toast.LENGTH_SHORT).show();
                return;
            }

            if ("attendance".equals(type)) {
                boolean sent = bluetoothHelper.sendData("UNLOCK");
                Toast.makeText(getContext(), sent ? "Door: unlock command sent." : "Door: unlock command failed.", Toast.LENGTH_SHORT).show();
                Log.d("QRCodeScan", "UNLOCK command result: " + sent);
            } else if ("close".equals(type)) {
                boolean sent = bluetoothHelper.sendData("LOCK");
                Toast.makeText(getContext(), sent ? "Door: lock command sent." : "Door: lock command failed.", Toast.LENGTH_SHORT).show();
                Log.d("QRCodeScan", "LOCK command result: " + sent);
            } else {
                Toast.makeText(getContext(), "Unknown QR Code type.", Toast.LENGTH_SHORT).show();
                Log.e("QRCodeScan", "Unknown QR Code type: " + type);
            }

        } catch (Exception e) {
            Log.e("QRCodeScan", "Error handling QR code: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Invalid QR Code Format.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Stop the handler to prevent memory leaks
        qrCheckHandler.removeCallbacks(qrCheckRunnable);
    }
}
