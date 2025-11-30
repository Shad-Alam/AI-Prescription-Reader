package com.example.aiprescriptionreader;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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
        initializeViews();

        // Set up button click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        imageView = findViewById(R.id.image_view);
        tvResult = findViewById(R.id.tv_result);
        tvSimpleInstruction = findViewById(R.id.tv_simple_instruction);
        btnCamera = findViewById(R.id.btn_camera);
        btnGallery = findViewById(R.id.btn_gallery);
    }

    private void setupClickListeners() {
        // Camera button - direct camera tulbe
        btnCamera.setOnClickListener(v -> checkCameraPermissionAndOpenCamera());

        // Gallery button - gallery theke select korbe
        btnGallery.setOnClickListener(v -> openGallery());
    }

    // ==================== CAMERA FUNCTIONALITY ====================
    private void checkCameraPermissionAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST);
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        try {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, PICK_IMAGE_CAMERA);
            } else {
                showToast("Camera app not found!");
            }
        } catch (Exception e) {
            showToast("Camera open error: " + e.getMessage());
        }
    }

    // ==================== GALLERY FUNCTIONALITY ====================
    private void openGallery() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE_GALLERY);
        } catch (Exception e) {
            showToast("Gallery open error: " + e.getMessage());
        }
    }

    // ==================== PERMISSION HANDLING ====================
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                showToast("Camera permission required for this feature!");
            }
        }
    }

    // ==================== IMAGE HANDLING ====================
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_IMAGE_CAMERA && data != null) {
                handleCameraImage(data);
            } else if (requestCode == PICK_IMAGE_GALLERY && data != null) {
                handleGalleryImage(data);
            }
        }
    }

    private void handleCameraImage(Intent data) {
        try {
            Bitmap bitmap = (Bitmap) data.getExtras().get("data");
            if (bitmap != null) {
                // Preprocess image for better recognition
                Bitmap processedBitmap = preprocessImage(bitmap);
                imageView.setImageBitmap(processedBitmap);
                recognizeTextFromBitmap(processedBitmap);
            }
        } catch (Exception e) {
            showToast("Camera image processing error!");
            e.printStackTrace();
        }
    }

    private void handleGalleryImage(Intent data) {
        Uri imageUri = data.getData();
        if (imageUri != null) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                // Preprocess image for better recognition
                Bitmap processedBitmap = preprocessImage(bitmap);
                imageView.setImageBitmap(processedBitmap);
                recognizeTextFromBitmap(processedBitmap);
            } catch (IOException e) {
                showToast("Gallery image load error!");
                e.printStackTrace();
            }
        }
    }

    // ==================== IMAGE PREPROCESSING ====================
    private Bitmap preprocessImage(Bitmap originalBitmap) {
        try {
            // 1. Resize image for better processing
            Bitmap resizedBitmap = resizeBitmap(originalBitmap, 1024);

            // 2. Convert to grayscale (helps text recognition)
            Bitmap grayBitmap = toGrayscale(resizedBitmap);

            // 3. Enhance contrast
            Bitmap enhancedBitmap = enhanceContrast(grayBitmap);

            return enhancedBitmap;

        } catch (Exception e) {
            return originalBitmap;
        }
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int maxDimension) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float scale = Math.min((float) maxDimension / width, (float) maxDimension / height);

        int newWidth = (int) (width * scale);
        int newHeight = (int) (height * scale);

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    private Bitmap toGrayscale(Bitmap bitmap) {
        Bitmap grayBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);

        for (int x = 0; x < bitmap.getWidth(); x++) {
            for (int y = 0; y < bitmap.getHeight(); y++) {
                int pixel = bitmap.getPixel(x, y);
                int r = (pixel >> 16) & 0xff;
                int g = (pixel >> 8) & 0xff;
                int b = pixel & 0xff;

                int gray = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                int grayPixel = (0xFF << 24) | (gray << 16) | (gray << 8) | gray;
                grayBitmap.setPixel(x, y, grayPixel);
            }
        }
        return grayBitmap;
    }

    private Bitmap enhanceContrast(Bitmap bitmap) {
        // Simple contrast enhancement
        Bitmap enhancedBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);

        for (int x = 0; x < bitmap.getWidth(); x++) {
            for (int y = 0; y < bitmap.getHeight(); y++) {
                int pixel = bitmap.getPixel(x, y);
                int gray = pixel & 0xff;

                // Enhance contrast
                int newGray = gray < 128 ? Math.max(0, gray - 20) : Math.min(255, gray + 20);
                int newPixel = (0xFF << 24) | (newGray << 16) | (newGray << 8) | newGray;
                enhancedBitmap.setPixel(x, y, newPixel);
            }
        }
        return enhancedBitmap;
    }

    // ==================== TEXT RECOGNITION ====================
    private void recognizeTextFromBitmap(Bitmap bitmap) {
        try {
            tvResult.setText("🔄 Processing image with enhanced AI...");
            tvSimpleInstruction.setText("Please wait...");

            InputImage image = InputImage.fromBitmap(bitmap, 0);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            recognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        String extractedText = visionText.getText();
                        String cleanedText = cleanExtractedText(extractedText);
                        displayResults(cleanedText);
                        showToast("✅ Text recognition successful!");
                    })
                    .addOnFailureListener(e -> {
                        tvResult.setText("❌ Recognition failed: " + e.getMessage());
                        tvSimpleInstruction.setText("Please try with a clearer image.");
                        showToast("Error: " + e.getMessage());
                    });
        } catch (Exception e) {
            tvResult.setText("❌ Processing error: " + e.getMessage());
        }
    }

    // ==================== TEXT CLEANING & PROCESSING ====================
    private String cleanExtractedText(String extractedText) {
        if (extractedText == null) return "No text detected";

        StringBuilder cleanedText = new StringBuilder();
        String[] lines = extractedText.split("\n");

        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                String cleanedLine = line
                        .replace("|", "I")
                        .replace("0", "O")
                        .replace("1", "I")
                        .replace("5", "S")
                        .replace("8", "B")
                        .replace("l", "I")
                        .trim();

                if (cleanedLine.length() >= 2) {
                    cleanedText.append(cleanedLine).append("\n");
                }
            }
        }

        return cleanedText.toString().trim();
    }

    private void displayResults(String extractedText) {
        tvResult.setText("📝 Extracted Text:\n" + extractedText);
        String simpleInstruction = processPrescriptionText(extractedText);
        tvSimpleInstruction.setText(simpleInstruction);
    }

    // ==================== PRESCRIPTION PROCESSING ====================
    private String processPrescriptionText(String extractedText) {
        if (extractedText == null || extractedText.trim().isEmpty() || extractedText.equals("No text detected")) {
            return "❌ No text detected!\n\n💡 Tips for better results:\n• Good lighting\n• Clear handwriting\n• Straight photo\n• Plain background";
        }

        String lowerText = extractedText.toLowerCase();
        StringBuilder instruction = new StringBuilder();

        instruction.append("🧾 DETECTED MEDICINES:\n\n");

        // Medicine patterns with common OCR errors
        String[][] medicinePatterns = {
                {"amoxicillin", "amox", "amoxy", "amoxicil", "amoxi", "amoxc"},
                {"paracetamol", "para", "acetamol", "paraceta", "paracet"},
                {"vitamin", "vit", "bcomplex", "b-comp", "bcom"},
                {"cephalexin", "cepha", "cephalesin", "ceph"},
                {"azithromycin", "azithro", "azee", "azi", "azrth"},
                {"omeprazole", "omepra", "omez", "ome", "omprz"}
        };

        boolean medicineFound = false;
        for (String[] patterns : medicinePatterns) {
            for (String pattern : patterns) {
                if (lowerText.contains(pattern)) {
                    medicineFound = true;
                    instruction.append("✅ ").append(capitalize(patterns[0])).append(":\n");

                    // Medicine-specific instructions
                    if (patterns[0].contains("amoxicillin")) {
                        instruction.append("• Antibiotic for infections\n");
                        instruction.append("• Take complete course\n");
                    } else if (patterns[0].contains("paracetamol")) {
                        instruction.append("• Pain and fever relief\n");
                        instruction.append("• Take after food\n");
                    } else if (patterns[0].contains("vitamin")) {
                        instruction.append("• Vitamin supplement\n");
                        instruction.append("• Take with food\n");
                    } else if (patterns[0].contains("cephalexin")) {
                        instruction.append("• Antibiotic\n");
                        instruction.append("• Complete full course\n");
                    }
                    instruction.append("\n");
                    break;
                }
            }
        }

        if (!medicineFound) {
            instruction.append("❌ No specific medicine detected.\n\n");
            instruction.append("📄 Detected Text:\n");
            instruction.append(extractedText).append("\n\n");
            instruction.append("💡 Try with clearer handwriting");
        } else {
            // Add dosage and schedule information
            instruction.append("💊 DOSAGE & SCHEDULE:\n");

            String dosage = extractDosage(lowerText);
            if (!dosage.isEmpty()) {
                instruction.append("• Strength: ").append(dosage).append("\n");
            }

            String schedule = extractSchedule(lowerText);
            if (!schedule.isEmpty()) {
                instruction.append("• ").append(schedule).append("\n");
            }

            String duration = extractDuration(lowerText);
            if (!duration.isEmpty()) {
                instruction.append("• ").append(duration).append("\n");
            }

            instruction.append("\n⚠️ Always verify with your doctor!");
        }

        return instruction.toString();
    }

    private String extractDosage(String text) {
        if (text.matches(".*\\b500\\b.*") && text.contains("mg")) return "500mg";
        if (text.matches(".*\\b250\\b.*") && text.contains("mg")) return "250mg";
        if (text.matches(".*\\b100\\b.*") && text.contains("mg")) return "100mg";
        if (text.contains("mg")) return "Check dosage in text";
        return "";
    }

    private String extractSchedule(String text) {
        if (text.contains("1+0+1") || text.contains("1-0-1")) return "Schedule: Morning 1, Evening 1";
        if (text.contains("1+1+1") || text.contains("1-1-1")) return "Schedule: Morning 1, Afternoon 1, Evening 1";
        if (text.contains("0+0+1") || text.contains("0-0-1")) return "Schedule: Only Evening 1";
        if (text.contains("bd") || text.contains("b.d") || text.contains("bid")) return "Schedule: Twice daily";
        if (text.contains("tds") || text.contains("t.d.s") || text.contains("tid")) return "Schedule: Thrice daily";
        if (text.contains("once") || text.contains("1 time")) return "Schedule: Once daily";
        return "";
    }

    private String extractDuration(String text) {
        if (text.contains("7 days") || text.contains("7day")) return "Duration: 7 days";
        if (text.contains("5 days") || text.contains("5day")) return "Duration: 5 days";
        if (text.contains("3 days") || text.contains("3day")) return "Duration: 3 days";
        if (text.contains("10 days") || text.contains("10day")) return "Duration: 10 days";
        if (text.contains("day") || text.contains("days")) return "Duration: As prescribed";
        return "";
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}