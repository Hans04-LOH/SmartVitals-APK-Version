package com.example.smartvitals;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AccountSecurityActivity extends AppCompatActivity {

    private ImageView btnBack;
    private LinearLayout cardChangeEmail;
    private LinearLayout cardChangePassword;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_security);

        btnBack = findViewById(R.id.btnBack);
        cardChangeEmail = findViewById(R.id.cardChangeEmail);
        cardChangePassword = findViewById(R.id.cardChangePassword);

        mAuth = FirebaseAuth.getInstance();

        btnBack.setOnClickListener(v -> finish());

        cardChangeEmail.setOnClickListener(v -> {
            Intent intent = new Intent(AccountSecurityActivity.this, ChangeEmailActivity.class);
            startActivity(intent);
        });

        cardChangePassword.setOnClickListener(v -> showPasswordResetDialog());
    }

    private void showPasswordResetDialog() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String email = user.getEmail();
        if (email == null || email.trim().isEmpty()) {
            Toast.makeText(this, "No email found for this account", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Change Password")
                .setMessage("Send a password reset link to:\n\n" + email + "\n\nOpen the email and set your new password there.")
                .setPositiveButton("Send Link", (dialog, which) -> sendPasswordResetLink(email))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void sendPasswordResetLink(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> new AlertDialog.Builder(this)
                        .setTitle("Email Sent")
                        .setMessage("Password reset link sent to:\n\n" + email + "\n\nPlease check your inbox and spam folder.")
                        .setPositiveButton("OK", null)
                        .show())
                .addOnFailureListener(e -> Toast.makeText(
                        this,
                        "Failed to send reset email: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show());
    }
}