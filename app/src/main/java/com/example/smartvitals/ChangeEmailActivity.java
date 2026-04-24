package com.example.smartvitals;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ChangeEmailActivity extends AppCompatActivity {

    private EditText etNewEmail, etCurrentPassword;
    private Button btnSaveNewEmail, btnCheckVerifiedEmail;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String originalEmail = "";

    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_email);

        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        etNewEmail = findViewById(R.id.etNewEmail);
        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        btnSaveNewEmail = findViewById(R.id.btnSaveNewEmail);
        btnCheckVerifiedEmail = findViewById(R.id.btnCheckVerifiedEmail);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            originalEmail = user.getEmail();
            etNewEmail.setText(user.getEmail());
        }

        btnSaveNewEmail.setOnClickListener(v -> sendVerificationEmail());

        btnCheckVerifiedEmail.setOnClickListener(v -> checkVerifiedEmailChange());
    }

    private void sendVerificationEmail() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null || currentUser.getEmail() == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentEmail = currentUser.getEmail().trim();
        String newEmail = etNewEmail.getText().toString().trim();
        String currentPassword = etCurrentPassword.getText().toString().trim();

        if (TextUtils.isEmpty(newEmail)) {
            etNewEmail.setError("New email is required");
            etNewEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            etNewEmail.setError("Enter a valid email");
            etNewEmail.requestFocus();
            return;
        }

        if (newEmail.equalsIgnoreCase(currentEmail)) {
            etNewEmail.setError("Please enter a different email");
            etNewEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(currentPassword)) {
            etCurrentPassword.setError("Current password is required");
            etCurrentPassword.requestFocus();
            return;
        }

        AuthCredential credential =
                EmailAuthProvider.getCredential(currentEmail, currentPassword);

        currentUser.reauthenticate(credential)
                .addOnSuccessListener(unused -> {
                    currentUser.verifyBeforeUpdateEmail(newEmail)
                            .addOnSuccessListener(unused2 -> Toast.makeText(
                                    ChangeEmailActivity.this,
                                    "Verification email sent. Open the new email, click the link, then return here and press 'I've Verified My Email'.",
                                    Toast.LENGTH_LONG
                            ).show())
                            .addOnFailureListener(e -> Toast.makeText(
                                    ChangeEmailActivity.this,
                                    "Failed to start email change: " + e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show());
                })
                .addOnFailureListener(e -> Toast.makeText(
                        ChangeEmailActivity.this,
                        "Current password is incorrect",
                        Toast.LENGTH_LONG
                ).show());
    }

    private void checkVerifiedEmailChange() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        user.reload()
                .addOnSuccessListener(unused -> {
                    FirebaseUser refreshedUser = mAuth.getCurrentUser();

                    if (refreshedUser == null || refreshedUser.getEmail() == null) {
                        Toast.makeText(this, "Unable to refresh user", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String refreshedEmail = refreshedUser.getEmail();
                    String userId = refreshedUser.getUid();

                    if (refreshedEmail.equalsIgnoreCase(originalEmail)) {
                        Toast.makeText(
                                this,
                                "Email is not changed yet. Please click the verification link in your new email first.",
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("email", refreshedEmail);

                    db.collection("users").document(userId)
                            .update(updates)
                            .addOnSuccessListener(unused2 -> {
                                originalEmail = refreshedEmail;
                                etNewEmail.setText(refreshedEmail);

                                new AlertDialog.Builder(ChangeEmailActivity.this)
                                        .setTitle("Success")
                                        .setMessage("You have changed your email successfully.")
                                        .setCancelable(false)
                                        .setPositiveButton("OK", (dialog, which) -> finish())
                                        .show();
                            })
                            .addOnFailureListener(e -> Toast.makeText(
                                    ChangeEmailActivity.this,
                                    "Failed to update Firestore email: " + e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show());
                })
                .addOnFailureListener(e -> Toast.makeText(
                        ChangeEmailActivity.this,
                        "Failed to refresh user: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show());
    }
}