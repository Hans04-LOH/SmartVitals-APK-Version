package com.example.smartvitals;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.health.connect.client.HealthConnectClient;
import androidx.health.connect.client.PermissionController;
import androidx.health.connect.client.permission.HealthPermission;
import androidx.health.connect.client.records.HeartRateRecord;
import androidx.health.connect.client.records.StepsRecord;

// ⭐ 这里加了翻译官的引入
import kotlin.jvm.JvmClassMappingKt;

import java.util.HashSet;
import java.util.Set;

public class ActivityDeviceConnect extends AppCompatActivity {

    public static final String PREFS_NAME = "smartvitals_prefs";
    public static final String KEY_DEVICE_CONNECTED = "device_connected";
    public static final String KEY_DEVICE_NAME = "device_name";
    public static final String KEY_HAS_MEASURED_DATA = "has_measured_data";
    public static final String KEY_HEART_RATE = "heart_rate";
    public static final String KEY_STEPS = "steps";
    public static final String KEY_SLEEP = "sleep";
    public static final String KEY_CALORIES = "calories";
    public static final String KEY_CONDITION = "condition";
    public static final String KEY_RISK = "risk";

    private ActivityResultLauncher<Set<String>> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_connect);

        requestPermissionLauncher = registerForActivityResult(
                PermissionController.createRequestPermissionResultContract(),
                granted -> {
                    // ⭐ 这里用翻译官包住了原来的 class
                    if (granted.contains(HealthPermission.getReadPermission(JvmClassMappingKt.getKotlinClass(HeartRateRecord.class))) &&
                            granted.contains(HealthPermission.getReadPermission(JvmClassMappingKt.getKotlinClass(StepsRecord.class)))) {

                        // 如果用户点击了“允许”，就去抓取真实数据！
                        fetchRealHealthData("Google Health Connect");

                    } else {
                        Toast.makeText(this, "Permission Denied. Using demo data instead.", Toast.LENGTH_SHORT).show();
                        // 如果用户拒绝，我们依然让他连上，但给一组假数据演示
                        saveRealDataToPrefs("Google Health Connect (Limited)", 0, 0);
                    }
                }
        );

        View smartWatchCard = findViewById(R.id.cardSmartWatch);
        View medicalSensorCard = findViewById(R.id.cardMedicalSensor);
        Button btnSyncHealthConnect = findViewById(R.id.btnSyncHealthConnect);

        if (smartWatchCard != null) {
            smartWatchCard.setOnClickListener(v -> saveRealDataToPrefs("Generic Smart Watch", 5000, 75));
        }

        if (medicalSensorCard != null) {
            medicalSensorCard.setOnClickListener(v -> saveRealDataToPrefs("SmartVitals Pro Sensor", 1200, 80));
        }

        if (btnSyncHealthConnect != null) {
            btnSyncHealthConnect.setOnClickListener(v -> checkAndRequestHealthPermissions());
        }
    }

    private void checkAndRequestHealthPermissions() {
        int availabilityStatus = HealthConnectClient.getSdkStatus(this, "com.google.android.apps.healthdata");

        if (availabilityStatus == HealthConnectClient.SDK_UNAVAILABLE) {
            Toast.makeText(this, "Health Connect is not installed!", Toast.LENGTH_LONG).show();
            return;
        }

        Set<String> permissions = new HashSet<>();
        // ⭐ 这里也用翻译官包住了原来的 class
        permissions.add(HealthPermission.getReadPermission(JvmClassMappingKt.getKotlinClass(HeartRateRecord.class)));
        permissions.add(HealthPermission.getReadPermission(JvmClassMappingKt.getKotlinClass(StepsRecord.class)));

        requestPermissionLauncher.launch(permissions);
    }

    private void fetchRealHealthData(String deviceName) {
        Toast.makeText(this, "Fetching real health data from Google...", Toast.LENGTH_SHORT).show();

        HealthConnectClient client = HealthConnectClient.getOrCreate(this);

        HealthDataFetcher.INSTANCE.fetchRealData(client, new HealthDataFetcher.FetchCallback() {
            @Override
            public void onSuccess(long steps, long avgHeartRate) {
                saveRealDataToPrefs(deviceName, steps, avgHeartRate);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(ActivityDeviceConnect.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                saveRealDataToPrefs(deviceName, 0, 0);
            }
        });
    }

    private void saveRealDataToPrefs(String deviceName, long realSteps, long realHeartRate) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        String stepsDisplay = String.format("%,d", realSteps);
        String hrDisplay = realHeartRate > 0 ? realHeartRate + " BPM" : "--";

        prefs.edit()
                .putBoolean(KEY_DEVICE_CONNECTED, true)
                .putString(KEY_DEVICE_NAME, deviceName)
                .putBoolean(KEY_HAS_MEASURED_DATA, true)
                .putString(KEY_HEART_RATE, hrDisplay)
                .putString(KEY_STEPS, stepsDisplay)
                .putString(KEY_SLEEP, "7h 15m")
                .putString(KEY_CALORIES, "380 kcal")
                .putString(KEY_CONDITION, "Monitoring")
                .putString(KEY_RISK, "Low")
                .apply();

        finish();
    }
}