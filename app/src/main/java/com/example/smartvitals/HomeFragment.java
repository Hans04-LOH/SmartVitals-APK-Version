package com.example.smartvitals;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Random;

public class HomeFragment extends Fragment implements SensorEventListener {

    public interface EmergencyNavigationHost {
        void openSupportEmergency();
    }

    private AppCompatButton btnSyncDevice;
    private TextView tvDeviceStatus, heartRateValue, tvTopHeartRate, tvTopSteps, tvTopSleep;
    private TextView stepsValue, sleepValue, caloriesValue, tvConditionValue, tvRiskBadge;
    private LinearLayout cardMedication;
    private TextView tvMedStatusCard, tvMedInfo;
    private AppCompatButton btnTakeMed;

    private SensorManager sensorManager;
    private Sensor stepCounterSensor;
    private boolean isSensorPresent = false;
    private int initialStepCount = -1;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) initSensor();
            });

    public HomeFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        btnSyncDevice = root.findViewById(R.id.btnSyncDevice);
        tvDeviceStatus = root.findViewById(R.id.tvDeviceStatus);
        heartRateValue = root.findViewById(R.id.heartRateValue);
        tvTopHeartRate = root.findViewById(R.id.tvTopHeartRate);
        tvTopSteps = root.findViewById(R.id.tvTopSteps);
        tvTopSleep = root.findViewById(R.id.tvTopSleep);
        stepsValue = root.findViewById(R.id.stepsValue);
        sleepValue = root.findViewById(R.id.sleepValue);
        caloriesValue = root.findViewById(R.id.caloriesValue);
        tvConditionValue = root.findViewById(R.id.tvConditionValue);
        tvRiskBadge = root.findViewById(R.id.tvRiskBadge);
        cardMedication = root.findViewById(R.id.cardMedication);
        tvMedStatusCard = root.findViewById(R.id.tvMedStatus);
        tvMedInfo = root.findViewById(R.id.tvMedInfo);
        btnTakeMed = root.findViewById(R.id.btnTakeMed);

        if (cardMedication != null) {
            cardMedication.setOnClickListener(v -> {
                startActivity(new Intent(getActivity(), MedicationHistoryActivity.class));
            });
        }

        setDefaultUi();
        sensorManager = (SensorManager) requireActivity().getSystemService(Context.SENSOR_SERVICE);
        checkPermissionAndInitSensor();

        btnSyncDevice.setOnClickListener(v -> syncDemoData());
        listenToLatestMedication();

        return root;
    }

    private void checkPermissionAndInitSensor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION);
            } else {
                initSensor();
            }
        } else {
            initSensor();
        }
    }

    private void initSensor() {
        if (sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null) {
            stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            isSensorPresent = true;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isSensorPresent) sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isSensorPresent) sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            float totalSteps = event.values[0];
            if (initialStepCount == -1) initialStepCount = (int) totalSteps;
            int currentSteps = (int) totalSteps - initialStepCount;

            String stepsStr = NumberFormat.getNumberInstance(Locale.getDefault()).format(currentSteps);
            stepsValue.setText(stepsStr);
            tvTopSteps.setText(stepsStr);
            caloriesValue.setText(String.format(Locale.getDefault(), "%.1f kcal", currentSteps * 0.04));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void syncDemoData() {
        btnSyncDevice.setEnabled(false);
        btnSyncDevice.setText("Syncing...");
        handler.postDelayed(() -> {
            if (!isAdded()) return;
            DemoResult result = generateRandomDemoResult();
            applyDemoResult(result);
            btnSyncDevice.setEnabled(true);
            btnSyncDevice.setText("Sync Smart Device");
            if (result.isDanger) showEmergencyDialog(result.avgHeartRate, result.maxHeartRate);
        }, 1200);
    }

    private void applyDemoResult(DemoResult result) {
        heartRateValue.setText(result.avgHeartRate + " bpm");
        tvTopHeartRate.setText(String.valueOf(result.maxHeartRate));
        sleepValue.setText(String.format(Locale.getDefault(), "%.1f h", result.sleepHours));
        tvTopSleep.setText(String.format(Locale.getDefault(), "%.1f h", result.sleepHours));
        tvConditionValue.setText(result.condition);
        tvRiskBadge.setText(result.risk);
        if (!isSensorPresent) {
            stepsValue.setText(String.valueOf(result.totalSteps));
            tvTopSteps.setText(String.valueOf(result.totalSteps));
        }
    }

    private DemoResult generateRandomDemoResult() {
        int pick = random.nextInt(5);
        if (pick == 0) return createHealthyData();
        if (pick == 1) return createNormalData();
        if (pick == 2) return createLowActivityData();
        if (pick == 3) return createWarningData();
        return createDangerData();
    }

    private DemoResult createHealthyData() {
        return new DemoResult(65, 105, 10000, 7.5, 400, "Healthy", "LOW", false);
    }
    private DemoResult createNormalData() {
        return new DemoResult(75, 120, 6000, 6.5, 250, "Normal", "OK", false);
    }
    private DemoResult createLowActivityData() {
        return new DemoResult(80, 110, 2000, 5.5, 100, "Low Activity", "CHECK", false);
    }
    private DemoResult createWarningData() {
        return new DemoResult(105, 145, 800, 4.0, 50, "Warning", "MED", false);
    }
    private DemoResult createDangerData() {
        return new DemoResult(135, 170, 300, 2.5, 20, "Danger", "HIGH", true);
    }

    private void listenToLatestMedication() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) return;
        FirebaseFirestore.getInstance().collection("users").document(mAuth.getCurrentUser().getUid())
                .collection("medications").orderBy("timestamp", Query.Direction.DESCENDING).limit(1)
                .addSnapshotListener((value, error) -> {
                    if (!isAdded() || value == null || value.isEmpty()) return;
                    DocumentSnapshot doc = value.getDocuments().get(0);
                    tvMedStatusCard.setText("Next: " + doc.getString("time"));
                    tvMedInfo.setText(doc.getString("medicineName"));
                });
    }

    private void setDefaultUi() {
        tvDeviceStatus.setText("Real-time Tracking Active");
        heartRateValue.setText("--");
        stepsValue.setText("--");
    }

    private void showEmergencyDialog(long avg, long max) {
        new AlertDialog.Builder(requireContext()).setTitle("Alert").setMessage("Danger detected!").setPositiveButton("OK", null).show();
    }

    private static class DemoResult {
        long avgHeartRate, maxHeartRate, totalSteps, calories;
        double sleepHours;
        String condition, risk;
        boolean isDanger;

        DemoResult(long avg, long max, long steps, double sleep, long kcal, String cond, String r, boolean danger) {
            this.avgHeartRate = avg; this.maxHeartRate = max; this.totalSteps = steps;
            this.sleepHours = sleep; this.calories = kcal; this.condition = cond;
            this.risk = r; this.isDanger = danger;
        }
    }
}