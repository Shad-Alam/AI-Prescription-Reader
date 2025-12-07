package com.example.aiprescriptionreader;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OCRManager {

    public interface OCRCallback {
        void onSuccess(String extractedText, int confidence);
        void onError(String error);
    }

    private Context context;
    private SharedPreferences prefs;
    private ExecutorService executor;

    public OCRManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences("PrescriptionPrefs", Context.MODE_PRIVATE);
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void processImage(String imagePath, OCRCallback callback) {
        executor.execute(() -> {
            try {
                File file = new File(imagePath);
                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());

                if (bitmap == null) {
                    callback.onError("Failed to load image");
                    return;
                }

                InputImage image = InputImage.fromBitmap(bitmap, 0);
                TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

                recognizer.process(image)
                        .addOnSuccessListener(new OnSuccessListener<Text>() {
                            @Override
                            public void onSuccess(Text visionText) {
                                String extractedText = visionText.getText();

                                // Confidence calculation FIXED
                                int confidence = calculateConfidence(visionText);
                                updateStats();

                                if (confidence > 70) {
                                    updateSuccessfulScans();
                                }

                                callback.onSuccess(extractedText, confidence);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                callback.onError("OCR failed: " + e.getMessage());
                            }
                        });

            } catch (Exception e) {
                callback.onError("Error: " + e.getMessage());
            }
        });
    }

    private int calculateConfidence(Text visionText) {
        // ML Kit doesn't provide confidence per block in this version
        // So we use a simple heuristic

        int textLength = visionText.getText().length();

        if (textLength > 100) return 85;
        else if (textLength > 50) return 80;
        else if (textLength > 20) return 75;
        else if (textLength > 10) return 70;
        else return 65;
    }

    // Alternative: Count lines for confidence
    private int calculateConfidenceByLines(Text visionText) {
        int lineCount = 0;
        for (Text.TextBlock block : visionText.getTextBlocks()) {
            lineCount += block.getLines().size();
        }

        if (lineCount > 5) return 90;
        else if (lineCount > 3) return 80;
        else if (lineCount > 1) return 70;
        else return 60;
    }

    private void updateStats() {
        SharedPreferences.Editor editor = prefs.edit();

        long lastScanDate = prefs.getLong("last_scan_date", 0);
        long currentDate = System.currentTimeMillis();

        java.util.Calendar cal1 = java.util.Calendar.getInstance();
        java.util.Calendar cal2 = java.util.Calendar.getInstance();
        cal1.setTimeInMillis(lastScanDate);
        cal2.setTimeInMillis(currentDate);

        if (cal1.get(java.util.Calendar.DAY_OF_YEAR) != cal2.get(java.util.Calendar.DAY_OF_YEAR)) {
            editor.putInt("scans_today", 0);
            editor.putLong("last_scan_date", currentDate);
        }

        int scansToday = prefs.getInt("scans_today", 0);
        int totalScans = prefs.getInt("total_scans", 0);

        editor.putInt("scans_today", scansToday + 1);
        editor.putInt("total_scans", totalScans + 1);
        editor.apply();
    }

    private void updateSuccessfulScans() {
        int successful = prefs.getInt("successful_scans", 0);
        prefs.edit().putInt("successful_scans", successful + 1).apply();
    }
}