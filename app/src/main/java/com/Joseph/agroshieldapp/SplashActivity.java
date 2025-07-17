package com.Joseph.agroshieldapp;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.*;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private ImageView logo;
    private TextView appName, slogan;
    private ProgressBar progressBar;

    private final String fullSlogan = "Protecting Crops, Empowering Farmers";
    private int index = 0;
    private final long typingDelay = 60;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        logo = findViewById(R.id.logo);
        appName = findViewById(R.id.app_name);
        slogan = findViewById(R.id.slogan);
        progressBar = findViewById(R.id.progress_bar);

        animateLogo();
    }

    private void animateLogo() {
        logo.setVisibility(View.VISIBLE);

        AnimationSet set = new AnimationSet(true);
        set.setInterpolator(new AccelerateDecelerateInterpolator());
        set.setDuration(1500);

        RotateAnimation rotate = new RotateAnimation(0, 360,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(800);

        ScaleAnimation scale = new ScaleAnimation(0.5f, 1f, 0.5f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        scale.setDuration(1500);

        set.addAnimation(rotate);
        set.addAnimation(scale);
        logo.startAnimation(set);

        new Handler().postDelayed(this::animateAppName, 1200);
    }

    private void animateAppName() {
        appName.setVisibility(View.VISIBLE);
        appName.setTranslationY(200);
        appName.setAlpha(0f);
        appName.animate()
                .translationY(0)
                .alpha(1f)
                .setDuration(800)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(this::startTypingEffect)
                .start();
    }

    private void startTypingEffect() {
        slogan.setVisibility(View.VISIBLE);
        slogan.setText("");
        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (index < fullSlogan.length()) {
                    slogan.append(String.valueOf(fullSlogan.charAt(index)));
                    index++;
                    handler.postDelayed(this, typingDelay);
                } else {
                    progressBar.setVisibility(View.VISIBLE);
                    new Handler().postDelayed(() -> {
                        Intent intent = new Intent(SplashActivity.this,OnboardingActivity.class);
                        startActivity(intent);
                        overridePendingTransition(R.anim.slide_up, R.anim.fade_out);
                        finish();
                    }, 2000);
                }
            }
        };
        handler.postDelayed(runnable, 500);
    }
}
