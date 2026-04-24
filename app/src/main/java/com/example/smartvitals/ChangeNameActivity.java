package com.example.smartvitals;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ChangeNameActivity extends AppCompatActivity {

    private EditText etNewName;
    private Button btnSaveNewName;
    private ImageView btnBack;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_name);

        btnBack = findViewById(R.id.btnBack);
        etNewName = findViewById(R.id.etNewName);
        btnSaveNewName = findViewById(R.id.btnSaveNewName);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnBack.setOnClickListener(v -> finish());

        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();

            db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String currentName = documentSnapshot.getString("name");
                            if (currentName != null && !currentName.isEmpty()) {
                                etNewName.setText(currentName);
                            }
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(ChangeNameActivity.this, "Failed to load current name", Toast.LENGTH_SHORT).show()
                    );
        }

        btnSaveNewName.setOnClickListener(v -> {
            String newName = etNewName.getText().toString().trim();

            if (TextUtils.isEmpty(newName)) {
                etNewName.setError("Name is required");
                etNewName.requestFocus();
                return;
            }

            if (mAuth.getCurrentUser() == null) {
                Toast.makeText(ChangeNameActivity.this, "User not logged in", Toast.LENGTH_SHORT).show();
                return;
            }

            String userId = mAuth.getCurrentUser().getUid();

            Map<String, Object> updates = new HashMap<>();
            updates.put("name", newName);

            db.collection("users").document(userId)
                    .update(updates)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(ChangeNameActivity.this, "Name updated successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(ChangeNameActivity.this, "Failed to update name: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
        });
    }
}