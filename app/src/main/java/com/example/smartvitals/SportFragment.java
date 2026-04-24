package com.example.smartvitals;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.health.connect.client.HealthConnectClient;

import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

public class SportFragment extends Fragment {

    private TextView tvDeviceStatus, tvWorkoutTitle, tvWorkoutHint, tvTodayDurationValue, tvAvgHrValue, tvWorkoutMessage, tvSuggestedExercises;
    private LinearLayout cardSuggestedExercise;
    private TextView tvExerciseIcon, tvExerciseTitle, tvExerciseStatus, tvReminderTime;
    private Button btnSetReminder, btnStartWorkout, btnFinishWorkout;
    private LinearLayout btnConnectWearable;

    private android.os.Handler timerHandler = new android.os.Handler();
    private long startTime = 0L;

    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;
            if (tvTodayDurationValue != null) tvTodayDurationValue.setText(String.format(Locale.getDefault(), "%d min %02d sec", minutes, seconds));
            if (tvAvgHrValue != null) {
                int liveHr = 110 + new Random().nextInt(30);
                tvAvgHrValue.setText(liveHr + " bpm");
                if (liveHr > 130) {
                    tvAvgHrValue.setTextColor(Color.RED);
                    if (tvWorkoutTitle != null) { tvWorkoutTitle.setText("⚠️ HR TOO HIGH!"); tvWorkoutTitle.setTextColor(Color.RED); }
                } else {
                    tvAvgHrValue.setTextColor(Color.parseColor("#0F172A"));
                    if (tvWorkoutTitle != null) { tvWorkoutTitle.setText("Workout in Progress..."); tvWorkoutTitle.setTextColor(Color.WHITE); }
                }
            }
            timerHandler.postDelayed(this, 1000);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sport, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 初始化所有控件
        tvDeviceStatus = view.findViewById(R.id.tvDeviceStatus);
        tvWorkoutTitle = view.findViewById(R.id.tvWorkoutTitle);
        tvWorkoutHint = view.findViewById(R.id.tvWorkoutHint);
        tvTodayDurationValue = view.findViewById(R.id.tvTodayDurationValue);
        tvAvgHrValue = view.findViewById(R.id.tvAvgHrValue);
        tvWorkoutMessage = view.findViewById(R.id.tvWorkoutMessage);
        tvSuggestedExercises = view.findViewById(R.id.tvSuggestedExercises);
        cardSuggestedExercise = view.findViewById(R.id.cardSuggestedExercise);
        tvExerciseIcon = view.findViewById(R.id.tvExerciseIcon);
        tvExerciseTitle = view.findViewById(R.id.tvExerciseTitle);
        tvExerciseStatus = view.findViewById(R.id.tvExerciseStatus);
        tvReminderTime = view.findViewById(R.id.tvReminderTime);
        btnSetReminder = view.findViewById(R.id.btnSetReminder);
        btnStartWorkout = view.findViewById(R.id.btnStartWorkout);
        btnFinishWorkout = view.findViewById(R.id.btnFinishWorkout);
        btnConnectWearable = view.findViewById(R.id.btnConnectWearable);

