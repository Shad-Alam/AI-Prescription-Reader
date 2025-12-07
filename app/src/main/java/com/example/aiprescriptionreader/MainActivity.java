package com.example.aiprescriptionreader;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button btnStartScanner;
    private TextView tvScansToday, tvAccuracy;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("PrescriptionPrefs", MODE_PRIVATE);

        btnStartScanner = findViewById(R.id.btnStartScanner);
        tvScansToday = findViewById(R.id.tvScansToday);
        tvAccuracy = findViewById(R.id.tvAccuracy);

        btnStartScanner.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ScannerActivity.class);
            startActivity(intent);
        });

        updateStats();
    }

    private void updateStats() {
        int scansToday = prefs.getInt("scans_today", 0);
        int totalScans = prefs.getInt("total_scans", 0);
        int successfulScans = prefs.getInt("successful_scans", 0);

        tvScansToday.setText(String.valueOf(scansToday));

        if (totalScans > 0) {
            int accuracy = (successfulScans * 100) / totalScans;
            tvAccuracy.setText(accuracy + "%");
        } else {
            tvAccuracy.setText("95%");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStats();
    }
}