package com.Joseph.agroshieldapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import java.util.Arrays;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private Button btnNext;
    private TextView tvSkip;
    private LinearLayout dotsLayout;
    private OnboardingAdapter adapter;
    private List<OnboardingItem> items;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        viewPager = findViewById(R.id.onboardingViewPager);
        btnNext = findViewById(R.id.btnNext);
        tvSkip = findViewById(R.id.tvSkip);
        dotsLayout = findViewById(R.id.dotsLayout);

        items = Arrays.asList(
                new OnboardingItem(R.drawable.agro1, "Smart Protection", "Detect threats early on your farm"),
                new OnboardingItem(R.drawable.agro2, "Real-time Alerts", "Monitor your farm 24/7 with AI"),
                new OnboardingItem(R.drawable.agro, "Report & Act", "Engage authorities and save your harvest")
        );

        adapter = new OnboardingAdapter(items);
        viewPager.setAdapter(adapter);
        setupDots(0);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            public void onPageSelected(int position) {
                setupDots(position);
                btnNext.setText(position == items.size() - 1 ? "Get Started" : "Next");
            }
        });

        btnNext.setOnClickListener(v -> {
            int pos = viewPager.getCurrentItem();
            if (pos < items.size() - 1) {
                viewPager.setCurrentItem(pos + 1);
            } else {
                // Proceed to Login/Signup
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            }
        });

        tvSkip.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void setupDots(int position) {
        dotsLayout.removeAllViews();
        for (int i = 0; i < items.size(); i++) {
            TextView dot = new TextView(this);
            dot.setText("•");
            dot.setTextSize(35);
            dot.setTextColor(i == position ? Color.GREEN : Color.LTGRAY);
            dotsLayout.addView(dot);
        }
    }
}
