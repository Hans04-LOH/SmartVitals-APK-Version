package com.example.smartvitals;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class SupportFragment extends Fragment {

    private String dynamicEmergencyNumber = "999";
    private static final String AMBULANCE_NUMBER = "999";

    private static final String MYSJ_PACKAGE = "my.gov.malaysia.mysejahtera";

    private static final String FAB_PREFS = "support_fab_prefs";
    private static final String KEY_FAB_X = "fab_x";
    private static final String KEY_FAB_Y = "fab_y";

    private boolean hasHandledAutoEmergency = false;

    public SupportFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_support, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        fetchEmergencyContact();

        FloatingActionButton fabAiChatbot = view.findViewById(R.id.fabAiChatbot);
        AppCompatButton btnTestPopup = view.findViewById(R.id.btnTestPopup);
        AppCompatButton btnEmergencyContact = view.findViewById(R.id.btnEmergencyContact);
        TextView tvEmergencyInfoBubble = view.findViewById(R.id.tvEmergencyInfoBubble);

        TextView btnBookNow = view.findViewById(R.id.btnBookNow);
        TextView btnExplore = view.findViewById(R.id.btnExplore);

        LinearLayout btnNearPharmacy = view.findViewById(R.id.btnNearPharmacy);
        LinearLayout btnCallAmbulance = view.findViewById(R.id.btnCallAmbulance);
        LinearLayout btnMedicationHistory = view.findViewById(R.id.btnMedicationHistory);

        if (fabAiChatbot != null) {
            setupDraggableFab(fabAiChatbot);
        }

        if (btnEmergencyContact != null) {
            btnEmergencyContact.setOnClickListener(v -> openEmergencyDialer());
        }

        if (btnBookNow != null) {
            btnBookNow.setOnClickListener(v -> openHospitalBooking());
        }

        if (btnExplore != null) {
            btnExplore.setOnClickListener(v -> openCheckupService());
        }

        if (btnNearPharmacy != null) {
            btnNearPharmacy.setOnClickListener(v -> openNearbyPharmacy());
        }

        if (btnCallAmbulance != null) {
            btnCallAmbulance.setOnClickListener(v -> openAmbulanceDialer());
        }

        if (btnMedicationHistory != null) {
            btnMedicationHistory.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), MedicationHistoryActivity.class);
                startActivity(intent);
            });
        }

        if (btnTestPopup != null && tvEmergencyInfoBubble != null) {
            btnTestPopup.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        tvEmergencyInfoBubble.setVisibility(View.VISIBLE);
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float x = event.getX();
                        float y = event.getY();
                        boolean inside =
                                x >= 0 && x <= v.getWidth() &&
                                        y >= 0 && y <= v.getHeight();
                        tvEmergencyInfoBubble.setVisibility(inside ? View.VISIBLE : View.GONE);
                        return true;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        tvEmergencyInfoBubble.setVisibility(View.GONE);
                        return true;
                }
                return false;
            });
        }

        Bundle args = getArguments();
        boolean shouldOpenEmergencyCall =
                args != null && args.getBoolean("openEmergencyCall", false);

        if (shouldOpenEmergencyCall && !hasHandledAutoEmergency) {
            hasHandledAutoEmergency = true;

            if (args != null) {
                args.putBoolean("openEmergencyCall", false);
            }

            view.postDelayed(() -> {
                if (!isAdded()) return;
                openEmergencyDialer();
            }, 300);
        }
    }

    private void fetchEmergencyContact() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            FirebaseFirestore.getInstance().collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String contact = documentSnapshot.getString("emergencyContact");
                            if (contact != null && !contact.trim().isEmpty()) {
                                dynamicEmergencyNumber = contact;
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                    });
        }
    }

    private void setupDraggableFab(FloatingActionButton fab) {
        restoreFabPosition(fab);

        final float[] dX = new float[1];
        final float[] dY = new float[1];
        final boolean[] isDragging = new boolean[1];
        final float dragThreshold = 16f;

        fab.setOnTouchListener((v, event) -> {
            View parent = (View) v.getParent();
            if (parent == null) return false;

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    dX[0] = v.getX() - event.getRawX();
                    dY[0] = v.getY() - event.getRawY();
                    isDragging[0] = false;
                    return true;

                case MotionEvent.ACTION_MOVE:
                    float newX = event.getRawX() + dX[0];
                    float newY = event.getRawY() + dY[0];

                    float maxX = parent.getWidth() - v.getWidth();
                    float maxY = parent.getHeight() - v.getHeight();

                    newX = Math.max(0, Math.min(newX, maxX));
                    newY = Math.max(0, Math.min(newY, maxY));

                    if (Math.abs(v.getX() - newX) > dragThreshold || Math.abs(v.getY() - newY) > dragThreshold) {
                        isDragging[0] = true;
                    }

                    v.setX(newX);
                    v.setY(newY);
                    return true;

                case MotionEvent.ACTION_UP:
                    saveFabPosition(v.getX(), v.getY());

                    if (!isDragging[0]) {
                        openAiChat();
                    }
                    return true;

                default:
                    return false;
            }
        });
    }

    private void restoreFabPosition(FloatingActionButton fab) {
        fab.post(() -> {
            SharedPreferences prefs = requireContext().getSharedPreferences(FAB_PREFS, 0);

            float savedX = prefs.getFloat(KEY_FAB_X, -1f);
            float savedY = prefs.getFloat(KEY_FAB_Y, -1f);

            if (savedX >= 0f && savedY >= 0f) {
                View parent = (View) fab.getParent();
                if (parent != null) {
                    float maxX = parent.getWidth() - fab.getWidth();
                    float maxY = parent.getHeight() - fab.getHeight();

                    fab.setX(Math.max(0, Math.min(savedX, maxX)));
                    fab.setY(Math.max(0, Math.min(savedY, maxY)));
                }
            }
        });
    }

    private void saveFabPosition(float x, float y) {
        SharedPreferences prefs = requireContext().getSharedPreferences(FAB_PREFS, 0);
        prefs.edit()
                .putFloat(KEY_FAB_X, x)
                .putFloat(KEY_FAB_Y, y)
                .apply();
    }

    private void openAiChat() {
        Intent intent = new Intent(getActivity(), AiChatActivity.class);
        startActivity(intent);
    }

    private void openEmergencyDialer() {
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + dynamicEmergencyNumber));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Unable to open dialer.", Toast.LENGTH_SHORT).show();
        }
    }

    private void openAmbulanceDialer() {
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + AMBULANCE_NUMBER));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Unable to open ambulance dialer.", Toast.LENGTH_SHORT).show();
        }
    }

    private void openNearbyPharmacy() {
        try {
            Uri gmmIntentUri = Uri.parse("geo:0,0?q=pharmacy near me");
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            startActivity(mapIntent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Unable to open map.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isMySejahteraInstalled() {
        try {
            requireContext().getPackageManager().getPackageInfo(MYSJ_PACKAGE, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void openMySejahteraPlayStore() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + MYSJ_PACKAGE));
            startActivity(intent);
        } catch (Exception e) {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + MYSJ_PACKAGE));
            startActivity(intent);
        }
    }

    private void openMySejahteraHome(String message) {
        try {
            Intent launchIntent = requireContext()
                    .getPackageManager()
                    .getLaunchIntentForPackage(MYSJ_PACKAGE);

            if (launchIntent != null) {
                startActivity(launchIntent);
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            } else {
                openMySejahteraPlayStore();
            }
        } catch (Exception e) {
            openMySejahteraPlayStore();
        }
    }

    private void openHospitalBooking() {
        if (!isMySejahteraInstalled()) {
            Toast.makeText(requireContext(), "MySejahtera not found. Redirecting to Play Store...", Toast.LENGTH_SHORT).show();
            openMySejahteraPlayStore();
            return;
        }
        openMySejahteraHome("Opening MySejahtera for hospital booking...");
    }

    private void openCheckupService() {
        if (!isMySejahteraInstalled()) {
            Toast.makeText(requireContext(), "MySejahtera not found. Redirecting to Play Store...", Toast.LENGTH_SHORT).show();
            openMySejahteraPlayStore();
            return;
        }
        openMySejahteraHome("Opening MySejahtera for check-up services...");
    }
}