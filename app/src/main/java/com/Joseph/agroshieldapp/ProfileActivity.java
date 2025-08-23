package com.Joseph.agroshieldapp;

import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileActivity extends AppCompatActivity {

    // UI
    private ImageView profileImageView;
    private ImageButton btnChangePhoto;
    private Button btnSave;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private ScrollView mainContent;

    // Data
    private Uri selectedImageUri = null;
    private String selectedImageBase64 = null;

    // Async
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Pick image launcher (system picker: no runtime storage permission needed)
    private ActivityResultLauncher<String> pickImageLauncher;

    private static final String KEY_STATE_BASE64 = "state_base64";
    private static final String KEY_STATE_IMAGE_URI = "state_image_uri";
    private static final String TAG = "ProfileActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        bindViews();
        setupPickImageLauncher();
        setupListeners();

        restoreState(savedInstanceState);
        updateUiState(false);
    }

    private void bindViews() {
        profileImageView = findViewById(R.id.profileImageView);
        btnChangePhoto = findViewById(R.id.btnChangePhoto);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);
        tvStatus = findViewById(R.id.tvStatus);
        mainContent = findViewById(R.id.mainContent);

        // ProgressBar as indeterminate
        progressBar.setIndeterminate(true);
    }

    private void setupPickImageLauncher() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri uri) {
                        if (uri == null) return;
                        handleImagePicked(uri);
                    }
                }
        );
    }

    private void setupListeners() {
        btnChangePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Use system picker to avoid storage permissions
                pickImageLauncher.launch("image/*");
            }
        });

        profileImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedImageBase64 == null || selectedImageBase64.isEmpty()) {
                    Toast.makeText(ProfileActivity.this, "No image selected yet", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(ProfileActivity.this, FullScreenImageActivity.class);
                intent.putExtra(FullScreenImageActivity.EXTRA_IMAGE_BASE64, selectedImageBase64);
                startActivity(intent);
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveImageToFirestore();
            }
        });
    }
    private void handleImagePicked(Uri uri) {
        setBusy(true, "Processing image...");
        selectedImageUri = uri;

        ioExecutor.execute(() -> {
            try {
                Bitmap bitmap = decodeBitmapDownsampled(uri, 1080, 1080);
                if (bitmap == null) throw new IllegalStateException("Unable to decode image");

                // Detect and crop face
                Bitmap faceBitmap = detectAndCropFaceSync(bitmap);
                if (faceBitmap == null) {
                    mainHandler.post(() ->
                            Toast.makeText(this, "No face detected — using full image", Toast.LENGTH_SHORT).show());
                    faceBitmap = bitmap;
                }

                // Scale down before encoding
                Bitmap compressed = maybeResize(faceBitmap, 1080);
                String base64 = encodeBitmapToBase64(compressed, 85);

                // Update UI
                mainHandler.post(() -> {
                    selectedImageBase64 = base64;
                    cacheProfileImage(base64); // save to cache
                    profileImageView.setImageBitmap(compressed);
                    updateUiState(true);
                    setBusy(false, null);
                });

            } catch (Exception e) {
                Log.e(TAG, "Image processing failed", e);
                mainHandler.post(() -> {
                    setBusy(false, null);
                    showError("Failed to process image. Try again.");
                });
            }
        });
    }

    private Bitmap detectAndCropFaceSync(Bitmap original) {
        try {
            FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                    .build();

            FaceDetector detector = FaceDetection.getClient(options);
            InputImage image = InputImage.fromBitmap(original, 0);

            // Blocking wait since we're on ioExecutor
            java.util.List<Face> faces = Tasks.await(detector.process(image));
            detector.close();

            if (!faces.isEmpty()) {
                Face face = faces.get(0); // First face found
                android.graphics.Rect box = face.getBoundingBox();

                // Clamp bounds
                int left = Math.max(0, box.left);
                int top = Math.max(0, box.top);
                int right = Math.min(original.getWidth(), box.right);
                int bottom = Math.min(original.getHeight(), box.bottom);

                return Bitmap.createBitmap(original, left, top, right - left, bottom - top);
            }
        } catch (Exception e) {
            Log.e(TAG, "Face detection failed: " + e.getMessage());
        }
        return null;
    }
    private void saveImageToFirestore() {
        if (selectedImageBase64 == null || selectedImageBase64.isEmpty()) {
            showError("Please choose an image first.");
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            showError("Not signed in.");
            return;
        }

        setBusy(true, "Saving image...");

        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getUid());
        data.put("imageBase64", selectedImageBase64);
        data.put("updatedAt", Timestamp.now());

        FirebaseFirestore.getInstance()
                .collection("profileimages")
                .document(user.getUid()) // use userId as document id for idempotency
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    setBusy(false, null);
                    Toast.makeText(this, "Profile image saved", Toast.LENGTH_SHORT).show();

                    // Navigate back to AccountFragment (parent handles this result)
                    Intent result = new Intent();
                    result.putExtra("navigateTo", "AccountFragment");
                    setResult(Activity.RESULT_OK, result);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore save failed", e);
                    setBusy(false, null);
                    showError("Failed to save. Check your connection and try again.");
                });
    }

    // --- Helpers ---


    private void cacheProfileImage(String base64) {
        getSharedPreferences("profile_cache", MODE_PRIVATE)
                .edit()
                .putString("cached_profile_image", base64)
                .apply();
    }


    private void setBusy(boolean busy, @Nullable String message) {
        progressBar.setVisibility(busy ? View.VISIBLE : View.GONE);
        tvStatus.setText(message != null ? message : "");
        tvStatus.setVisibility(busy && message != null ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!busy && selectedImageBase64 != null && !selectedImageBase64.isEmpty());
        btnChangePhoto.setEnabled(!busy);
        mainContent.setEnabled(!busy);
    }

    private void updateUiState(boolean hasImage) {
        btnSave.setEnabled(hasImage);
    }

    private void showError(String msg) {
        tvStatus.setText(msg);
        tvStatus.setVisibility(View.VISIBLE);
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private Bitmap decodeBitmapDownsampled(Uri uri, int reqWidth, int reqHeight) throws Exception {
        ContentResolver resolver = getContentResolver();

        // First decode with inJustDecodeBounds=true to check dimensions
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream is = resolver.openInputStream(uri)) {
            BitmapFactory.decodeStream(is, null, bounds);
        }

        // Calculate inSampleSize
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = calculateInSampleSize(bounds, reqWidth, reqHeight);
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;

        try (InputStream is2 = resolver.openInputStream(uri)) {
            return BitmapFactory.decodeStream(is2, null, opts);
        }
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return Math.max(1, inSampleSize);
    }

    private Bitmap maybeResize(Bitmap src, int maxDim) {
        int w = src.getWidth();
        int h = src.getHeight();
        int max = Math.max(w, h);
        if (max <= maxDim) return src;

        float scale = (float) maxDim / (float) max;
        int newW = Math.round(w * scale);
        int newH = Math.round(h * scale);
        return Bitmap.createScaledBitmap(src, newW, newH, true);
    }

    private String encodeBitmapToBase64(Bitmap bitmap, int quality) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        byte[] bytes = baos.toByteArray();
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    private Bitmap decodeBase64ToBitmap(String base64) {
        byte[] decoded = Base64.decode(base64, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (selectedImageBase64 != null) outState.putString(KEY_STATE_BASE64, selectedImageBase64);
        if (selectedImageUri != null) outState.putString(KEY_STATE_IMAGE_URI, selectedImageUri.toString());
    }

    private void restoreState(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) return;
        selectedImageBase64 = savedInstanceState.getString(KEY_STATE_BASE64);
        String uriStr = savedInstanceState.getString(KEY_STATE_IMAGE_URI);
        if (uriStr != null) selectedImageUri = Uri.parse(uriStr);

        if (selectedImageBase64 != null) {
            Bitmap bmp = decodeBase64ToBitmap(selectedImageBase64);
            if (bmp != null) {
                profileImageView.setImageBitmap(bmp);
                updateUiState(true);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ioExecutor.shutdownNow();
    }
}
