package com.example.smartvitals;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

public class ProfileFragment extends Fragment {

    private TextView tvProfileAvatar, tvProfileName, tvProfileUid;
    private LinearLayout btnEditProfile, btnLogout, btnAccountSettings;
    private ImageView btnCopyId;

    private TextView tvAge, tvBloodType, tvConditions, tvDisease, tvEmergencyContact, tvHeight, tvWeight;

    public ProfileFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        tvProfileAvatar = view.findViewById(R.id.tvProfileAvatar);
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileUid = view.findViewById(R.id.tvProfileUid);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnAccountSettings = view.findViewById(R.id.btnAccountSettings);
        btnCopyId = view.findViewById(R.id.btnCopyId);

        tvAge = view.findViewById(R.id.tvAge);
        tvBloodType = view.findViewById(R.id.tvBloodType);
        tvConditions = view.findViewById(R.id.tvConditions);
        tvDisease = view.findViewById(R.id.tvDisease);
        tvEmergencyContact = view.findViewById(R.id.tvEmergencyContact);
        tvHeight = view.findViewById(R.id.tvHeight);
        tvWeight = view.findViewById(R.id.tvWeight);

        btnCopyId.setOnClickListener(v -> {
            String uid = tvProfileUid.getText().toString();
            if (!uid.isEmpty() && !uid.equals("User ID")) {
                ClipboardManager clipboard =
                        (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("User ID", uid);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(requireContext(), "User ID copied to clipboard!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "ID not ready yet", Toast.LENGTH_SHORT).show();
            }
        });

        loadUserProfile();

        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), EditProfileActivity.class);
            startActivity(intent);
        });

        if (btnAccountSettings != null) {
            btnAccountSettings.setOnClickListener(v -> {
                Intent intent = new Intent(requireActivity(), AccountSecurityActivity.class);
                startActivity(intent);
            });
        }

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            startActivity(intent);
            requireActivity().finish();
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserProfile();
    }

    private void loadUserProfile() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            tvProfileUid.setText(userId);

            db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            if (name != null && !name.trim().isEmpty()) {
                                tvProfileName.setText(name);
                                tvProfileAvatar.setText(name.substring(0, 1).toUpperCase());
                            } else {
                                tvProfileName.setText("User");
                                tvProfileAvatar.setText("U");
                            }

                            String age = documentSnapshot.getString("age");
                            String bloodType = documentSnapshot.getString("bloodType");
                            String disease = documentSnapshot.getString("disease");
                            String emergencyContact = documentSnapshot.getString("emergencyContact");
                            String heightStr = documentSnapshot.getString("height");
                            String weightStr = documentSnapshot.getString("weight");

                            if (tvAge != null) {
                                tvAge.setText(age != null && !age.isEmpty() ? age : "--");
                            }

                            if (tvBloodType != null) {
                                tvBloodType.setText(bloodType != null && !bloodType.isEmpty() ? bloodType : "--");
                            }

                            if (tvDisease != null) {
                                tvDisease.setText(disease != null && !disease.isEmpty() ? disease : "None");
                            }

                            if (tvEmergencyContact != null) {
                                tvEmergencyContact.setText(
                                        emergencyContact != null && !emergencyContact.isEmpty()
                                                ? emergencyContact
                                                : "Not set"
                                );
                            }

                            if (tvHeight != null) {
                                tvHeight.setText(heightStr != null && !heightStr.isEmpty() ? heightStr + " cm" : "Not set");
                            }

                            if (tvWeight != null) {
                                tvWeight.setText(weightStr != null && !weightStr.isEmpty() ? weightStr + " kg" : "Not set");
                            }

                            if (tvConditions != null) {
                                try {
                                    if (heightStr != null && weightStr != null
                                            && !heightStr.isEmpty() && !weightStr.isEmpty()) {

                                        double heightCm = Double.parseDouble(heightStr);
                                        double weightKg = Double.parseDouble(weightStr);

                                        if (heightCm > 0) {
                                            double heightM = heightCm / 100.0;
                                            double bmi = weightKg / (heightM * heightM);
                                            tvConditions.setText(String.format(Locale.getDefault(), "%.1f", bmi));
                                        } else {
                                            tvConditions.setText("--");
                                        }
                                    } else {
                                        tvConditions.setText("--");
                                    }
                                } catch (Exception e) {
                                    tvConditions.setText("--");
                                }
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (getContext() != null) {
                            Toast.makeText(requireContext(), "Failed to load profile data", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}