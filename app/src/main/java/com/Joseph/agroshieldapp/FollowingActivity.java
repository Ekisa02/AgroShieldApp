package com.Joseph.agroshieldapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.Joseph.agroshieldapp.Social.CacheManager;
import com.Joseph.agroshieldapp.Social.User;
import com.Joseph.agroshieldapp.Social.UserAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class FollowingActivity extends AppCompatActivity {

    private static final String TAG = "FollowingActivity";
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;

    // UI Components
    private RecyclerView recyclerFollowing, recyclerSuggestions;
    private ProgressBar progressBar;
    private TextView errorTextView;

    // Adapters and Data
    private UserAdapter followingAdapter, suggestionAdapter;
    private List<User> followingList = new ArrayList<>();
    private List<User> suggestionList = new ArrayList<>();

    // Firebase
    private FirebaseFirestore db;
    private DatabaseReference userRef;
    private String currentUserId;

    // Listeners
    private ListenerRegistration followingListener;
    private ValueEventListener usersValueEventListener;
    private List<String> currentFollowingIds = new ArrayList<>();

    // Cache
    private CacheManager cacheManager;
    private boolean isLoadingSuggestions = false;

    // Permission tracking
    private boolean isNotificationPromptShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_following);

        cacheManager = new CacheManager(this);
        initializeViews();
        setupRecyclerViews();
        initializeFirebase();
        setupRealTimeListeners();
        loadData();
        cacheManager.clearOldCache();

        // Show notification permission prompt after a delay (better UX)
        showDelayedPermissionPrompt();
    }

    private void initializeViews() {
        recyclerFollowing = findViewById(R.id.recyclerFollowing);
        recyclerSuggestions = findViewById(R.id.recyclerSuggestions);
        progressBar = findViewById(R.id.progressBar);
        errorTextView = findViewById(R.id.errorTextView);

        // Set up retry button from the new layout
        findViewById(R.id.retryButton).setOnClickListener(v -> onRetryClicked());
    }

    private void setupRecyclerViews() {
        recyclerFollowing.setLayoutManager(new LinearLayoutManager(this));
        recyclerSuggestions.setLayoutManager(new LinearLayoutManager(this));

        followingAdapter = new UserAdapter(this, followingList);
        suggestionAdapter = new UserAdapter(this, suggestionList);

        recyclerFollowing.setAdapter(followingAdapter);
        recyclerSuggestions.setAdapter(suggestionAdapter);
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        userRef = FirebaseDatabase.getInstance().getReference("Users");
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    private void setupRealTimeListeners() {
        // Real-time listener for following changes
        followingListener = db.collection("following").document(currentUserId)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Following listener error: " + error.getMessage());
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        List<String> newFollowingIds = (List<String>) documentSnapshot.get("followingIds");
                        if (newFollowingIds != null) {
                            currentFollowingIds = newFollowingIds;
                            refreshSuggestions();
                            updateUserCounts(); // Update counts when following changes
                        }
                    } else {
                        currentFollowingIds.clear();
                        refreshSuggestions();
                        updateUserCounts(); // Update counts when following changes
                    }
                });
    }

    private void loadData() {
        loadFollowing();
        loadSuggestions();
    }

    private void loadFollowing() {
        showLoading(true);
        hideError();

        db.collection("following").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> followingIds = (List<String>) documentSnapshot.get("followingIds");
                        if (followingIds != null && !followingIds.isEmpty()) {
                            currentFollowingIds = followingIds;
                            for (String followedUid : followingIds) {
                                fetchUserWithCache(followedUid, "following");
                            }
                        } else {
                            showEmptyState("You're not following anyone yet");
                        }
                    } else {
                        showEmptyState("You're not following anyone yet");
                    }
                    showLoading(false);
                    updateUserCounts(); // Update counts after loading
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading following: " + e.getMessage());
                    showError("Failed to load following list");
                    showLoading(false);
                });
    }

    private void loadSuggestions() {
        if (isLoadingSuggestions) return;

        isLoadingSuggestions = true;
        showLoading(true);
        hideError();

        if (usersValueEventListener != null) {
            userRef.removeEventListener(usersValueEventListener);
        }

        usersValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                suggestionList.clear();

                List<User> newSuggestions = new ArrayList<>();
                int processedUsers = 0;

                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    String uid = userSnap.getKey();
                    if (uid != null && !uid.equals(currentUserId) &&
                            !currentFollowingIds.contains(uid)) {

                        String username = userSnap.child("name").getValue(String.class);
                        if (username == null) username = "Unknown User";

                        processedUsers++;
                        checkFollowBackStatus(uid, username, newSuggestions);
                    }
                }

                if (processedUsers == 0) {
                    showEmptyState("No suggestions available");
                }

                isLoadingSuggestions = false;
                showLoading(false);
                updateUserCounts(); // Update counts after loading
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading users: " + error.getMessage());
                showError("Failed to load suggestions");
                isLoadingSuggestions = false;
                showLoading(false);
            }
        };

        userRef.addListenerForSingleValueEvent(usersValueEventListener);
    }

    private void checkFollowBackStatus(String uid, String username, List<User> newSuggestions) {
        db.collection("followers").document(currentUserId).get()
                .addOnSuccessListener(followerDoc -> {
                    String followStatus = "follow";
                    if (followerDoc.exists()) {
                        List<String> followerIds = (List<String>) followerDoc.get("followerIds");
                        if (followerIds != null && followerIds.contains(uid)) {
                            followStatus = "follow_back";
                        }
                    }
                    fetchUserForSuggestion(uid, username, followStatus, newSuggestions);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking follow back: " + e.getMessage());
                    fetchUserForSuggestion(uid, username, "follow", newSuggestions);
                });
    }

    private void fetchUserForSuggestion(String uid, String username, String followStatus, List<User> newSuggestions) {
        db.collection("profileimages").document(uid).get()
                .addOnSuccessListener(doc -> {
                    Bitmap profileBitmap = null;
                    String base64Image = null;
                    if (doc.exists()) {
                        String base64 = doc.getString("imageBase64");
                        if (base64 != null && !base64.isEmpty()) {
                            profileBitmap = decodeBase64ToBitmap(base64);
                            base64Image = base64;
                        }
                    }

                    User user = new User(uid, username, profileBitmap, followStatus);
                    suggestionList.add(user);
                    suggestionAdapter.notifyDataSetChanged();

                    if (base64Image != null) {
                        cacheManager.cacheUser(uid, username, base64Image);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching profile: " + e.getMessage());
                    User user = new User(uid, username, null, followStatus);
                    suggestionList.add(user);
                    suggestionAdapter.notifyDataSetChanged();
                });
    }

    private void fetchUserWithCache(String uid, String followStatus) {
        com.Joseph.agroshieldapp.Social.CachedUser cachedUser = cacheManager.getCachedUser(uid);
        if (cachedUser != null) {
            Bitmap profileBitmap = null;
            if (cachedUser.profileImageBase64 != null && !cachedUser.profileImageBase64.isEmpty()) {
                profileBitmap = decodeBase64ToBitmap(cachedUser.profileImageBase64);
            }
            User user = new User(uid, cachedUser.username, profileBitmap, followStatus);
            followingList.add(user);
            followingAdapter.notifyDataSetChanged();
            updateUserCounts(); // Update counts when user is added
        } else {
            fetchUserDetails(uid, followStatus);
        }
    }

    private void fetchUserDetails(String uid, String followStatus) {
        userRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot userSnap) {
                if (userSnap.exists()) {
                    String username = userSnap.child("name").getValue(String.class);
                    if (username == null) username = "Unknown User";
                    fetchProfileImage(uid, username, followStatus);
                } else {
                    Log.w(TAG, "User data not found for UID: " + uid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching user details: " + error.getMessage());
            }
        });
    }

    private void fetchProfileImage(String uid, String username, String followStatus) {
        db.collection("profileimages").document(uid).get()
                .addOnSuccessListener(doc -> {
                    Bitmap profileBitmap = null;
                    String base64Image = null;
                    if (doc.exists()) {
                        String base64 = doc.getString("imageBase64");
                        if (base64 != null && !base64.isEmpty()) {
                            profileBitmap = decodeBase64ToBitmap(base64);
                            base64Image = base64;
                        }
                    }
                    User user = new User(uid, username, profileBitmap, followStatus);
                    followingList.add(user);
                    followingAdapter.notifyDataSetChanged();
                    updateUserCounts(); // Update counts when user is added

                    if (base64Image != null) {
                        cacheManager.cacheUser(uid, username, base64Image);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching profile image: " + e.getMessage());
                    User user = new User(uid, username, null, followStatus);
                    followingList.add(user);
                    followingAdapter.notifyDataSetChanged();
                    updateUserCounts(); // Update counts when user is added
                });
    }

    private Bitmap decodeBase64ToBitmap(String base64Str) {
        try {
            byte[] decodedBytes = Base64.decode(base64Str, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            Log.e(TAG, "Error decoding base64: " + e.getMessage());
            return null;
        }
    }

    // ==================== NOTIFICATION PERMISSION HANDLING ====================

    /**
     * Show permission prompt after a delay to avoid blocking initial UI load
     */
    private void showDelayedPermissionPrompt() {
        new android.os.Handler().postDelayed(() -> {
            if (!isNotificationPromptShown) {
                checkNotificationPermission();
                isNotificationPromptShown = true;
            }
        }, 2000); // 2-second delay
    }

    /**
     * Check if we need to ask for notification permission (Android 13+ only)
     */
    private void checkNotificationPermission() {
        // Only ask on Android 13+ (Tiramisu)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                // Check if we've already explained why we need permission
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.POST_NOTIFICATIONS)) {

                    showPermissionRationaleDialog();
                } else {
                    // First time asking - show friendly explanation
                    showFirstTimePermissionDialog();
                }
            }
        }
    }

    /**
     * Show dialog explaining why we need notifications (for users who denied once)
     */
    private void showPermissionRationaleDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Enable Notifications")
                .setMessage("Get instant updates when someone follows you back or interacts with your content. You can always enable this later in settings.")
                .setPositiveButton("Enable Now", (dialog, which) ->
                        requestNotificationPermission())
                .setNegativeButton("Not Now", (dialog, which) -> {
                    // User chose to skip - don't block the app
                    Toast.makeText(this, "You can enable notifications in settings anytime", Toast.LENGTH_SHORT).show();
                })
                .setCancelable(false)
                .show();
    }

    /**
     * Show friendly first-time permission request
     */
    private void showFirstTimePermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Stay Updated")
                .setMessage("Would you like to receive notifications for new followers and interactions?")
                .setPositiveButton("Yes, Enable", (dialog, which) ->
                        requestNotificationPermission())
                .setNegativeButton("Maybe Later", (dialog, which) -> {
                    // User chose to skip - don't block the app
                    Toast.makeText(this, "Notifications can be enabled anytime in settings", Toast.LENGTH_SHORT).show();
                })
                .setCancelable(false)
                .show();
    }

    /**
     * Actually request the permission from system
     */
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_REQUEST_CODE);
        }
    }

    /**
     * Handle permission request result
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notifications enabled! You'll get updates on new followers", Toast.LENGTH_SHORT).show();
            } else {
                // Permission denied - check if user selected "Don't ask again"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.POST_NOTIFICATIONS)) {
                        // User selected "Don't ask again"
                        showNeverAskAgainDialog();
                    } else {
                        // User simply denied
                        Toast.makeText(this, "Notifications disabled. You can enable them in app settings", Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
    }

    /**
     * Show dialog for users who selected "Don't ask again"
     */
    private void showNeverAskAgainDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Notification Settings")
                .setMessage("To enable notifications later, go to:\n\nApp Settings → Notifications")
                .setPositiveButton("Open Settings", (dialog, which) -> openAppSettings())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Open app settings so user can manually enable notifications
     */
    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    // ==================== UI UPDATE METHODS FOR NEW LAYOUT ====================

    /**
     * Show loading state using the new layout components
     */
    private void showLoading(boolean show) {
        runOnUiThread(() -> {
            findViewById(R.id.loadingLayout).setVisibility(show ? View.VISIBLE : View.GONE);
            findViewById(R.id.errorLayout).setVisibility(View.GONE);
            findViewById(R.id.emptyStateLayout).setVisibility(View.GONE);

            // Also hide the main content when loading
            findViewById(R.id.recyclerFollowing).setVisibility(show ? View.GONE : View.VISIBLE);
            findViewById(R.id.recyclerSuggestions).setVisibility(show ? View.GONE : View.VISIBLE);
        });
    }

    /**
     * Show error state using the new layout
     */
    private void showError(String message) {
        runOnUiThread(() -> {
            TextView errorTextView = findViewById(R.id.errorTextView);
            errorTextView.setText(message);

            findViewById(R.id.errorLayout).setVisibility(View.VISIBLE);
            findViewById(R.id.loadingLayout).setVisibility(View.GONE);
            findViewById(R.id.emptyStateLayout).setVisibility(View.GONE);

            // Hide main content on error
            findViewById(R.id.recyclerFollowing).setVisibility(View.GONE);
            findViewById(R.id.recyclerSuggestions).setVisibility(View.GONE);
        });
    }

    /**
     * Show empty state using the new layout
     */
    private void showEmptyState(String message) {
        runOnUiThread(() -> {
            // Try to find the empty state text view (you may need to add an ID to your layout)
            TextView emptyTextView = findViewById(R.id.emptyStateTextView);
            if (emptyTextView != null) {
                emptyTextView.setText(message);
            }

            findViewById(R.id.emptyStateLayout).setVisibility(View.VISIBLE);
            findViewById(R.id.loadingLayout).setVisibility(View.GONE);
            findViewById(R.id.errorLayout).setVisibility(View.GONE);
        });
    }

    /**
     * Hide error state
     */
    private void hideError() {
        runOnUiThread(() -> {
            findViewById(R.id.errorLayout).setVisibility(View.GONE);

            // Show main content when error is hidden
            findViewById(R.id.recyclerFollowing).setVisibility(View.VISIBLE);
            findViewById(R.id.recyclerSuggestions).setVisibility(View.VISIBLE);
        });
    }

    /**
     * Update user count badges in the new layout
     */
    private void updateUserCounts() {
        runOnUiThread(() -> {
            TextView tvFollowingCount = findViewById(R.id.tvFollowingCount);
            TextView tvSuggestionsCount = findViewById(R.id.tvSuggestionsCount);

            if (tvFollowingCount != null) {
                tvFollowingCount.setText(String.valueOf(followingList.size()));
            }
            if (tvSuggestionsCount != null) {
                tvSuggestionsCount.setText(String.valueOf(suggestionList.size()));
            }
        });
    }

    // ==================== PUBLIC METHODS FOR ADAPTER ====================

    public void removeUserFromSuggestions(String uid) {
        for (int i = 0; i < suggestionList.size(); i++) {
            if (suggestionList.get(i).getUid().equals(uid)) {
                suggestionList.remove(i);
                suggestionAdapter.notifyItemRemoved(i);
                break;
            }
        }

        if (suggestionList.isEmpty()) {
            showEmptyState("No suggestions available");
        }
        updateUserCounts(); // Update counts after removal
    }

    public void refreshSuggestions() {
        if (!isLoadingSuggestions) {
            loadSuggestions();
        }
    }

    public void onRetryClicked() {
        hideError();
        loadData();
    }

    // ==================== LIFECYCLE METHODS ====================

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Clean up listeners to prevent memory leaks
        if (followingListener != null) {
            followingListener.remove();
        }

        if (usersValueEventListener != null) {
            userRef.removeEventListener(usersValueEventListener);
        }

        // Clean up adapters
        if (followingAdapter != null) {
            followingAdapter.cleanup();
        }
        if (suggestionAdapter != null) {
            suggestionAdapter.cleanup();
        }

        // Clear data
        followingList.clear();
        suggestionList.clear();
    }

    // Note: Removed the automatic permission check from onStart()
    // Permission is now requested with a delay in onCreate()
}