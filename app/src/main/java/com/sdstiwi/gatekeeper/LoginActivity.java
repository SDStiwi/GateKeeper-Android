package com.sdstiwi.gatekeeper;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Entry screen: authenticates against the local SQLite user table and routes to the
 * teacher or student dashboard.
 *
 * The bundled demo database is copied asynchronously first; all buttons stay disabled until
 * that finishes. In demo mode ({@link DemoConfig#DEMO_MODE}) no Firebase or Bluetooth access
 * is attempted.
 *
 * Note: credentials are compared as plain text, which is acceptable only because the data is
 * fixed demo content. A production version must store salted password hashes.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText editTextEmail, editTextPassword;
    private Button btnLogin, btnRegister, btnDemoTeacher, btnDemoStudent;
    static DatabaseHelper dbHelper;
    private boolean databaseReady = false;

    /**
     * Inflates the layout, then copies the demo database in the background and enables the
     * UI once it is ready. Also restores an existing session if one was saved.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        dbHelper = new DatabaseHelper(this);
        initializeUI();

        btnLogin.setEnabled(false);
        btnRegister.setEnabled(false);

        // Copy the bundled demo database before any login lookup.
        dbHelper.copyDatabaseAsync(success -> {
            databaseReady = success;
            btnLogin.setEnabled(success);
            btnRegister.setEnabled(success);
            btnDemoTeacher.setEnabled(success);
            btnDemoStudent.setEnabled(success);

            if (success) {
                DemoDataSeeder.seed(this);
                fillDemoTeacher();
                checkIfUserAlreadyLoggedIn();
                Toast.makeText(this, "Demo data ready — try the prefilled account.", Toast.LENGTH_SHORT).show();
            } else {
                showError("Could not load the bundled demo database.");
            }
        });
    }

    private void initializeUI() {
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        btnDemoTeacher = findViewById(R.id.btnDemoTeacher);
        btnDemoStudent = findViewById(R.id.btnDemoStudent);

        btnLogin.setOnClickListener(v -> loginUser());
        btnRegister.setOnClickListener(v -> openRegisterActivity());
        btnDemoTeacher.setOnClickListener(v -> fillDemoTeacher());
        btnDemoStudent.setOnClickListener(v -> fillDemoStudent());
    }

    private void fillDemoTeacher() {
        editTextEmail.setText(DemoConfig.TEACHER_EMAIL);
        editTextPassword.setText(DemoConfig.TEACHER_PASSWORD);
        editTextEmail.setSelection(editTextEmail.length());
    }

    private void fillDemoStudent() {
        editTextEmail.setText(DemoConfig.STUDENT_EMAIL);
        editTextPassword.setText(DemoConfig.STUDENT_PASSWORD);
        editTextEmail.setSelection(editTextEmail.length());
    }

    /**
     * Validates the form, checks the credentials and account state (approved and active),
     * stores the session and navigates to the role-specific dashboard.
     */
    private void loginUser() {
        if (!databaseReady) {
            showError("Demo data is still loading.");
            return;
        }

        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showToast("Please enter email and password.");
            return;
        }

        btnLogin.setEnabled(false);

        Users user = dbHelper.getUserByEmail(email);

        if (user == null || !user.getPassword().equals(password)) {
            showError("Invalid email or password.");
            return;
        }

        if (!"approved".equalsIgnoreCase(user.getApprovalStatus())) {
            showError("This demo account is not approved.");
            return;
        }

        if (user.getIsActive() == 0) {
            showError("This demo account is inactive.");
            return;
        }

        saveUserSession(user);
        dbHelper.updateLastLogin(user.getUid());

        // No Firebase account, network, or ESP32 is required in demo mode.
        if (!DemoConfig.DEMO_MODE) {
            SyncHelper.syncUserToRealtimeDB(user);
            SyncHelper.syncUserToFirebaseAuth(user);
        }

        navigateToDashboard(user.getRole());
    }

    /**
     * Persists the logged-in user's uid, email and role in the {@code UserSession} preferences.
     * Other screens read these values to know who is signed in.
     */
    private void saveUserSession(Users user) {
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        prefs.edit()
                .putString("uid", user.getUid())
                .putString("email", user.getEmail())
                .putString("role", user.getRole())
                .apply();
    }

    /**
     * Starts the dashboard matching {@code role} ({@code "teacher"} or {@code "student"}) and
     * closes this activity so Back does not return to the login form.
     */
    private void navigateToDashboard(String role) {
        Intent intent;
        if ("teacher".equalsIgnoreCase(role)) {
            intent = new Intent(this, TeacherDashboard.class);
        } else if ("student".equalsIgnoreCase(role)) {
            intent = new Intent(this, StudentDashboard.class);
        } else {
            showError("This demo role has no dashboard.");
            return;
        }

        startActivity(intent);
        finish();
    }

    private void openRegisterActivity() {
        startActivity(new Intent(this, RegisterActivity.class));
    }

    /**
     * Skips the login form when a previously saved session still points at an active user.
     */
    private void checkIfUserAlreadyLoggedIn() {
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        String userUid = prefs.getString("uid", null);
        if (userUid != null) {
            Users user = dbHelper.getUserByUid(userUid);
            if (user != null && user.getIsActive() == 1) {
                navigateToDashboard(user.getRole());
            }
        }
    }

    private void showError(String message) {
        btnLogin.setEnabled(databaseReady);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        btnLogin.setEnabled(true);
    }

    /**
     * Triggers a full Firestore sync. Does nothing in demo mode, which is offline-first.
     * Kept as a static entry point because several fragments call it.
     */
    public static void startSynchronization(Context context) {
        // Kept for existing fragments. Demo mode is offline-first.
        if (DemoConfig.DEMO_MODE) return;
        SyncManager.getInstance().syncAllData(context);
    }
}
