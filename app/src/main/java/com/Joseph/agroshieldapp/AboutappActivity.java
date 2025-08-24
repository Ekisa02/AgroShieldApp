package com.Joseph.agroshieldapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Objects;

public class AboutappActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aboutapp);

        // Set up toolbar - FIXED: Now the toolbar exists in the layout
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("About AgroShield");

        // Set all text content
        TextView tvAppDescription = findViewById(R.id.tv_app_description);
        tvAppDescription.setText("Tired of guesswork, crop loss, and unfair prices? Welcome to AgroShield – the revolutionary mobile platform that puts the power of artificial intelligence, market access, and expert knowledge directly into the hands of farmers. We're not just an app; we're your trusted partner on the journey from seed to sale, designed to secure your harvest and maximize your income.\n\nAgroShield is built for the unique challenges and opportunities of modern farming, especially for smallholder farmers in Africa. We bridge the gap between traditional knowledge and cutting-edge technology, creating a sustainable and profitable future for agriculture.");

        // Initialize sections with your custom icons
        setupSection(R.id.section_transforms,
                R.drawable.ic_transformation,
                "How AgroShield Transforms Your Farming Business",
                "Imagine having a team of agronomists, a personal market broker, and a weather station in your pocket. AgroShield makes this a reality.\n\n• Become Your Own Crop Doctor: Stop losing sleep over sick plants.\n• Cut Out the Middleman: Why let others profit from your hard work?\n• Make Data-Driven Decisions: No more relying on outdated almanacs.\n• Find Supplies Instantly: Once you have a diagnosis, AgroShield doesn't leave you hanging.\n• Farm Smarter, Not Harder: Unsure what to plant next season?");

        setupSection(R.id.section_features,
                R.drawable.ic_features,
                "Deep Dive into Core Features",
                "🔍 AI-Powered Crop Health Scanner\n• Instant Diagnosis: Identify pests, fungi, and nutrient deficiencies in seconds.\n• Actionable Advice: Receive tailored treatment plans.\n• History Log: Track past diagnoses and treatments.\n\n📍 Smart Agrovet & Input Locator\n• Find Nearby Sellers: Locate the closest shops.\n• Compare Prices: See product listings from different vendors.\n• Stock Availability: Check if what you need is in stock.\n\n🤝 Digital Marketplace & Sourcing Hub\n• List Your Produce: Create beautiful listings with photos.\n• Discover Buyers: Browse offers from retailers and exporters.\n• Secure Transactions: Built-in messaging and trade facilitation.\n• Price Trends: Access real-time market price data.\n\n🌦️ Hyper-Local Weather & Climate Intelligence\n• 7-Day Forecasts: Precise rainfall and temperature predictions.\n• Early Warning Alerts: Get notifications for droughts or floods.\n• Climate-Smart Guidance: Recommendations on conservation techniques.\n\n📚 The Knowledge Hub: Learn & Grow\n• Expert Articles & Videos: Step-by-step guides on modern farming.\n• Voice-Based Tips: Listen to advice in your local language.\n• Community Forum: Ask questions and share experiences.\n\n📱 Built for Reality: Offline & Low-Data Mode\n• Work Anywhere: Capture images and check records offline.\n• Data Efficient: The app is designed to use minimal data.");

        setupSection(R.id.section_africa,
                R.drawable.ic_africa,
                "Designed for Africa, by Africa",
                "AgroShield stands apart because we understand the ground we operate on:\n\n• Culturally Relevant AI: Our disease detection models are trained on millions of images of African crops in African conditions for unparalleled accuracy.\n• Localized Ecosystem: We integrate with local agrovet networks and local buyer communities, creating a truly relevant economic tool.\n• Inclusive Design: We support multiple local languages and ensure the app is intuitive for users of all literacy levels.");

        setupSection(R.id.section_benefits,
                R.drawable.ic_benefits,
                "Who Else Benefits?",
                "For Farmers: Increase yields, reduce costs, and achieve financial stability.\n\nFor Buyers & Cooperatives: Gain a transparent, reliable, and efficient supply chain. Source quality produce directly and build long-term relationships with farmers.\n\nFor Governments & NGOs: Access aggregated, anonymized data on crop health, disease outbreaks, and yield predictions for better policy-making, targeted support, and rapid response to food security threats.\n\nFor Agrovets: Reach more customers digitally and understand local demand better to manage inventory.");

        // Set up download button
        Button downloadButton = findViewById(R.id.btn_download);
        downloadButton.setOnClickListener(v -> {
            // Handle download action
            Toast.makeText(this, "Redirecting to download...", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupSection(int sectionId, int iconRes, String title, String content) {
        View sectionView = findViewById(sectionId);

        ImageView sectionIcon = sectionView.findViewById(R.id.section_icon);
        TextView sectionTitle = sectionView.findViewById(R.id.section_title);
        ImageView expandIcon = sectionView.findViewById(R.id.expand_icon);
        LinearLayout sectionContent = sectionView.findViewById(R.id.section_content);
        TextView sectionDescription = sectionView.findViewById(R.id.section_description);

        // Set your custom expand icons
        expandIcon.setImageResource(R.drawable.ic_expand_more);

        sectionIcon.setImageResource(iconRes);
        sectionTitle.setText(title);
        sectionDescription.setText(content);

        View header = sectionView.findViewById(R.id.section_header);
        header.setOnClickListener(v -> toggleSection(sectionContent, expandIcon));
    }

    private void toggleSection(LinearLayout content, ImageView expandIcon) {
        if (content.getVisibility() == View.VISIBLE) {
            // Collapse section - use your custom expand_more icon
            content.setVisibility(View.GONE);
            expandIcon.setImageResource(R.drawable.ic_expand_more);
            animateRotation(expandIcon, 0);
        } else {
            // Expand section - use your custom expand_less icon
            content.setVisibility(View.VISIBLE);
            expandIcon.setImageResource(R.drawable.ic_expand_less);
            animateRotation(expandIcon, 180);
        }
    }

    private void animateRotation(ImageView imageView, float rotation) {
        imageView.animate()
                .rotation(rotation)
                .setDuration(300)
                .start();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}