package com.Joseph.agroshieldapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.canhub.cropper.CropImage;
import com.canhub.cropper.CropImageActivity;
import com.canhub.cropper.CropImageView;
import androidx.exifinterface.media.ExifgitInterface;
import com.canhub.cropper.CropImage;
import com.canhub.cropper.CropImageView;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;
    private static final int REQUEST_PERMISSIONS = 3;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private StorageReference storageRef;

    // Views
    private ImageView profileImageView;
    private ImageButton btnChangePhoto;
    private Button btnSave;

    // Image handling
    private Uri currentImageUri;
    private String currentBase64Image;
    private boolean imageModified = false;

    // Permissions
    private static final String[] REQUIRED_PERMISSIONS;

    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            REQUIRED_PERMISSIONS = new String[]{
                    android.Manifest.permission.CAMERA,
                    android.Manifest.permission.READ_MEDIA_IMAGES
            };
        } else {
            REQUIRED_PERMISSIONS = new String[]{
                    android.Manifest.permission.CAMERA,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initializeFirebase();
        initializeViews();
        setupClickListeners();
        loadUserProfile();
    }

    private void initializeFirebase() {
        try {
            mAuth = FirebaseAuth.getInstance();
            currentUser = mAuth.getCurrentUser();
            db = FirebaseFirestore.getInstance();
            storageRef = FirebaseStorage.getInstance().getReference();
        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization failed: " + e.getMessage());
            showError("Database initialization failed");
        }
    }

    private void initializeViews() {
        profileImageView = findViewById(R.id.profileImageView);
        btnChangePhoto = findViewById(R.id.btnChangePhoto);
        btnSave = findViewById(R.id.btnSave);

        // Set circular transformation for profile image
        RequestOptions requestOptions = new RequestOptions()
                .circleCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder);

        Glide.with(this)
                .load(R.drawable.ic_profile_placeholder)
                .apply(requestOptions)
                .into(profileImageView);
    }

    private void setupClickListeners() {
        btnChangePhoto.setOnClickListener(v -> handleImageSelection());
        btnSave.setOnClickListener(v -> saveProfileImage());

        profileImageView.setOnClickListener(v -> {
            if (currentBase64Image != null) {
                viewImageFullScreen();
            }
        });
    }

    private void handleImageSelection() {
        if (!checkPermissions()) {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_PERMISSIONS);
            return;
        }

        showImageSourceDialog();
    }

    private void showImageSourceDialog() {
        CharSequence[] options = {"Take Photo", "Choose from Gallery", "Cancel"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Profile Picture");
        builder.setItems(options, (dialog, which) -> {
            if (options[which].equals("Take Photo")) {
                dispatchTakePictureIntent();
            } else if (options[which].equals("Choose from Gallery")) {
                openImagePicker();
            }
        });
        builder.show();
    }

    private void dispatchTakePictureIntent() {
        try {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                File photoFile = createImageFile();
                if (photoFile != null) {
                    currentImageUri = Uri.fromFile(photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentImageUri);
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
            }
        } catch (IOException ex) {
            Log.e(TAG, "Error creating image file: " + ex.getMessage());
            showError("Error accessing camera");
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    private File createImageFile() throws IOException {
        String timeStamp = String.valueOf(System.currentTimeMillis());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(null);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_IMAGE_CAPTURE:
                    handleCameraResult();
                    break;
                case REQUEST_IMAGE_PICK:
                    handleGalleryResult(data);
                    break;
                case CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE:
                    handleCropResult(data);
                    break;
            }
        }
    }

    private void handleCameraResult() {
        if (currentImageUri != null) {
            startImageCropper(currentImageUri);
        }
    }

    private void handleGalleryResult(Intent data) {
        if (data != null && data.getData() != null) {
            startImageCropper(data.getData());
        }
    }

    private void startImageCropper(Uri imageUri) {
        CropImage.activity(imageUri)
                .setGuidelines(CropImageView.Guidelines.ON)
                .setCropShape(CropImageView.CropShape.OVAL)
                .setAspectRatio(1, 1)
                .setRequestedSize(500, 500)
                .setFixAspectRatio(true)
                .setAllowRotation(true)
                .setAllowFlipping(true)
                .setAllowCounterRotation(true)
                .setAutoZoomEnabled(true)
                .setMultiTouchEnabled(true)
                .setInitialCropWindowPaddingRatio(0.1f)
                .setBorderLineThickness(3f)
                .setBorderCornerThickness(5f)
                .setBorderLineColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .setBorderCornerColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .start(this);
    }

    private void handleCropResult(Intent data) {
        CropImage.ActivityResult result = CropImage.getActivityResult(data);
        if (result != null && result.getUriContent() != null) {
            processCroppedImage(result.getUriContent());
        }
    }

    private void processCroppedImage(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            bitmap = handleImageRotation(bitmap, imageUri);

            // Compress and convert to Base64
            currentBase64Image = convertBitmapToBase64(bitmap);

            // Display the image
            Glide.with(this)
                    .load(bitmap)
                    .apply(new RequestOptions().circleCrop())
                    .into(profileImageView);

            imageModified = true;
            btnSave.setEnabled(true);

        } catch (IOException e) {
            Log.e(TAG, "Error processing image: " + e.getMessage());
            showError("Error processing image");
        }
    }

    private Bitmap handleImageRotation(Bitmap bitmap, Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream != null) {
                ExifInterface exif = new ExifInterface(inputStream);
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

                Matrix matrix = new Matrix();
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        matrix.postRotate(90);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        matrix.postRotate(180);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        matrix.postRotate(270);
                        break;
                }

                return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not correct image orientation: " + e.getMessage());
        }
        return bitmap;
    }

    private String convertBitmapToBase64(Bitmap bitmap) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] imageBytes = baos.toByteArray();
            return Base64.encodeToString(imageBytes, Base64.DEFAULT);
        } catch (Exception e) {
            Log.e(TAG, "Error converting image to Base64: " + e.getMessage());
            return null;
        }
    }

    private void saveProfileImage() {
        if (currentUser == null || currentBase64Image == null) {
            showError("No image to save");
            return;
        }

        showLoading("Saving profile picture...");

        // Upload to Firebase Storage first
        uploadImageToStorage();
    }

    private void uploadImageToStorage() {
        try {
            byte[] imageBytes = Base64.decode(currentBase64Image, Base64.DEFAULT);
            String fileName = "profile_" + currentUser.getUid() + "_" + System.currentTimeMillis() + ".jpg";
            StorageReference imageRef = storageRef.child("profile_images/" + fileName);

            UploadTask uploadTask = imageRef.putBytes(imageBytes);
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    saveImageDataToFirestore(uri.toString());
                }).addOnFailureListener(e -> {
                    hideLoading();
                    showError("Failed to get download URL: " + e.getMessage());
                });
            }).addOnFailureListener(e -> {
                hideLoading();
                showError("Upload failed: " + e.getMessage());
            });

        } catch (Exception e) {
            hideLoading();
            Log.e(TAG, "Error uploading image: " + e.getMessage());
            showError("Error uploading image");
        }
    }

    private void saveImageDataToFirestore(String imageUrl) {
        Map<String, Object> profileData = new HashMap<>();
        profileData.put("userId", currentUser.getUid());
        profileData.put("imageUrl", imageUrl);
        profileData.put("base64Image", currentBase64Image);
        profileData.put("timestamp", System.currentTimeMillis());
        profileData.put("email", currentUser.getEmail());

        db.collection("profileImages")
                .document(currentUser.getUid())
                .set(profileData)
                .addOnSuccessListener(aVoid -> {
                    hideLoading();
                    showSuccess("Profile picture saved successfully!");
                    imageModified = false;
                    btnSave.setEnabled(false);
                })
                .addOnFailureListener(e -> {
                    hideLoading();
                    showError("Failed to save profile: " + e.getMessage());
                });
    }

    private void loadUserProfile() {
        if (currentUser == null) return;

        db.collection("profileImages")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.contains("imageUrl")) {
                        String imageUrl = documentSnapshot.getString("imageUrl");
                        loadProfileImage(imageUrl);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error loading profile image: " + e.getMessage());
                });
    }

    private void loadProfileImage(String imageUrl) {
        Glide.with(this)
                .load(imageUrl)
                .apply(new RequestOptions()
                        .circleCrop()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .error(R.drawable.ic_profile_placeholder))
                .into(profileImageView);
    }

    private void viewImageFullScreen() {
        if (currentBase64Image != null) {
            Intent intent = new Intent(this, FullScreenImageActivity.class);
            intent.putExtra("image_base64", currentBase64Image);
            startActivity(intent);
        }
    }

    private boolean checkPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                handleImageSelection();
            } else {
                showError("Permissions required to access photos");
            }
        }
    }

    private void showLoading(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void hideLoading() {
        // Hide loading if you have a progress dialog
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showSuccess(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}