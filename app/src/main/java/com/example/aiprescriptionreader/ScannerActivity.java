package com.example.aiprescriptionreader;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScannerActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION = 100;
    private PreviewView previewView;
    private Button btnFlash, btnCapture, btnSwitch;

    private ImageCapture imageCapture;
    private Camera camera;
    private boolean isFlashOn = false;
    private int cameraFacing = CameraSelector.LENS_FACING_BACK;

    private ExecutorService cameraExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        previewView = findViewById(R.id.previewView);
        btnFlash = findViewById(R.id.btnFlash);
        btnCapture = findViewById(R.id.btnCapture);
        btnSwitch = findViewById(R.id.btnSwitch);

        btnFlash.setOnClickListener(v -> toggleFlash());
        btnCapture.setOnClickListener(v -> captureImage());
        btnSwitch.setOnClickListener(v -> switchCamera());

        if (checkPermission()) {
            startCamera();
        } else {
            requestPermission();
        }

        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private boolean checkPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(cameraFacing)
                        .build();

                cameraProvider.unbindAll();

                camera = cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageCapture);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void toggleFlash() {
        if (camera != null) {
            isFlashOn = !isFlashOn;
            if (isFlashOn) {
                btnFlash.setText("🔦");
                camera.getCameraControl().enableTorch(true);
            } else {
                btnFlash.setText("⚡");
                camera.getCameraControl().enableTorch(false);
            }
        }
    }

    private void switchCamera() {
        cameraFacing = (cameraFacing == CameraSelector.LENS_FACING_BACK) ?
                CameraSelector.LENS_FACING_FRONT : CameraSelector.LENS_FACING_BACK;
        startCamera();
    }

    private void captureImage() {
        if (imageCapture == null) return;

        File photoFile = createImageFile();
        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        String path = photoFile.getAbsolutePath();
                        Toast.makeText(ScannerActivity.this, "Image captured", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(ScannerActivity.this, ResultActivity.class);
                        intent.putExtra("image_path", path);
                        startActivity(intent);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Toast.makeText(ScannerActivity.this, "Capture failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private File createImageFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "PRESCRIPTION_" + timeStamp;
        File storageDir = getExternalFilesDir("Prescriptions");
        if (!storageDir.exists()) storageDir.mkdirs();
        return new File(storageDir, fileName + ".jpg");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}