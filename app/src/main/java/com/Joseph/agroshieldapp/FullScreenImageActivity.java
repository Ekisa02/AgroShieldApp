package com.Joseph.agroshieldapp;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.Base64;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class FullScreenImageActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_BASE64 = "extra_image_base64";

    private ImageView imageView;
    private Matrix matrix = new Matrix();
    private float[] matrixValues = new float[9];
    private float minScale = 1f;
    private float maxScale = 4f;
    private float currentScale = 1f;

    private float lastX, lastY;
    private boolean isPanning = false;

    private ScaleGestureDetector scaleDetector;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_image);

        imageView = findViewById(R.id.fullImageView);
        imageView.setScaleType(ImageView.ScaleType.MATRIX);

        String base64 = getIntent().getStringExtra(EXTRA_IMAGE_BASE64);
        if (base64 != null && !base64.isEmpty()) {
            Bitmap bmp = decode(base64);
            if (bmp != null) {
                imageView.setImageBitmap(bmp);
                // Center image
                imageView.post(() -> {
                    centerImage();
                    imageView.setImageMatrix(matrix);
                });
            }
        }

        scaleDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float scaleFactor = detector.getScaleFactor();
                float newScale = clamp(currentScale * scaleFactor, minScale, maxScale);

                float factorToApply = newScale / currentScale;
                matrix.postScale(factorToApply, factorToApply, detector.getFocusX(), detector.getFocusY());
                currentScale = newScale;
                imageView.setImageMatrix(matrix);
                return true;
            }
        });

        imageView.setOnTouchListener((v, event) -> {
            scaleDetector.onTouchEvent(event);

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    lastX = event.getX();
                    lastY = event.getY();
                    isPanning = true;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (!scaleDetector.isInProgress() && isPanning) {
                        float dx = event.getX() - lastX;
                        float dy = event.getY() - lastY;
                        matrix.postTranslate(dx, dy);
                        imageView.setImageMatrix(matrix);
                        lastX = event.getX();
                        lastY = event.getY();
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    isPanning = false;
                    break;
            }
            return true;
        });

        // Tap anywhere or press back to exit full screen
        imageView.setOnClickListener(v -> finish());
    }

    private void centerImage() {
        if (imageView.getDrawable() == null) return;

        int viewW = imageView.getWidth();
        int viewH = imageView.getHeight();
        int imgW = imageView.getDrawable().getIntrinsicWidth();
        int imgH = imageView.getDrawable().getIntrinsicHeight();

        if (imgW <= 0 || imgH <= 0) return;

        float scale = Math.min((float) viewW / imgW, (float) viewH / imgH);
        currentScale = scale;
        minScale = scale;

        float dx = (viewW - imgW * scale) / 2f;
        float dy = (viewH - imgH * scale) / 2f;

        matrix.reset();
        matrix.postScale(scale, scale);
        matrix.postTranslate(dx, dy);
    }

    private float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }

    private Bitmap decode(String base64) {
        byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
        return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
}
