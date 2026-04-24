package com.example.smartvitals;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class PermissionsRationaleActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView textView = new TextView(this);
        textView.setPadding(40, 80, 40, 40);
        textView.setText("SmartVitals uses Health Connect permissions to read heart rate and steps data.");
        setContentView(textView);
    }
}