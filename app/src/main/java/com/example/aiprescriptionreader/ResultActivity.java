package com.example.aiprescriptionreader;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import java.io.File;

public class ResultActivity extends AppCompatActivity {

    private ImageView ivPrescription;
    private TextView tvConfidence, tvResult, tvAnalysis;
    private ProgressBar progressBar;
    private CardView cardAnalysis;
    private Button btnCopy, btnShare, btnRescan;

    private OCRManager ocrManager;
    private String imagePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        imagePath = getIntent().getStringExtra("image_path");

        initializeViews();
        loadImage();
        startOCR();
    }

    private void initializeViews() {
        ivPrescription = findViewById(R.id.ivPrescription);
        tvConfidence = findViewById(R.id.tvConfidence);
        tvResult = findViewById(R.id.tvResult);
        tvAnalysis = findViewById(R.id.tvAnalysis);
        progressBar = findViewById(R.id.progressBar);
        cardAnalysis = findViewById(R.id.cardAnalysis);
        btnCopy = findViewById(R.id.btnCopy);
        btnShare = findViewById(R.id.btnShare);
        btnRescan = findViewById(R.id.btnRescan);

        btnCopy.setOnClickListener(v -> copyText());
        btnShare.setOnClickListener(v -> shareResults());
        btnRescan.setOnClickListener(v -> {
            startActivity(new Intent(this, ScannerActivity.class));
            finish();
        });
    }

    private void loadImage() {
        try {
            File file = new File(imagePath);
            if (file.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                ivPrescription.setImageBitmap(bitmap);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startOCR() {
        progressBar.setVisibility(View.VISIBLE);
        tvResult.setText("Scanning prescription...");

        ocrManager = new OCRManager(this);
        ocrManager.processImage(imagePath, new OCRManager.OCRCallback() {
            @Override
            public void onSuccess(String extractedText, int confidence) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    tvResult.setText(extractedText);
                    tvConfidence.setText("Confidence: " + confidence + "%");

                    String analysis = MedicineAnalyzer.analyzePrescription(extractedText);
                    tvAnalysis.setText(analysis);
                    cardAnalysis.setVisibility(View.VISIBLE);

                    Toast.makeText(ResultActivity.this, "Scan successful!", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    tvResult.setText("Error: " + error);
                    Toast.makeText(ResultActivity.this, "Scan failed", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void copyText() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Prescription", tvResult.getText());
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Text copied", Toast.LENGTH_SHORT).show();
    }

    private void shareResults() {
        String textToShare = "Prescription Scan:\n\n" + tvResult.getText();

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, textToShare);
        startActivity(Intent.createChooser(shareIntent, "Share Prescription"));
    }
}