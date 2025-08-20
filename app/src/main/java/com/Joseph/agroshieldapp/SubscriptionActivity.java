package com.Joseph.agroshieldapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SubscriptionActivity extends AppCompatActivity {

    private RadioGroup userTypeGroup;
    private RadioButton individualRadio, companyRadio;
    private TextView priceTextView, benefitsTextView;
    private Button subscribeButton;

    private FirebaseAuth mAuth;
    private DatabaseReference userRef;
    private FirebaseUser currentUser;

    private String selectedUserType = "individual";
    private double selectedPrice = 20.00;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscription);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        userRef = FirebaseDatabase.getInstance().getReference("Users");
        currentUser = mAuth.getCurrentUser();

        // Initialize views
        initViews();

        // Setup listeners
        setupListeners();

        // Load initial state
        updateUI();
    }

    private void initViews() {
        userTypeGroup = findViewById(R.id.userTypeGroup);
        individualRadio = findViewById(R.id.individualRadio);
        companyRadio = findViewById(R.id.companyRadio);
        priceTextView = findViewById(R.id.priceTextView);
        benefitsTextView = findViewById(R.id.benefitsTextView);
        subscribeButton = findViewById(R.id.subscribeButton);
    }

    private void setupListeners() {
        userTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.individualRadio) {
                selectedUserType = "individual";
                selectedPrice = 20.00;
            } else if (checkedId == R.id.companyRadio) {
                selectedUserType = "company";
                selectedPrice = 50.00;
            }
            updateUI();
        });

        subscribeButton.setOnClickListener(v -> {
            if (currentUser != null) {
                processSubscription();
            } else {
                Toast.makeText(this, "Please log in to subscribe", Toast.LENGTH_SHORT).show();
                // Redirect to login
                startActivity(new Intent(this, LoginActivity.class));
            }
        });
    }

    private void updateUI() {
        String priceText = String.format("$%.2f per month", selectedPrice);
        priceTextView.setText(priceText);

        String benefits = getBenefitsText(selectedUserType);
        benefitsTextView.setText(benefits);
    }

    private String getBenefitsText(String userType) {
        StringBuilder benefits = new StringBuilder();

        // Common benefits for all users
        benefits.append("🌟 Premium Benefits 🌟\n\n");

        benefits.append("✅ Advanced Disease Detection\n");
        benefits.append("   • AI-powered crop disease identification\n");
        benefits.append("   • Early warning system for plant diseases\n");
        benefits.append("   • Detailed treatment recommendations\n\n");

        benefits.append("✅ Smart Agricultural Monitoring\n");
        benefits.append("   • Real-time crop health monitoring\n");
        benefits.append("   • Soil quality analysis and recommendations\n");
        benefits.append("   • Weather-based farming advisories\n\n");

        benefits.append("✅ Expert Support\n");
        benefits.append("   • 24/7 access to agricultural experts\n");
        benefits.append("   • Personalized farming advice\n");
        benefits.append("   • Priority customer support\n\n");

        benefits.append("✅ Exclusive Content\n");
        benefits.append("   • Advanced farming techniques\n");
        benefits.append("   • Market trends and insights\n");
        benefits.append("   • Exclusive webinars and workshops\n\n");

        benefits.append("✅ Ad-Free Experience\n");
        benefits.append("   • Unlimited app usage without ads\n");
        benefits.append("   • Faster processing and responses\n\n");

        // User-specific benefits
        if ("company".equals(userType)) {
            benefits.append("🏢 Company Exclusive Features:\n");
            benefits.append("   • Multi-user account management\n");
            benefits.append("   • Bulk field analysis (up to 1000 acres)\n");
            benefits.append("   • Customized enterprise reports\n");
            benefits.append("   • API access for integration\n");
            benefits.append("   • Dedicated account manager\n");
            benefits.append("   • White-label reporting\n");
            benefits.append("   • Team collaboration tools\n");
            benefits.append("   • Advanced analytics dashboard\n");
        } else {
            benefits.append("👤 Individual Exclusive Features:\n");
            benefits.append("   • Personal farm analysis (up to 100 acres)\n");
            benefits.append("   • Family account sharing (up to 5 users)\n");
            benefits.append("   • Seasonal planting guides\n");
            benefits.append("   • Harvest optimization tools\n");
            benefits.append("   • Personal crop calendar\n");
            benefits.append("   • Local market price alerts\n");
        }

        return benefits.toString();
    }

    private void processSubscription() {
        // In a real app, this would integrate with a payment gateway
        // For now, we'll simulate successful payment

        showPaymentConfirmationDialog();
    }

    private void showPaymentConfirmationDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Confirm Subscription")
                .setMessage(String.format("You are about to subscribe to the %s plan for $%.2f per month. Proceed with payment?",
                        selectedUserType.equals("company") ? "Company" : "Individual", selectedPrice))
                .setPositiveButton("Proceed to Payment", (dialog, which) -> {
                    completeSubscription();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void completeSubscription() {
        String uid = currentUser.getUid();

        // Update user subscription in database
        userRef.child(uid).child("subscription").setValue("premium")
                .addOnSuccessListener(aVoid -> {
                    userRef.child(uid).child("subscriptionType").setValue(selectedUserType)
                            .addOnSuccessListener(aVoid1 -> {
                                userRef.child(uid).child("subscriptionPrice").setValue(selectedPrice)
                                        .addOnSuccessListener(aVoid2 -> {
                                            showSuccessMessage();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(this, "Error saving subscription details", Toast.LENGTH_SHORT).show();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error saving subscription type", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Subscription failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showSuccessMessage() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Subscription Successful! 🎉")
                .setMessage("Welcome to AgroShield Premium! Your " +
                        (selectedUserType.equals("company") ? "Company" : "Individual") +
                        " subscription is now active.\n\nYou now have access to all premium features.")
                .setPositiveButton("Explore Features", (dialog, which) -> {
                    // Navigate to main activity or premium features page
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.putExtra("showPremium", true);
                    startActivity(intent);
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up any resources if needed
    }
}