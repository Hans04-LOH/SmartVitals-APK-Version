package com.example.smartvitals;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private EditText etName, etAge, etBloodType, etHeight, etWeight, etDisease, etEmergencyContact;
    private AppCompatButton btnSaveProfile;
    private ImageView btnBack;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
        } else {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        etName = findViewById(R.id.etEditName);
        etAge = findViewById(R.id.etEditAge);
        etBloodType = findViewById(R.id.etEditBloodType);
        etHeight = findViewById(R.id.etEditHeight);
        etWeight = findViewById(R.id.etEditWeight);
        etDisease = findViewById(R.id.etEditDisease);
        etEmergencyContact = findViewById(R.id.etEditEmergencyContact);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        loadCurrentData();

        btnSaveProfile.setOnClickListener(v -> saveProfileData());
    }

    private void loadCurrentData() {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        etName.setText(documentSnapshot.getString("name"));
                        etAge.setText(documentSnapshot.getString("age"));
                        etBloodType.setText(documentSnapshot.getString("bloodType"));
                        etHeight.setText(documentSnapshot.getString("height"));
                        etWeight.setText(documentSnapshot.getString("weight"));
                        etDisease.setText(documentSnapshot.getString("disease"));
                        etEmergencyContact.setText(documentSnapshot.getString("emergencyContact"));
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(EditProfileActivity.this, "Failed to load data", Toast.LENGTH_SHORT).show());
    }

    private void saveProfileData() {
        btnSaveProfile.setEnabled(false);
        btnSaveProfile.setText("Saving...");

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", etName.getText().toString().trim());
        updates.put("age", etAge.getText().toString().trim());
        updates.put("bloodType", etBloodType.getText().toString().trim());
        updates.put("height", etHeight.getText().toString().trim());
        updates.put("weight", etWeight.getText().toString().trim());
        updates.put("disease", etDisease.getText().toString().trim());
        updates.put("emergencyContact", etEmergencyContact.getText().toString().trim());

        db.collection("users").document(userId)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(EditProfileActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSaveProfile.setEnabled(true);
                    btnSaveProfile.setText("Save Changes");
                    Toast.makeText(EditProfileActivity.this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                });
    }
}