        if (btnConnectWearable != null) btnConnectWearable.setOnClickListener(v -> startActivity(new Intent(getActivity(), ActivityDeviceConnect.class)));
        if (btnStartWorkout != null) btnStartWorkout.setOnClickListener(v -> startWorkout());
        if (btnFinishWorkout != null) btnFinishWorkout.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext()).setTitle("Finish Workout?").setMessage("Are you sure?").setPositiveButton("Yes", (d, w) -> finishWorkout()).setNegativeButton("No", null).show();
        });
        if (btnSetReminder != null) btnSetReminder.setOnClickListener(v -> showTimePicker());

        loadSavedReminder();
        updateSportUi();

        SharedPreferences prefs = requireActivity().getSharedPreferences(ActivityDeviceConnect.PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.getBoolean("workout_active", false)) {
            startTime = prefs.getLong("workout_start_time", System.currentTimeMillis());
            timerHandler.postDelayed(timerRunnable, 0);
        }
    }

    private void saveRoutine(int h, int m, String routineName) {
        if (getActivity() == null) return;
        saveReminder(h, m, routineName);
        setSystemAlarm(h, m, routineName);

        String timeStr = String.format(Locale.getDefault(), "%02d:%02d", h, m);
        if (tvReminderTime != null) tvReminderTime.setText(routineName + " set at " + timeStr);
        Toast.makeText(getContext(), routineName + " schedule confirmed!", Toast.LENGTH_SHORT).show();
    }

    private void setSystemAlarm(int hour, int minute, String taskName) {
        Context context = getContext();
        if (context == null) return;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
                Toast.makeText(context, "Please enable Alarm permission for this feature", Toast.LENGTH_LONG).show();
                return;
            }
        }

        Intent intent = new Intent(context, WorkoutReminderReceiver.class);
        intent.putExtra("exercise_name", taskName);

        int requestCode = (int) System.currentTimeMillis();

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        if (alarmManager != null) {
            AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(
                    calendar.getTimeInMillis(),
                    pendingIntent
            );
            alarmManager.setAlarmClock(info, pendingIntent);
        }

        long diffMillis = calendar.getTimeInMillis() - System.currentTimeMillis();
        long minutesLeft = diffMillis / (1000 * 60);
        Toast.makeText(context, "Reminder set for " + taskName + " (in " + minutesLeft + " mins)", Toast.LENGTH_SHORT).show();
    }

    private void showTimePicker() {
        String[] options = {"Morning Walk (07:30 AM)", "Afternoon Stretch (04:30 PM)", "Evening Stroll (07:00 PM)", "Custom Time..."};
        new AlertDialog.Builder(requireContext()).setTitle("Pick a routine")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) saveRoutine(7, 30, "Morning Walk");
                    else if (which == 1) saveRoutine(16, 30, "Afternoon Stretch");
                    else if (which == 2) saveRoutine(19, 0, "Evening Stroll");
                    else openCustomTimePicker();
                }).show();
    }

    private void openCustomTimePicker() {
        Calendar c = Calendar.getInstance();
        new TimePickerDialog(getContext(), (v, h, m) -> showExerciseNameInput(h, m), c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
    }

    private void showExerciseNameInput(int h, int m) {
        final EditText etInput = new EditText(requireContext());
        new AlertDialog.Builder(requireContext()).setTitle("2. What exercise?").setView(etInput)
                .setPositiveButton("Set Reminder", (d, w) -> {
                    String name = etInput.getText().toString().trim();
                    saveRoutine(h, m, name.isEmpty() ? "Custom Exercise" : name);
                }).show();
    }

    private void saveReminder(int h, int m, String name) {
        requireActivity().getSharedPreferences("SportPrefs", Context.MODE_PRIVATE).edit()
                .putInt("remind_h", h).putInt("remind_m", m).putString("remind_name", name).apply();
    }

    private void loadSavedReminder() {
        SharedPreferences p = requireActivity().getSharedPreferences("SportPrefs", Context.MODE_PRIVATE);
        if (p.contains("remind_h")) {
            int h = p.getInt("remind_h", 8); int m = p.getInt("remind_m", 0); String n = p.getString("remind_name", "Exercise");
            if (tvReminderTime != null) tvReminderTime.setText(String.format(Locale.getDefault(), "%s at %02d:%02d", n, h, m));
        }
    }

    private void startWorkout() {
        if (getActivity() == null) return;
        Toast.makeText(getActivity(), "Starting your personal plan...", Toast.LENGTH_SHORT).show();
        try {
            HealthConnectClient client = HealthConnectClient.getOrCreate(requireContext());
            HealthDataFetcher.INSTANCE.fetchRealData(client, new HealthDataFetcher.FetchCallback() {
                @Override
                public void onSuccess(long steps, long avgHeartRate) {
                    startTime = System.currentTimeMillis();
                    timerHandler.postDelayed(timerRunnable, 0);
                    SharedPreferences prefs = requireActivity().getSharedPreferences(ActivityDeviceConnect.PREFS_NAME, Context.MODE_PRIVATE);
                    prefs.edit().putBoolean("workout_active", true).putLong("workout_start_time", startTime).putLong("workout_start_steps", steps).apply();
                    requireActivity().runOnUiThread(() -> updateSportUi());
                }
                @Override public void onError(String error) { startWorkoutFallback(); }
            });
        } catch (Exception e) { startWorkoutFallback(); }
    }

    private void finishWorkout() {
        if (getActivity() == null) return;
        try {
            HealthConnectClient client = HealthConnectClient.getOrCreate(requireContext());
            HealthDataFetcher.INSTANCE.fetchRealData(client, new HealthDataFetcher.FetchCallback() {
                @Override
                public void onSuccess(long finalSteps, long avgHeartRate) {
                    timerHandler.removeCallbacks(timerRunnable);
                    SharedPreferences prefs = requireActivity().getSharedPreferences(ActivityDeviceConnect.PREFS_NAME, Context.MODE_PRIVATE);
                    long startSteps = prefs.getLong("workout_start_steps", 0);
                    long deltaSteps = Math.max(50, finalSteps - startSteps);
                    String oldStepsStr = prefs.getString(ActivityDeviceConnect.KEY_STEPS, "0").replaceAll("[^0-9]", "");
                    long oldSteps = oldStepsStr.isEmpty() ? 0 : Long.parseLong(oldStepsStr);
                    String totalStepsFormatted = String.format("%,d", oldSteps + deltaSteps);
                    prefs.edit().putBoolean("workout_active", false).putString(ActivityDeviceConnect.KEY_STEPS, totalStepsFormatted).apply();
                    requireActivity().runOnUiThread(() -> {
                        new AlertDialog.Builder(requireContext()).setTitle("Summary").setMessage("Steps: " + deltaSteps).setPositiveButton("OK", null).show();
                        updateSportUi();
                    });
                }
                @Override public void onError(String error) { finishWorkoutFallback(); }
            });
        } catch (Exception e) { finishWorkoutFallback(); }
    }

    private void startWorkoutFallback() {
        startTime = System.currentTimeMillis();
        timerHandler.postDelayed(timerRunnable, 0);
        requireActivity().getSharedPreferences(ActivityDeviceConnect.PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean("workout_active", true).putLong("workout_start_time", startTime).apply();
        updateSportUi();
    }

    private void finishWorkoutFallback() {
        timerHandler.removeCallbacks(timerRunnable);
        requireActivity().getSharedPreferences(ActivityDeviceConnect.PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean("workout_active", false).apply();
        updateSportUi();
    }

    private void updateSportUi() {
        if (getActivity() == null || getView() == null) return;
        SharedPreferences prefs = requireActivity().getSharedPreferences(ActivityDeviceConnect.PREFS_NAME, Context.MODE_PRIVATE);
        boolean isConnected = prefs.getBoolean(ActivityDeviceConnect.KEY_DEVICE_CONNECTED, false);
        boolean workoutActive = prefs.getBoolean("workout_active", false);

        if (tvDeviceStatus != null) {
            tvDeviceStatus.setText(isConnected ? "Device Connected" : "Not Connected");
            tvDeviceStatus.setTextColor(isConnected ? Color.GREEN : Color.GRAY);
        }
        if (btnStartWorkout != null) btnStartWorkout.setEnabled(isConnected && !workoutActive);
        if (btnFinishWorkout != null) btnFinishWorkout.setEnabled(workoutActive);
    }

    private void syncRealHealthData() {
        if (getActivity() == null) return;

        SharedPreferences prefs = requireActivity().getSharedPreferences(ActivityDeviceConnect.PREFS_NAME, Context.MODE_PRIVATE);
        boolean isConnected = prefs.getBoolean(ActivityDeviceConnect.KEY_DEVICE_CONNECTED, false);

        if (!isConnected) return;

        try {
            HealthConnectClient client = HealthConnectClient.getOrCreate(requireContext());
            HealthDataFetcher.INSTANCE.fetchRealData(client, new HealthDataFetcher.FetchCallback() {
                @Override
                public void onSuccess(long steps, long avgHeartRate) {
                    if (getActivity() == null) return;

                    String stepsDisplay = String.format(Locale.getDefault(), "%,d", steps);
                    String hrDisplay = avgHeartRate > 0 ? avgHeartRate + " BPM" : "--";

                    boolean workoutActive = prefs.getBoolean("workout_active", false);

                    if (!workoutActive) {
                        prefs.edit()
                                .putString(ActivityDeviceConnect.KEY_HEART_RATE, hrDisplay)
                                .putString(ActivityDeviceConnect.KEY_STEPS, stepsDisplay)
                                .apply();

                        requireActivity().runOnUiThread(() -> updateSportUi());
                    }
                }

                @Override
                public void onError(String error) {
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSportUi();
        syncRealHealthData();
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        timerHandler.removeCallbacks(timerRunnable);
    }
}