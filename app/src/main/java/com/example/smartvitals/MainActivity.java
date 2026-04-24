package com.example.smartvitals;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

public class MainActivity extends AppCompatActivity implements HomeFragment.EmergencyNavigationHost {

    private LinearLayout navHome, navSport, navScanSlot, navSupport, navProfile;
    private FrameLayout bgHomeHolder, bgSportHolder, bgSupportHolder, bgProfileHolder, navScanCenter;
    private ImageView ivNavHome, ivNavSport, ivNavScan, ivNavSupport, ivNavProfile;
    private TextView tvNavHome, tvNavSport, tvNavScan, tvNavSupport, tvNavProfile;
    private String currentTab = "home";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        bindViews();
        setupClicks();

        if (savedInstanceState == null) {
            switchTab("home");
        } else {
            currentTab = savedInstanceState.getString("tab", "home");
            updateNavStyles(currentTab);
        }
    }

    private void bindViews() {
        navHome = findViewById(R.id.navHome);
        navSport = findViewById(R.id.navSport);
        navScanSlot = findViewById(R.id.navScanSlot);
        navSupport = findViewById(R.id.navSupport);
        navProfile = findViewById(R.id.navProfile);

        bgHomeHolder = findViewById(R.id.bgHomeHolder);
        bgSportHolder = findViewById(R.id.bgSportHolder);
        bgSupportHolder = findViewById(R.id.bgSupportHolder);
        bgProfileHolder = findViewById(R.id.bgProfileHolder);
        navScanCenter = findViewById(R.id.navScanCenter);

        ivNavHome = findViewById(R.id.ivNavHome);
        ivNavSport = findViewById(R.id.ivNavSport);
        ivNavScan = findViewById(R.id.ivNavScan);
        ivNavSupport = findViewById(R.id.ivNavSupport);
        ivNavProfile = findViewById(R.id.ivNavProfile);

        tvNavHome = findViewById(R.id.tvNavHome);
        tvNavSport = findViewById(R.id.tvNavSport);
        tvNavScan = findViewById(R.id.tvNavScan);
        tvNavSupport = findViewById(R.id.tvNavSupport);
        tvNavProfile = findViewById(R.id.tvNavProfile);
    }

    private void setupClicks() {
        navHome.setOnClickListener(v -> switchTab("home"));
        navSport.setOnClickListener(v -> switchTab("sport"));
        navScanCenter.setOnClickListener(v -> switchTab("scan"));
        navScanSlot.setOnClickListener(v -> switchTab("scan"));
        navSupport.setOnClickListener(v -> switchTab("support"));
        navProfile.setOnClickListener(v -> switchTab("profile"));
    }

    private void switchTab(String tab) {
        currentTab = tab;

        Fragment selectedFragment;
        switch (tab) {
            case "sport":
                selectedFragment = new SportFragment();
                break;
            case "scan":
                selectedFragment = new ScanFragment();
                break;
            case "support":
                selectedFragment = new SupportFragment();
                break;
            case "profile":
                selectedFragment = new ProfileFragment();
                break;
            default:
                selectedFragment = new HomeFragment();
                break;
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, selectedFragment)
                .commit();

        updateNavStyles(tab);
    }

    @Override
    public void openSupportEmergency() {
        currentTab = "support";

        SupportFragment supportFragment = new SupportFragment();
        Bundle bundle = new Bundle();
        bundle.putBoolean("openEmergencyCall", true);
        supportFragment.setArguments(bundle);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, supportFragment)
                .commit();

        updateNavStyles("support");
    }

    private void updateNavStyles(String tab) {
        resetNavStyles();

        switch (tab) {
            case "sport":
                setRegularSelected(bgSportHolder, ivNavSport, tvNavSport);
                break;
            case "scan":
                navScanCenter.setBackgroundResource(R.drawable.bg_scan_fab_active);
                ivNavScan.setColorFilter(Color.WHITE);
                tvNavScan.setTextColor(Color.parseColor("#059669"));
                break;
            case "support":
                setRegularSelected(bgSupportHolder, ivNavSupport, tvNavSupport);
                break;
            case "profile":
                setRegularSelected(bgProfileHolder, ivNavProfile, tvNavProfile);
                break;
            default:
                setRegularSelected(bgHomeHolder, ivNavHome, tvNavHome);
                break;
        }
    }

    private void resetNavStyles() {
        setRegularUnselected(bgHomeHolder, ivNavHome, tvNavHome);
        setRegularUnselected(bgSportHolder, ivNavSport, tvNavSport);
        setRegularUnselected(bgSupportHolder, ivNavSupport, tvNavSupport);
        setRegularUnselected(bgProfileHolder, ivNavProfile, tvNavProfile);

        navScanCenter.setBackgroundResource(R.drawable.bg_scan_fab);
        ivNavScan.setColorFilter(Color.WHITE);
        tvNavScan.setTextColor(Color.parseColor("#94A3B8"));
    }

    private void setRegularSelected(FrameLayout holder, ImageView icon, TextView label) {
        holder.setBackgroundResource(R.drawable.bg_nav_item_selected);
        icon.setColorFilter(Color.parseColor("#059669"));
        label.setTextColor(Color.parseColor("#059669"));
    }

    private void setRegularUnselected(FrameLayout holder, ImageView icon, TextView label) {
        holder.setBackgroundColor(Color.TRANSPARENT);
        icon.setColorFilter(Color.parseColor("#94A3B8"));
        label.setTextColor(Color.parseColor("#94A3B8"));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("tab", currentTab);
    }
}