package com.example.aiprescriptionreader;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_GALLERY = 1;
    private static final int PICK_IMAGE_CAMERA = 2;
    private static final int CAMERA_PERMISSION_REQUEST = 100;

    private ImageView imageView;
    private TextView tvResult, tvSimpleInstruction;
    private Button btnCamera, btnGallery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI components
        imageView = findViewById(R.id.image_view);
        tvResult = findViewById(R.id.tv_result);
        tvSimpleInstruction = findViewById(R.id.tv_simple_instruction);
        btnCamera = findViewById(R.id.btn_camera);  // Change button ID
        btnGallery = findViewById(R.id.btn_gallery); // Add gallery button

        // Camera button - direct camera tulbe
        btnCamera.setOnClickListener(v -> checkCameraPermissionAndOpenCamera());

        // Gallery button - gallery theke select korbe
        btnGallery.setOnClickListener(v -> openGallery());
    }

    // ==================== CAMERA FUNCTIONALITY ====================
    private void checkCameraPermissionAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Camera permission na thakle request korun
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST);
        } else {
            // Camera permission already ache
            openCamera();
        }
    }

    private void openCamera() {
        try {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, PICK_IMAGE_CAMERA);
            } else {
                Toast.makeText(this, "Camera app not found!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Camera open error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // ==================== GALLERY FUNCTIONALITY ====================
    private void openGallery() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE_GALLERY);
        } catch (Exception e) {
            Toast.makeText(this, "Gallery open error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // ==================== PERMISSION HANDLING ====================
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Camera permission granted - camera open korun
                openCamera();
            } else {
                // Camera permission denied
                Toast.makeText(this, "Camera permission na thakle camera kaj korbe na!", Toast.LENGTH_LONG).show();
            }
        }
    }

    // ==================== IMAGE HANDLING ====================
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_IMAGE_CAMERA && data != null) {
                // Camera theke photo ela
                handleCameraImage(data);
            } else if (requestCode == PICK_IMAGE_GALLERY && data != null) {
                // Gallery theke photo ela
                handleGalleryImage(data);
            }
        }
    }

    private void handleCameraImage(Intent data) {
        try {
            Bitmap bitmap = (Bitmap) data.getExtras().get("data");
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
                // Camera theke ela bitmap theke text recognize korun
                recognizeTextFromBitmap(bitmap);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Camera image process error!", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void handleGalleryImage(Intent data) {
        Uri imageUri = data.getData();
        if (imageUri != null) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                imageView.setImageBitmap(bitmap);
                recognizeTextFromImageUri(imageUri);
            } catch (IOException e) {
                Toast.makeText(this, "Gallery image load error!", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }

    // ==================== TEXT RECOGNITION ====================
    private void recognizeTextFromBitmap(Bitmap bitmap) {
        try {
            tvResult.setText("Processing camera image...");

            InputImage image = InputImage.fromBitmap(bitmap, 0);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            recognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        String extractedText = visionText.getText();
                        displayResults(extractedText);
                        Toast.makeText(MainActivity.this, "Text recognition successful!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        tvResult.setText("Text recognition failed: " + e.getMessage());
                        Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        } catch (Exception e) {
            tvResult.setText("Image processing error: " + e.getMessage());
        }
    }

    private void recognizeTextFromImageUri(Uri imageUri) {
        try {
            tvResult.setText("Processing gallery image...");

            InputImage image = InputImage.fromFilePath(this, imageUri);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            recognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        String extractedText = visionText.getText();
                        displayResults(extractedText);
                        Toast.makeText(MainActivity.this, "Text recognition successful!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        tvResult.setText("Text recognition failed: " + e.getMessage());
                        Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        } catch (IOException e) {
            tvResult.setText("Image file error: " + e.getMessage());
        }
    }

    private void displayResults(String extractedText) {
        tvResult.setText("Extracted Text:\n" + extractedText);
        String simpleInstruction = processPrescriptionText(extractedText);
        tvSimpleInstruction.setText(simpleInstruction);
    }

    // ==================== TEXT PROCESSING ====================
    private String processPrescriptionText(String extractedText) {
        if (extractedText == null || extractedText.trim().isEmpty()) {
            return "❌ No text detected. Please try with clear handwriting.";
        }

        String lowerText = extractedText.toLowerCase();
        StringBuilder instruction = new StringBuilder();

        instruction.append("🧾 SIMPLE INSTRUCTIONS:\n\n");

        // Medicine detection
        if (lowerText.contains("amoxicillin") || lowerText.contains("amox") || lowerText.contains("amoxy")) {
            instruction.append("✅ Amoxicillin:\n");
            instruction.append("• Antibiotic\n");
            instruction.append("• Complete the full course\n\n");
        }

        if (lowerText.contains("paracetamol") || lowerText.contains("para") || lowerText.contains("acetamol")) {
            instruction.append("✅ Paracetamol:\n");
            instruction.append("• Pain reliever/Fever reducer\n\n");
        }

        // Dosage detection
        if (lowerText.contains("500") && lowerText.contains("mg")) {
            instruction.append("💊 Dosage: 500mg\n");
        }

        // Schedule detection
        if (lowerText.contains("1+0+1") || lowerText.contains("1-0-1")) {
            instruction.append("⏰ Schedule: Morning 1, Evening 1\n");
        } else if (lowerText.contains("1+1+1")) {
            instruction.append("⏰ Schedule: Morning 1, Afternoon 1, Evening 1\n");
        }

        if (instruction.toString().equals("🧾 SIMPLE INSTRUCTIONS:\n\n")) {
            instruction.append("📝 Prescription detected. Check details manually.\n");
        }

        return instruction.toString();
    }
}