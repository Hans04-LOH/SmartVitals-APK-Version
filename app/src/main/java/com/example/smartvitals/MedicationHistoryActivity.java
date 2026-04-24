package com.example.smartvitals;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MedicationHistoryActivity extends AppCompatActivity {

    private LinearLayout emptyStateLayout;
    private LinearLayout historyListContainer;
    private AppCompatButton btnAddReminder;
    private AppCompatButton btnAddFirstReminder;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medication_history);

        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        historyListContainer = findViewById(R.id.historyListContainer);
        btnAddReminder = findViewById(R.id.btnAddReminder);
        btnAddFirstReminder = findViewById(R.id.btnAddFirstReminder);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
            loadHistoryFromFirebase();
        } else {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
        }

        if (btnAddReminder != null) {
            btnAddReminder.setOnClickListener(v -> showAddReminderDialog());
        }

        if (btnAddFirstReminder != null) {
            btnAddFirstReminder.setOnClickListener(v -> showAddReminderDialog());
        }
    }

    private void loadHistoryFromFirebase() {
        db.collection("users").document(userId).collection("medications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Load error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }

                    historyListContainer.removeAllViews();

                    if (value == null || value.isEmpty()) {
                        emptyStateLayout.setVisibility(View.VISIBLE);
                        historyListContainer.setVisibility(View.GONE);
                        return;
                    }

                    emptyStateLayout.setVisibility(View.GONE);
                    historyListContainer.setVisibility(View.VISIBLE);

                    LayoutInflater inflater = LayoutInflater.from(this);

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        View row = inflater.inflate(R.layout.item_medication_history, historyListContainer, false);

                        TextView tvMedName = row.findViewById(R.id.tvMedName);
                        TextView tvMedMeta = row.findViewById(R.id.tvMedMeta);
                        TextView tvMedStatus = row.findViewById(R.id.tvMedStatus);
                        AppCompatButton btnMarkTaken = row.findViewById(R.id.btnMarkTaken);
                        AppCompatButton btnMarkMissed = row.findViewById(R.id.btnMarkMissed);
                        AppCompatButton btnDeleteReminder = row.findViewById(R.id.btnDeleteReminder);

                        String docId = doc.getId();
                        String medicineName = doc.getString("medicineName");
                        String time = doc.getString("time");
                        String note = doc.getString("note");
                        String status = doc.getString("status");
                        String createdAt = doc.getString("createdAt");

                        tvMedName.setText(medicineName != null ? medicineName : "Unknown");

                        String metaText;
                        if (TextUtils.isEmpty(note)) {
                            metaText = "Time: " + time + "\nCreated: " + createdAt;
                        } else {
                            metaText = "Time: " + time + "\nNote: " + note + "\nCreated: " + createdAt;
                        }
                        tvMedMeta.setText(metaText);
                        tvMedStatus.setText(status != null ? status : "Scheduled");

                        if ("Taken".equalsIgnoreCase(status)) {
                            tvMedStatus.setTextColor(0xFF059669);
                        } else if ("Missed".equalsIgnoreCase(status)) {
                            tvMedStatus.setTextColor(0xFFDC2626);
                        } else {
                            tvMedStatus.setTextColor(0xFF3B82F6);
                        }

                        btnMarkTaken.setOnClickListener(v -> updateStatus(docId, "Taken"));
                        btnMarkMissed.setOnClickListener(v -> updateStatus(docId, "Missed"));

                        if (btnDeleteReminder != null) {
                            btnDeleteReminder.setOnClickListener(v -> showDeleteConfirmDialog(docId));
                        }

                        historyListContainer.addView(row);
                    }
                });
    }

    private void updateStatus(String docId, String newStatus) {
        db.collection("users").document(userId).collection("medications")
                .document(docId)
                .update("status", newStatus)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Status updated to " + newStatus, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show());
    }

    private void showDeleteConfirmDialog(String docId) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Reminder")
                .setMessage("Are you sure you want to delete this reminder?")
                .setPositiveButton("Delete", (dialog, which) -> deleteReminder(docId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteReminder(String docId) {
        db.collection("users").document(userId).collection("medications")
                .document(docId)
                .delete()
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Reminder deleted", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to delete reminder", Toast.LENGTH_SHORT).show());
    }

    private void showAddReminderDialog() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int padding = dpToPx(20);
        root.setPadding(padding, padding, padding, 0);

        EditText etMedicineName = new EditText(this);
        etMedicineName.setHint("Medicine name");
        root.addView(etMedicineName);

        EditText etNote = new EditText(this);
        etNote.setHint("Optional note (e.g. after meal)");
        root.addView(etNote);

        TextView tvSelectedTime = new TextView(this);
        tvSelectedTime.setText("Time: 08:00");
        tvSelectedTime.setPadding(0, dpToPx(16), 0, dpToPx(8));
        root.addView(tvSelectedTime);

        final int[] selectedHour = {8};
        final int[] selectedMinute = {0};

        AppCompatButton btnPickTime = new AppCompatButton(this);
        btnPickTime.setText("Pick Time");
        btnPickTime.setOnClickListener(v -> {
            TimePickerDialog dialog = new TimePickerDialog(
                    this,
                    (view, hourOfDay, minute) -> {
                        selectedHour[0] = hourOfDay;
                        selectedMinute[0] = minute;
                        tvSelectedTime.setText(String.format(
                                Locale.getDefault(),
                                "Time: %02d:%02d",
                                selectedHour[0],
                                selectedMinute[0]
                        ));
                    },
                    selectedHour[0],
                    selectedMinute[0],
                    true
            );
            dialog.show();
        });
        root.addView(btnPickTime);

        new AlertDialog.Builder(this)
                .setTitle("Add Medication Reminder")
                .setView(root)
                .setPositiveButton("Save", (dialog, which) -> {
                    String medicineName = etMedicineName.getText().toString().trim();
                    String note = etNote.getText().toString().trim();

                    if (medicineName.isEmpty()) {
                        Toast.makeText(this, "Please enter medicine name", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String time = String.format(
                            Locale.getDefault(),
                            "%02d:%02d",
                            selectedHour[0],
                            selectedMinute[0]
                    );

                    saveReminderToFirebase(medicineName, time, note);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveReminderToFirebase(String medicineName, String time, String note) {
        String createdAt = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(new Date());

        Map<String, Object> medData = new HashMap<>();
        medData.put("medicineName", medicineName);
        medData.put("time", time);
        medData.put("note", note);
        medData.put("status", "Scheduled");
        medData.put("createdAt", createdAt);
        medData.put("timestamp", System.currentTimeMillis());

        db.collection("users").document(userId).collection("medications")
                .add(medData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Medication reminder added", Toast.LENGTH_SHORT).show();
                    syncLatestReminderToSportPrefs(medicineName, time);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to add reminder", Toast.LENGTH_SHORT).show());
    }

    private void syncLatestReminderToSportPrefs(String medicineName, String time) {
        try {
            String[] parts = time.split(":");
            int h = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);

            SharedPreferences sportPrefs = getSharedPreferences("SportPrefs", Context.MODE_PRIVATE);
            sportPrefs.edit()
                    .putInt("remind_h", h)
                    .putInt("remind_m", m)
                    .putString("remind_name", medicineName)
                    .apply();
        } catch (Exception ignored) {
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}