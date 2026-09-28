package com.sdstiwi.gatekeeper;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.sdstiwi.gatekeeper.R;

import com.sdstiwi.gatekeeper.DatabaseHelper;

import java.util.UUID;

public class RegisterActivity extends AppCompatActivity {

    private EditText editTextFirstName, editTextLastName, editTextEmail, editTextPassword,editTextDepartement;
    private Spinner spinnerSpeciality;
    private RadioGroup radioGroupRole;
    private Button btnRegister;
    private TextView  signinlink;

    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize UI Components
        editTextFirstName = findViewById(R.id.editTextFirstName);
        editTextLastName = findViewById(R.id.editTextLastName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextDepartement = findViewById(R.id.departement); // Department field
        spinnerSpeciality = findViewById(R.id.spinnerSpeciality);
        radioGroupRole = findViewById(R.id.radioGroupRole);
        btnRegister = findViewById(R.id.btnRegister);
        signinlink = findViewById(R.id.signinlink);

        signinlink.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);  // Navigate to LoginActivity
            finish();  // Close the RegisterActivity
        });
        // Initialize SQLite Database Helper
        dbHelper = new DatabaseHelper(this);

        // Handle Role Selection (Show Speciality Spinner for students)
        radioGroupRole.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioStudent) {
                spinnerSpeciality.setVisibility(View.VISIBLE);
            } else {
                spinnerSpeciality.setVisibility(View.GONE);
            }
        });

        // Handle Registration Click
        btnRegister.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        // Get input data
        String firstName = editTextFirstName.getText().toString().trim();
        String lastName = editTextLastName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String department = editTextDepartement.getText().toString();

        // Get selected speciality (if student)
        String speciality = "";
        if (spinnerSpeciality.getVisibility() == View.VISIBLE) {
            speciality = spinnerSpeciality.getSelectedItem().toString();
        }

        // Validate role selection
        int selectedRoleId = radioGroupRole.getCheckedRadioButtonId();
        if (selectedRoleId == -1) {
            showToast("Select a role!");
            return;
        }
        RadioButton selectedRoleButton = findViewById(selectedRoleId);
        String role = selectedRoleButton.getText().toString().toLowerCase(); // "teacher" or "student"

        // Validate input fields
        if (TextUtils.isEmpty(firstName) || TextUtils.isEmpty(lastName) ||
                TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(department)) {
            showToast("All fields are required!");
            return;
        }

        if (role.equals("student") && speciality.isEmpty()) {
            showToast("Speciality is required for students!");
            return;
        }

        // Set approval status to 'approved' by default
        String approvalStatus = "approved";
        int isActive = 1; // Default active status

        // Generate a unique user ID (UID) for the user
        String uid = UUID.randomUUID().toString();

        // Account registration is separate from course enrollment.
        // New accounts are active immediately; course enrollment still uses its own status workflow.
        boolean insertSuccess = dbHelper.insertOrUpdateUser(uid, email, password, firstName, lastName,
                speciality, role, department, approvalStatus, isActive);

        if (!insertSuccess) {
            showToast("Registration failed. Email might already be in use.");
            return;
        }

        getSharedPreferences("UserSession", MODE_PRIVATE)
                .edit()
                .putString("uid", uid)
                .putString("role", role)
                .apply();

        showToast("Registration successful!");
        navigateToDashboard(role);

    }

    private void showToast(String message) {
        Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    private void navigateToDashboard(String role) {
        Intent intent = role.equals("teacher") ?
                new Intent(RegisterActivity.this, TeacherDashboard.class) :
                new Intent(RegisterActivity.this, StudentDashboard.class);
        startActivity(intent);
        finish();
    }
}
