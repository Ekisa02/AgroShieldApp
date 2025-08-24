package com.Joseph.agroshieldapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AccountFragment extends Fragment {

    // Profile Views
    private MaterialCardView profileCard, accountSettingsCard, appSettingsCard;
    private ImageView editProfileBtn;
    private ShapeableImageView profileImage;
    private Chip premiumChip;
    private TextView userName, followersCount, followingCount, memberSinceYear ,followers,following;
    private LinearLayout followersSection, followingSection, memberSinceSection;

    // Account Settings Views
    private MaterialButton personalInfoBtn, notificationsBtn, paymentBtn, securityBtn, helpBtn, logoutBtn;

    // App Settings Views
    private LinearLayout darkModeSection;
    private MaterialButton languageBtn;
    private SwitchMaterial darkModeSwitch;

    // Glow Border Views
    private View glowBorderView, accountGlowBorderView, appGlowBorderView;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;
    private FirebaseUser currentUser;
    private FirebaseFirestore firestore;
    private DatabaseReference followersRef, followingRef;

    // Theme
    private boolean isDarkMode = false;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            // Initialize Firebase
            mAuth = FirebaseAuth.getInstance();
            userRef = FirebaseDatabase.getInstance().getReference("Users");
            currentUser = mAuth.getCurrentUser();
            // Initialize Firestore for social counts
            firestore = FirebaseFirestore.getInstance();

            // Initialize Realtime Database references
            if (currentUser != null) {
                String uid = currentUser.getUid();
                followersRef = FirebaseDatabase.getInstance().getReference("Followers").child(uid);
                followingRef = FirebaseDatabase.getInstance().getReference("Following").child(uid);
            }
        } catch (Exception e) {
            Log.e("AccountFragment", "Firebase initialization error: " + e.getMessage());
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_account_fragment, container, false);

        try {
            // Initialize all views
            initAllViews(view);

            // Set up visual effects
            setupVisualEffects();

            // Set up click listeners
            setupAllClickListeners();

            // Load user data
            loadUserData();

            // Load theme preference
            loadThemePreference();

        } catch (Exception e) {
            Log.e("AccountFragment", "Error in onCreateView: " + e.getMessage());
            showErrorLayout(view);
        }

        //profile image
        profileImage = view.findViewById(R.id.profileImage);

        fetchAndDisplayProfileImage();

        NavigateToFollwersPage();

        return view;
    }

    private void initAllViews(View view) {
        try {
            // Main cards
            profileCard = view.findViewById(R.id.profileCard);
            accountSettingsCard = view.findViewById(R.id.accountSettingsCard);
            appSettingsCard = view.findViewById(R.id.appSettingsCard);

            // Glow border views
            glowBorderView = view.findViewById(R.id.glowBorderView);
            accountGlowBorderView = view.findViewById(R.id.accountGlowBorderView);
            appGlowBorderView = view.findViewById(R.id.appGlowBorderView);

            // Profile section
            editProfileBtn = view.findViewById(R.id.editProfileBtn);
            //profileImage = view.findViewById(R.id.profileImage);
            userName = view.findViewById(R.id.userName);
            premiumChip = view.findViewById(R.id.premiumChip);
            followers = view.findViewById(R.id.followers);
            following = view.findViewById(R.id.Following);
            followersCount = view.findViewById(R.id.followersCount);
            followingCount = view.findViewById(R.id.followingCount);
            memberSinceYear = view.findViewById(R.id.memberSinceYear);

            // Stats sections
            followersSection = view.findViewById(R.id.followersSection);
            followingSection = view.findViewById(R.id.followingSection);
            memberSinceSection = view.findViewById(R.id.memberSinceSection);

            // Account settings buttons
            personalInfoBtn = view.findViewById(R.id.personalInfoBtn);
            notificationsBtn = view.findViewById(R.id.notificationsBtn);
            paymentBtn = view.findViewById(R.id.paymentBtn);
            securityBtn = view.findViewById(R.id.securityBtn);
            helpBtn = view.findViewById(R.id.helpBtn);
            logoutBtn = view.findViewById(R.id.logoutBtn);

            // App settings
            darkModeSection = view.findViewById(R.id.darkModeSection);
            darkModeSwitch = view.findViewById(R.id.darkModeSwitch);
            languageBtn = view.findViewById(R.id.languageBtn);

        } catch (Exception e) {
            Log.e("AccountFragment", "Error initializing views: " + e.getMessage());
            throw e;
        }
    }


    //fetching profile image
    private void fetchAndDisplayProfileImage() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "Not signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference docRef = FirebaseFirestore.getInstance()
                .collection("profileimages")
                .document(user.getUid());

        docRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String base64 = documentSnapshot.getString("imageBase64");
                        if (base64 != null && !base64.isEmpty()) {
                            Bitmap bmp = decodeBase64ToBitmap(base64);
                            if (bmp != null) {
                                profileImage.setImageBitmap(bmp);
                            } else {
                                profileImage.setImageResource(R.drawable.ic_profile_placeholder);
                            }
                        } else {
                            profileImage.setImageResource(R.drawable.ic_profile_placeholder);
                        }
                    } else {
                        profileImage.setImageResource(R.drawable.ic_profile_placeholder);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
                    profileImage.setImageResource(R.drawable.ic_profile_placeholder);
                });
    }
    //profile helper reference
    private String getCachedProfileImage() {
        return requireContext()
                .getSharedPreferences("profile_cache", Context.MODE_PRIVATE)
                .getString("cached_profile_image", null);
    }


    private Bitmap decodeBase64ToBitmap(String base64) {
        try {
            byte[] decoded = Base64.decode(base64, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
        } catch (Exception e) {
            return null;
        }
    }

    private void setupVisualEffects() {
        try {
            // Set alpha programmatically for glow effects
            setViewAlpha(glowBorderView, 0.7f);
            setViewAlpha(accountGlowBorderView, 0.6f);
            setViewAlpha(appGlowBorderView, 0.6f);

            // Add subtle animations to cards
            enhanceCardVisuals();

        } catch (Exception e) {
            Log.e("AccountFragment", "Error setting up visual effects: " + e.getMessage());
        }
    }

    private void setViewAlpha(View view, float alpha) {
        if (view != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    view.setAlpha(alpha);
                } else {
                    AlphaAnimation alphaAnimation = new AlphaAnimation(alpha, alpha);
                    alphaAnimation.setDuration(0);
                    alphaAnimation.setFillAfter(true);
                    view.startAnimation(alphaAnimation);
                }
            } catch (Exception e) {
                Log.e("AccountFragment", "Error setting view alpha: " + e.getMessage());
            }
        }
    }

    private void enhanceCardVisuals() {
        try {
            // Add subtle animations to cards
            if (profileCard != null) {
                profileCard.animate()
                        .setDuration(500)
                        .scaleX(1.02f)
                        .scaleY(1.02f)
                        .setInterpolator(new OvershootInterpolator())
                        .start();
            }

        } catch (Exception e) {
            Log.e("AccountFragment", "Error enhancing card visuals: " + e.getMessage());
        }
    }

    private void setupAllClickListeners() {
        try {
            // Header section listeners
            setupHeaderClickListeners();

            // Account settings listeners
            setupAccountSettingsListeners();

            // App settings listeners
            setupAppSettingsListeners();

        } catch (Exception e) {
            Log.e("AccountFragment", "Error setting up click listeners: " + e.getMessage());
        }
    }

    private void setupHeaderClickListeners() {
        try {
            // Edit profile button
            if (editProfileBtn != null) {
                editProfileBtn.setOnClickListener(v -> {
                    applyPressAnimation(v);
                    startActivity(new Intent(getActivity(), ProfileActivity.class));
                    Toast.makeText(getContext(), "Navigating to profile page....", Toast.LENGTH_SHORT).show();
                });
            }

            // Premium chip - handle upgrade to premium
            if (premiumChip != null) {
                premiumChip.setOnClickListener(v -> {
                    // Check if user is already premium
                    checkPremiumStatusAndHandleClick();
                });
            }

            // Stats sections
            setSectionClickListener(followersSection, "Show Followers");

            setSectionClickListener(followingSection, "Show Following");
            setSectionClickListener(memberSinceSection, "Member Since Details");

        } catch (Exception e) {
            Log.e("AccountFragment", "Error setting up header click listeners: " + e.getMessage());
        }
    }

    // checking membership status
    private void checkPremiumStatusAndHandleClick() {
        if (currentUser == null) return;

        String uid = currentUser.getUid();
        userRef.child(uid).child("subscription").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String subscription = snapshot.getValue(String.class);
                    if ("premium".equals(subscription)) {
                        // User is already premium, show cancellation dialog
                        showPremiumCancellationDialog();
                    } else {
                        // User is basic, navigate to subscription page
                        navigateToSubscriptionPage();
                    }
                } else {
                    // Subscription field doesn't exist, navigate to subscription page
                    navigateToSubscriptionPage();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Error checking subscription status", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToSubscriptionPage() {
        startActivity(new Intent(getActivity(), SubscriptionActivity.class));
    }

    private void showPremiumCancellationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Premium Membership")
                .setMessage("You are already a Premium member. Would you like to cancel your membership?")
                .setPositiveButton("Cancel Membership", (dialog, which) -> {
                    // Handle cancellation logic
                    cancelPremiumMembership();
                })
                .setNegativeButton("Keep Premium", (dialog, which) -> {
                    // User wants to keep premium
                    Toast.makeText(getContext(), "Enjoy your Premium benefits!", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("Manage Subscription", (dialog, which) -> {
                    // Open subscription management
                    managePremiumSubscription();
                })
                .show();
    }

    private void showPremiumUpgradeDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Upgrade to Premium")
                .setMessage("Enjoy exclusive features with our Premium membership!\n\n• Ad-free experience\n• Premium content access\n• Priority support\n• Exclusive features")
                .setPositiveButton("Upgrade Now", (dialog, which) -> {
                    // Handle upgrade logic here
                    upgradeToPremium();
                })
                .setNegativeButton("Later", null)
                .show();
    }

    private void upgradeToPremium() {
        if (currentUser == null) return;

        String uid = currentUser.getUid();
        userRef.child(uid).child("subscription").setValue("premium")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Successfully upgraded to Premium! 🎉", Toast.LENGTH_SHORT).show();
                    updatePremiumChip(true);
                    showPremiumWelcomeMessage();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Upgrade failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void cancelPremiumMembership() {
        if (currentUser == null) return;

        // Show confirmation dialog for cancellation
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirm Cancellation")
                .setMessage("Are you sure you want to cancel your Premium membership? You'll lose access to all Premium features immediately.")
                .setPositiveButton("Yes, Cancel", (dialog, which) -> {
                    proceedWithCancellation();
                })
                .setNegativeButton("Keep Membership", null)
                .show();
    }

    private void proceedWithCancellation() {
        if (currentUser == null) return;

        String uid = currentUser.getUid();
        userRef.child(uid).child("subscription").setValue("basic")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Premium membership cancelled", Toast.LENGTH_SHORT).show();
                    updatePremiumChip(false);
                    resetToDefaultFeatures();
                    showCancellationConfirmation();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Cancellation failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updatePremiumChip(boolean isPremium) {
        if (premiumChip != null) {
            if (isPremium) {
                premiumChip.setText("Premium Member");
                premiumChip.setChipBackgroundColorResource(R.color.premium_gold);
                premiumChip.setChipIconResource(R.drawable.ic_premium);
                // You can add more premium styling here
            } else {
                premiumChip.setText("Upgrade to Premium");
                premiumChip.setChipBackgroundColorResource(R.color.colorPrimary);
                premiumChip.setChipIconResource(R.drawable.ic_upgrade);
                // Reset to basic styling
            }
        }
    }

    private void resetToDefaultFeatures() {
        // Reset any premium features to their default state
        // This could include:
        // - Hiding premium-only content
        // - Resetting UI elements to basic version
        // - Disabling premium features

        // Example: Reset any premium indicators in the app
        if (getActivity() != null) {
            // Notify other parts of the app about subscription change
            Intent intent = new Intent("SUBSCRIPTION_CHANGED");
            intent.putExtra("isPremium", false);
            requireContext().sendBroadcast(intent);
        }

        // You can add more specific reset logic here based on your app's features
        Toast.makeText(getContext(), "Premium features disabled", Toast.LENGTH_SHORT).show();
    }

    private void showPremiumWelcomeMessage() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Welcome to Premium! 🎉")
                .setMessage("Thank you for upgrading to Premium! Here's what you get:\n\n" +
                        "• Ad-free experience\n" +
                        "• Exclusive content access\n" +
                        "• Priority customer support\n" +
                        "• Advanced features unlocked\n\n" +
                        "Enjoy your enhanced experience!")
                .setPositiveButton("Got it!", null)
                .show();
    }

    private void showCancellationConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Membership Cancelled")
                .setMessage("Your Premium membership has been cancelled. You can upgrade again anytime to regain access to Premium features.")
                .setPositiveButton("OK", null)
                .setNeutralButton("Upgrade Again", (dialog, which) -> {
                    showPremiumUpgradeDialog();
                })
                .show();
    }

    private void managePremiumSubscription() {
        // Open subscription management screen or activity
        Toast.makeText(getContext(), "Opening subscription management...", Toast.LENGTH_SHORT).show();

        // You can implement this to open a subscription management activity
        // Intent intent = new Intent(getActivity(), SubscriptionManagementActivity.class);
        // startActivity(intent);
    }
    private void setupAccountSettingsListeners() {
        try {
            setButtonClickListener(personalInfoBtn, "Personal Information");
            setButtonClickListener(notificationsBtn, "Notifications");
            setButtonClickListener(paymentBtn, "Payment Methods");
            setButtonClickListener(securityBtn, "Security");
            setButtonClickListener(helpBtn, "Help Center");

            // Logout button
            if (logoutBtn != null) {
                logoutBtn.setOnClickListener(v -> logout());
            }

        } catch (Exception e) {
            Log.e("AccountFragment", "Error setting up account settings listeners: " + e.getMessage());
        }
    }

    private void setupAppSettingsListeners() {
        try {
            // Dark mode section
            if (darkModeSection != null) {
                darkModeSection.setOnClickListener(v -> {
                    if (darkModeSwitch != null) {
                        darkModeSwitch.setChecked(!darkModeSwitch.isChecked());
                    }
                });
            }

            // Dark mode switch
            if (darkModeSwitch != null) {
                darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    toggleDarkMode(isChecked);
                });
            }

            // Language button
            setButtonClickListener(languageBtn, "Change Language");

        } catch (Exception e) {
            Log.e("AccountFragment", "Error setting up app settings listeners: " + e.getMessage());
        }
    }

    private void setSectionClickListener(View view, final String message) {
        if (view != null) {
            try {
                view.setOnClickListener(v -> {
                    applyPressAnimation(v);
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                });

                setPressEffect(view);
            } catch (Exception e) {
                Log.e("AccountFragment", "Error setting section click listener: " + e.getMessage());
            }
        }
    }

    private void setButtonClickListener(MaterialButton button, final String message) {
        if (button != null) {
            try {
                button.setOnClickListener(v -> {
                    applyPressAnimation(v);
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                });

                setPressEffect(button);
            } catch (Exception e) {
                Log.e("AccountFragment", "Error setting button click listener: " + e.getMessage());
            }
        }
    }

    private void setPressEffect(View view) {
        if (view != null) {
            try {
                view.setOnTouchListener((v, event) -> {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
                            break;
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                            break;
                    }
                    return false;
                });
            } catch (Exception e) {
                Log.e("AccountFragment", "Error setting press effect: " + e.getMessage());
            }
        }
    }

    private void applyPressAnimation(View view) {
        try {
            view.animate()
                    .setDuration(150)
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .withEndAction(() -> view.animate()
                            .setDuration(150)
                            .scaleX(1f)
                            .scaleY(1f)
                            .start())
                    .start();
        } catch (Exception e) {
            Log.e("AccountFragment", "Error applying press animation: " + e.getMessage());
        }
    }


  // user name
    private void loadUserData() {
        if (currentUser == null) {
            Toast.makeText(getContext(), "Please log in to view profile", Toast.LENGTH_SHORT).show();
            navigateToLogin();
            return;
        }

        try {
            String uid = currentUser.getUid();
            userRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        // Load user name
                        String name = snapshot.child("name").getValue(String.class);
                        if (userName != null) {
                            userName.setText(name != null ? name : "Name not found");
                        }

                        // Load subscription status
                        String subscription = snapshot.child("subscription").getValue(String.class);
                        boolean isPremium = "premium".equals(subscription);
                        updatePremiumChip(isPremium);

                        // Load other user data
                        loadAdditionalUserData(snapshot);
                    } else {
                        // User document doesn't exist, create it
                        createUserDocument();
                        loadSampleData(); // Show sample data until user document is created
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(getContext(), "Error loading user data", Toast.LENGTH_SHORT).show();
                    loadSampleData(); // Fallback to sample data
                }
            });
        } catch (Exception e) {
            Log.e("AccountFragment", "Error loading user data: " + e.getMessage());
            loadSampleData(); // Fallback to sample data
        }
    }

    private void loadAdditionalUserData(DataSnapshot snapshot) {
        try {
            // Load user name if not already loaded
            String name = snapshot.child("name").getValue(String.class);
            if (userName != null && name != null) {
                userName.setText(name);
            }

            // Load REAL followers count from Firestore
            loadRealFollowersCount();

            // Load REAL following count from Firestore
            loadRealFollowingCount();

            // Load REAL member since date from multiple possible sources
            loadRealMemberSinceDate(snapshot);

        } catch (Exception e) {
            Log.e("AccountFragment", "Error loading additional user data: " + e.getMessage());
            // Fallback to sample data if real data fails
            loadSampleData();
        }
    }
    private void loadRealFollowersCount() {
        if (currentUser == null) return;

        String uid = currentUser.getUid();

        // Try Firestore first (where we store followers)
        firestore.collection("followers").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> followerIds = (List<String>) documentSnapshot.get("followerIds");
                        int followers = followerIds != null ? followerIds.size() : 0;
                        if (followersCount != null) {
                            followersCount.setText(formatCount(followers));
                        }
                    } else {
                        // Fallback to Realtime Database
                        loadFollowersFromRealtimeDB();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("AccountFragment", "Error loading followers from Firestore: " + e.getMessage());
                    loadFollowersFromRealtimeDB();
                });
    }

    private void loadFollowersFromRealtimeDB() {
        if (followersRef != null) {
            followersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    long followers = snapshot.getChildrenCount();
                    if (followersCount != null) {
                        followersCount.setText(formatCount(followers));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("AccountFragment", "Error loading followers from Realtime DB: " + error.getMessage());
                    if (followersCount != null) {
                        followersCount.setText("0");
                    }
                }
            });
        }
    }

    private void loadRealFollowingCount() {
        if (currentUser == null) return;

        String uid = currentUser.getUid();

        // Try Firestore first (where we store following)
        firestore.collection("following").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> followingIds = (List<String>) documentSnapshot.get("followingIds");
                        int following = followingIds != null ? followingIds.size() : 0;
                        if (followingCount != null) {
                            followingCount.setText(formatCount(following));
                        }
                    } else {
                        // Fallback to Realtime Database
                        loadFollowingFromRealtimeDB();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("AccountFragment", "Error loading following from Firestore: " + e.getMessage());
                    loadFollowingFromRealtimeDB();
                });
    }

    private void loadFollowingFromRealtimeDB() {
        if (followingRef != null) {
            followingRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    long following = snapshot.getChildrenCount();
                    if (followingCount != null) {
                        followingCount.setText(formatCount(following));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("AccountFragment", "Error loading following from Realtime DB: " + error.getMessage());
                    if (followingCount != null) {
                        followingCount.setText("0");
                    }
                }
            });
        }
    }

    private void loadRealMemberSinceDate(DataSnapshot snapshot) {
        if (memberSinceYear == null) return;

        // Try multiple possible field names for creation date
        Long createdAt = null;

        // Check Realtime Database fields
        if (snapshot.child("createdAt").exists()) {
            createdAt = snapshot.child("createdAt").getValue(Long.class);
        } else if (snapshot.child("timestamp").exists()) {
            createdAt = snapshot.child("timestamp").getValue(Long.class);
        } else if (snapshot.child("joinDate").exists()) {
            createdAt = snapshot.child("joinDate").getValue(Long.class);
        } else if (snapshot.child("memberSince").exists()) {
            createdAt = snapshot.child("memberSince").getValue(Long.class);
        }

        if (createdAt != null) {
            // Format the date properly
            String formattedDate = formatMemberSinceDate(createdAt);
            memberSinceYear.setText(formattedDate);
        } else {
            // If not found in Realtime DB, try Firestore
            loadMemberSinceFromFirestore();
        }
    }

    private void loadMemberSinceFromFirestore() {
        if (currentUser == null || memberSinceYear == null) return;

        String uid = currentUser.getUid();

        firestore.collection("Users").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Check multiple possible field names in Firestore
                        Long createdAt = documentSnapshot.getLong("createdAt");
                        if (createdAt == null) createdAt = documentSnapshot.getLong("timestamp");
                        if (createdAt == null) createdAt = documentSnapshot.getLong("joinDate");
                        if (createdAt == null) createdAt = documentSnapshot.getLong("memberSince");
                        if (createdAt == null) createdAt = documentSnapshot.getLong("creationTime");

                        if (createdAt != null) {
                            String formattedDate = formatMemberSinceDate(createdAt);
                            memberSinceYear.setText(formattedDate);
                        } else {
                            // Use account creation time as fallback
                            setAccountCreationFallback();
                        }
                    } else {
                        setAccountCreationFallback();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("AccountFragment", "Error loading member since from Firestore: " + e.getMessage());
                    setAccountCreationFallback();
                });
    }

    private String formatMemberSinceDate(long timestamp) {
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(timestamp);

            SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
            return "Member since " + dateFormat.format(calendar.getTime());
        } catch (Exception e) {
            Log.e("AccountFragment", "Error formatting date: " + e.getMessage());
            return "Member since 2025";
        }
    }

    private void setAccountCreationFallback() {
        if (memberSinceYear != null) {
            if (currentUser != null) {
                // Use Firebase Auth account creation time if available
                long creationTime = currentUser.getMetadata().getCreationTimestamp();
                if (creationTime > 0) {
                    String formattedDate = formatMemberSinceDate(creationTime);
                    memberSinceYear.setText(formattedDate);
                } else {
                    // Final fallback - current year
                    Calendar calendar = Calendar.getInstance();
                    int currentYear = calendar.get(Calendar.YEAR);
                    memberSinceYear.setText("Member since " + currentYear);
                }
            } else {
                // Final fallback - current year
                Calendar calendar = Calendar.getInstance();
                int currentYear = calendar.get(Calendar.YEAR);
                memberSinceYear.setText("Member since " + currentYear);
            }
        }
    }



    //folowers  && following button:
  private void  NavigateToFollwersPage(){
      //followers.setOnClickListener(view -> startActivity(new Intent(getActivity(), FollowersActivity.class)));
      following.setOnClickListener(view -> startActivity(new Intent(getActivity(), FollowingActivity.class)));

  }
    private void loadSampleData() {
        try {
            // Set sample data as fallback
            if (userName != null) {
                if (currentUser != null && currentUser.getDisplayName() != null) {
                    userName.setText(currentUser.getDisplayName());
                } else {
                    userName.setText("User");
                }
            }

            // Show loading state instead of zeros
            if (followersCount != null) followersCount.setText("...");
            if (followingCount != null) followingCount.setText("...");

            // Set realistic member since year
            if (memberSinceYear != null) {
                Calendar calendar = Calendar.getInstance();
                int currentYear = calendar.get(Calendar.YEAR);
                memberSinceYear.setText("Member since " + currentYear);
            }

            updatePremiumChip(false); // Assume basic subscription initially

        } catch (Exception e) {
            Log.e("AccountFragment", "Error loading sample data: " + e.getMessage());
        }
    }

    private void navigateToLogin() {
        try {
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().finish();
            }
        } catch (Exception e) {
            Log.e("AccountFragment", "Error navigating to login: " + e.getMessage());
        }
    }

    private String formatCount(long count) {
        try {
            if (count < 0) {
                return "0"; // Handle negative numbers
            } else if (count < 1000) {
                return String.valueOf(count);
            } else if (count < 1000000) {
                // Format with locale-aware formatting
                return String.format(Locale.getDefault(), "%.1fK", count / 1000.0);
            } else {
                return String.format(Locale.getDefault(), "%.1fM", count / 1000000.0);
            }
        } catch (Exception e) {
            Log.e("AccountFragment", "Error formatting count: " + e.getMessage());
            return "0";
        }
    }
    private void createUserDocument() {
        if (currentUser == null) return;

        try {
            Map<String, Object> user = new HashMap<>();
            user.put("name", currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "User");
            user.put("email", currentUser.getEmail());
            user.put("followers", 0);
            user.put("following", 0);
            user.put("memberSince", System.currentTimeMillis());
            user.put("subscription", "basic"); // Default to basic subscription
            user.put("profileImage", currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : "");

            String uid = currentUser.getUid();
            userRef.child(uid).setValue(user)
                    .addOnSuccessListener(aVoid -> {
                        // Reload user data
                        loadUserData();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to create user profile", Toast.LENGTH_SHORT).show();
                    });
        } catch (Exception e) {
            Log.e("AccountFragment", "Error creating user document: " + e.getMessage());
        }
    }

    private void loadThemePreference() {
        try {
            if (getContext() != null && darkModeSwitch != null) {
                SharedPreferences preferences = getContext().getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
                isDarkMode = preferences.getBoolean("dark_mode", false);
                darkModeSwitch.setChecked(isDarkMode);
            }
        } catch (Exception e) {
            Log.e("AccountFragment", "Error loading theme preference: " + e.getMessage());
        }
    }

    private void logout() {
        try {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        // Sign out from Firebase
                        mAuth.signOut();

                        // Navigate to login screen
                        Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
                        navigateToLogin();
                    })
                    .setNegativeButton("No", null)
                    .show();
        } catch (Exception e) {
            Log.e("AccountFragment", "Error during logout: " + e.getMessage());
        }
    }

    private void toggleDarkMode(boolean isDarkMode) {
        try {
            this.isDarkMode = isDarkMode;

            // Implement dark mode toggle functionality
            AppCompatDelegate.setDefaultNightMode(
                    isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );

            // Save preference
            SharedPreferences preferences = requireContext().getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
            preferences.edit().putBoolean("dark_mode", isDarkMode).apply();

            Toast.makeText(getContext(),
                    isDarkMode ? "Dark Mode Enabled" : "Dark Mode Disabled",
                    Toast.LENGTH_SHORT).show();

            // Update visual effects for new theme
            updateVisualEffectsForTheme(isDarkMode);
        } catch (Exception e) {
            Log.e("AccountFragment", "Error toggling dark mode: " + e.getMessage());
        }
    }

    private void updateVisualEffectsForTheme(boolean isDarkMode) {
        try {
            // Adjust glow intensity based on theme
            float glowAlpha = isDarkMode ? 0.8f : 0.6f;

            setViewAlpha(glowBorderView, glowAlpha);
            setViewAlpha(accountGlowBorderView, glowAlpha - 0.1f);
            setViewAlpha(appGlowBorderView, glowAlpha - 0.1f);
        } catch (Exception e) {
            Log.e("AccountFragment", "Error updating visual effects for theme: " + e.getMessage());
        }
    }

    private void showErrorLayout(View view) {
        try {
            // Show a simple error message if the complex layout fails
            TextView errorText = new TextView(getContext());
            errorText.setText("Error loading profile. Please try again.");
            errorText.setTextSize(16);
            errorText.setPadding(32, 32, 32, 32);
            errorText.setGravity(android.view.Gravity.CENTER);

            // Remove all views and add error message
            ViewGroup rootView = (ViewGroup) view;
            rootView.removeAllViews();
            rootView.addView(errorText);

            Toast.makeText(getContext(), "Layout loading failed", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e("AccountFragment", "Error showing error layout: " + e.getMessage());
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // 1. Try cache first
        String cachedBase64 = getCachedProfileImage();
        if (cachedBase64 != null && !cachedBase64.isEmpty()) {
            Bitmap bmp = decodeBase64ToBitmap(cachedBase64);
            if (bmp != null) {
                profileImage.setImageBitmap(bmp);
            }
        }

        // 2. Then fetch latest from Firestore to refresh if changed
        fetchAndDisplayProfileImage();

        try {
            // Refresh user data when fragment resumes
            loadUserData();

            // Refresh theme preference
            loadThemePreference();
        } catch (Exception e) {
            Log.e("AccountFragment", "Error in onResume: " + e.getMessage());
        }
    }
}