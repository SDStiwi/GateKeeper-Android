package com.sdstiwi.gatekeeper;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView gifImageView = findViewById(R.id.gifImageView);
        Glide.with(this)
                .asGif()
                .load(R.drawable.gate)
                .override(400, 400)
                .into(gifImageView);

        new Handler().postDelayed(() -> {
            SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
            String role = prefs.getString("role", "");

            Intent intent;
            if ("teacher".equalsIgnoreCase(role)) {
                intent = new Intent(this, TeacherDashboard.class);
            } else if ("student".equalsIgnoreCase(role)) {
                intent = new Intent(this, StudentDashboard.class);
            } else {
                intent = new Intent(this, LoginActivity.class);
            }

            startActivity(intent);
            finish();
        }, 1000);
    }
}
