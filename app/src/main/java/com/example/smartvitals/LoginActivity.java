package com.example.smartvitals;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private EditText etLoginEmail, etLoginPassword;
    private Button btnLogin;
    private TextView tvGoToSignUp, tvForgotPassword;
    private ImageView ivTogglePassword;
    private CheckBox cbRememberMe;

    private boolean isPasswordVisible = false;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        etLoginEmail = findViewById(R.id.etLoginEmail);
        etLoginPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvGoToSignUp = findViewById(R.id.tvGoToSignUp);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);
        cbRememberMe = findViewById(R.id.cbRememberMe);

        ivTogglePassword.setOnClickListener(v -> togglePasswordVisibility());

        btnLogin.setOnClickListener(v -> {
            String email = etLoginEmail.getText().toString().trim();
            String password = etLoginPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                etLoginEmail.setError("Email is required");
                etLoginEmail.requestFocus();
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etLoginEmail.setError("Enter a valid email");
                etLoginEmail.requestFocus();
                return;
            }

            if (TextUtils.isEmpty(password)) {
                etLoginPassword.setError("Password is required");
                etLoginPassword.requestFocus();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        Toast.makeText(LoginActivity.this, "Login Successful", Toast.LENGTH_LONG).show();
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        e.printStackTrace();
                        Toast.makeText(
                                LoginActivity.this,
                                "Login Failed: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    });
        });

        tvGoToSignUp.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
        });

        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
    }

    private void showForgotPasswordDialog() {
        final EditText etEmailInput = new EditText(this);
        etEmailInput.setHint("Enter your email");
        etEmailInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        etEmailInput.setPadding(40, 30, 40, 30);

        String currentEmail = etLoginEmail.getText().toString().trim();
        if (!currentEmail.isEmpty()) {
            etEmailInput.setText(currentEmail);
            etEmailInput.setSelection(currentEmail.length());
        }

        new AlertDialog.Builder(this)
                .setTitle("Forgot Password")
                .setMessage("Enter your account email to receive a password reset link.")
                .setView(etEmailInput)
                .setPositiveButton("Send Link", (dialog, which) -> {
                    String email = etEmailInput.getText().toString().trim();

                    if (TextUtils.isEmpty(email)) {
                        Toast.makeText(this, "Email is required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        Toast.makeText(this, "Enter a valid email", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    mAuth.sendPasswordResetEmail(email)
                            .addOnSuccessListener(unused -> new AlertDialog.Builder(this)
                                    .setTitle("Email Sent")
                                    .setMessage("Password reset link sent to:\n\n" + email + "\n\nPlease check your inbox and spam folder.")
                                    .setPositiveButton("OK", null)
                                    .show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed to send reset email: " + e.getMessage(), Toast.LENGTH_LONG).show()
                            );
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            etLoginPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            ivTogglePassword.setImageResource(android.R.drawable.ic_menu_view);
            isPasswordVisible = false;
        } else {
            etLoginPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            ivTogglePassword.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            isPasswordVisible = true;
        }

        etLoginPassword.setSelection(etLoginPassword.getText().length());
    }
}