package com.Joseph.agroshieldapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.exifinterface.media.ExifInterface;

import java.io.IOException;
import java.io.InputStream;

public class CustomCropActivity extends AppCompatActivity {

    private ImageView imageView;
    private FrameLayout cropOverlay;
    private Button btnCrop, btnCancel;

    private Bitmap originalBitmap;
    private Uri imageUri;
    private float scaleFactor = 1.0f;
    private float lastX, lastY;
    private boolean isDragging = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_crop);

        imageView = findViewById(R.id.imageView);
        cropOverlay = findViewById(R.id.cropOverlay);
        btnCrop = findViewById(R.id.btnCrop);
        btnCancel = findViewById(R.id.btnCancel);

        imageUri = getIntent().getParcelableExtra("image_uri");
        if (imageUri != null) {
            loadImage();
        } else {
            finish();
        }

        setupTouchListeners();
        setupButtonListeners();
    }

    private void loadImage() {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            originalBitmap = BitmapFactory.decodeStream(inputStream);

            // Handle image rotation
            originalBitmap = handleImageRotation(originalBitmap, imageUri);

            imageView.setImageBitmap(originalBitmap);

        } catch (IOException e) {
            Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
            finish();
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
            // If we can't read EXIF data, return the original bitmap
        }
        return bitmap;
    }

    private void setupTouchListeners() {
        imageView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastX = event.getX();
                    lastY = event.getY();
                    isDragging = true;
                    return true;

                case MotionEvent.ACTION_MOVE:
                    if (isDragging) {
                        float deltaX = event.getX() - lastX;
                        float deltaY = event.getY() - lastY;

                        // Move the image
                        imageView.setTranslationX(imageView.getTranslationX() + deltaX);
                        imageView.setTranslationY(imageView.getTranslationY() + deltaY);

                        lastX = event.getX();
                        lastY = event.getY();
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    isDragging = false;
                    return true;
            }
            return false;
        });

        // Pinch to zoom
        cropOverlay.setOnTouchListener(new View.OnTouchListener() {
            private static final int INVALID_POINTER_ID = -1;
            private int activePointerId = INVALID_POINTER_ID;
            private float lastTouchX, lastTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN: {
                        final int pointerIndex = event.getActionIndex();
                        final float x = event.getX(pointerIndex);
                        final float y = event.getY(pointerIndex);

                        lastTouchX = x;
                        lastTouchY = y;
                        activePointerId = event.getPointerId(0);
                        break;
                    }

                    case MotionEvent.ACTION_MOVE: {
                        final int pointerIndex = event.findPointerIndex(activePointerId);
                        final float x = event.getX(pointerIndex);
                        final float y = event.getY(pointerIndex);

                        if (!isDragging) {
                            final float dx = x - lastTouchX;
                            final float dy = y - lastTouchY;

                            imageView.setTranslationX(imageView.getTranslationX() + dx);
                            imageView.setTranslationY(imageView.getTranslationY() + dy);

                            lastTouchX = x;
                            lastTouchY = y;
                        }
                        break;
                    }

                    case MotionEvent.ACTION_UP: {
                        activePointerId = INVALID_POINTER_ID;
                        break;
                    }

                    case MotionEvent.ACTION_CANCEL: {
                        activePointerId = INVALID_POINTER_ID;
                        break;
                    }

                    case MotionEvent.ACTION_POINTER_UP: {
                        final int pointerIndex = event.getActionIndex();
                        final int pointerId = event.getPointerId(pointerIndex);

                        if (pointerId == activePointerId) {
                            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                            lastTouchX = event.getX(newPointerIndex);
                            lastTouchY = event.getY(newPointerIndex);
                            activePointerId = event.getPointerId(newPointerIndex);
                        }
                        break;
                    }
                }
                return true;
            }
        });
    }

    private void setupButtonListeners() {
        btnCrop.setOnClickListener(v -> performCrop());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void performCrop() {
        // Calculate crop area (oval shape)
        int overlaySize = Math.min(cropOverlay.getWidth(), cropOverlay.getHeight());
        int cropSize = (int) (overlaySize * 0.8f); // 80% of overlay size

        // Calculate position relative to image
        float[] imagePosition = getImagePositionInOverlay();

        // Create oval mask
        Bitmap croppedBitmap = createOvalCroppedBitmap(originalBitmap, imagePosition[0], imagePosition[1], cropSize);

        if (croppedBitmap != null) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("cropped_bitmap", croppedBitmap);
            setResult(RESULT_OK, resultIntent);
            finish();
        }
    }

    private float[] getImagePositionInOverlay() {
        // Calculate where the image is positioned relative to the overlay
        int[] overlayLocation = new int[2];
        cropOverlay.getLocationOnScreen(overlayLocation);

        int[] imageLocation = new int[2];
        imageView.getLocationOnScreen(imageLocation);

        float relativeX = imageLocation[0] - overlayLocation[0] - imageView.getTranslationX();
        float relativeY = imageLocation[1] - overlayLocation[1] - imageView.getTranslationY();

        return new float[]{relativeX, relativeY};
    }

    private Bitmap createOvalCroppedBitmap(Bitmap bitmap, float x, float y, int size) {
        try {
            // Calculate scale factor
            float scale = (float) bitmap.getWidth() / imageView.getWidth();

            // Calculate crop coordinates
            int cropX = (int) (x * scale);
            int cropY = (int) (y * scale);
            int cropSize = (int) (size * scale);

            // Ensure coordinates are within bounds
            cropX = Math.max(0, Math.min(cropX, bitmap.getWidth() - cropSize));
            cropY = Math.max(0, Math.min(cropY, bitmap.getHeight() - cropSize));

            // Create oval mask
            Bitmap output = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(output);

            final Paint paint = new Paint();
            final Rect rect = new Rect(0, 0, cropSize, cropSize);
            final RectF rectF = new RectF(rect);

            paint.setAntiAlias(true);
            canvas.drawARGB(0, 0, 0, 0);
            canvas.drawOval(rectF, paint);

            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(Bitmap.createBitmap(bitmap, cropX, cropY, cropSize, cropSize), rect, rect, paint);

            return output;

        } catch (Exception e) {
            Toast.makeText(this, "Error cropping image", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (originalBitmap != null && !originalBitmap.isRecycled()) {
            originalBitmap.recycle();
        }
    }
}