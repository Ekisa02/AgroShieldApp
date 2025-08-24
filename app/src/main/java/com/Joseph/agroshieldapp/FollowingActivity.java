package com.Joseph.agroshieldapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
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

import com.Joseph.agroshieldapp.Social.NotificationService;
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

import java.util.ArrayList;
import java.util.List;

public class FollowingActivity extends AppCompatActivity {

    private static final String TAG = "FollowingActivity";
    private static final int PAGE_SIZE = 10;

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

    // Pagination
    private int suggestionPage = 0;
    private boolean isLoadingSuggestions = false;
    private boolean hasMoreSuggestions = true;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_following);

        initializeViews();
        setupRecyclerViews();
        initializeFirebase();
        setupScrollListener();
        loadData();
    }

    private void initializeViews() {
        recyclerFollowing = findViewById(R.id.recyclerFollowing);
        recyclerSuggestions = findViewById(R.id.recyclerSuggestions);
        progressBar = findViewById(R.id.progressBar);
        errorTextView = findViewById(R.id.errorTextView);

        // Setup retry button
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

    private void setupScrollListener() {
        recyclerSuggestions.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager == null) return;

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                if (!isLoadingSuggestions && hasMoreSuggestions) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0
                            && totalItemCount >= PAGE_SIZE) {
                        loadSuggestions();
                    }
                }
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
                            for (String followedUid : followingIds) {
                                fetchUserDetails(followedUid, "following");
                            }
                        } else {
                            showEmptyState("You're not following anyone yet");
                        }
                    } else {
                        showEmptyState("You're not following anyone yet");
                    }
                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading following: " + e.getMessage());
                    showError("Failed to load following list");
                    showLoading(false);
                });
    }

    private void loadSuggestions() {
        if (isLoadingSuggestions || !hasMoreSuggestions) return;

        isLoadingSuggestions = true;
        showLoading(true);

        userRef.limitToFirst((suggestionPage + 1) * PAGE_SIZE)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int processed = 0;
                        List<User> newSuggestions = new ArrayList<>();

                        for (DataSnapshot userSnap : snapshot.getChildren()) {
                            if (processed >= suggestionPage * PAGE_SIZE) {
                                String uid = userSnap.getKey();
                                if (uid != null && !uid.equals(currentUserId)) {
                                    String username = userSnap.child("name").getValue(String.class);
                                    if (username == null) username = "Unknown User";

                                    checkFollowBackStatus(uid, username, newSuggestions);
                                }
                            }
                            processed++;
                        }

                        hasMoreSuggestions = processed == ((suggestionPage + 1) * PAGE_SIZE);
                        suggestionPage++;

                        // Add new suggestions to the list
                        suggestionList.addAll(newSuggestions);
                        suggestionAdapter.notifyDataSetChanged();

                        isLoadingSuggestions = false;
                        showLoading(false);

                        if (suggestionList.isEmpty()) {
                            showEmptyState("No suggestions available");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        isLoadingSuggestions = false;
                        showLoading(false);
                        showError("Failed to load suggestions: " + error.getMessage());
                    }
                });
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
                    Log.e(TAG, "Error checking follow back status: " + e.getMessage());
                    fetchUserForSuggestion(uid, username, "follow", newSuggestions);
                });
    }

    private void fetchUserForSuggestion(String uid, String username, String followStatus, List<User> newSuggestions) {
        db.collection("profileimages").document(uid).get()
                .addOnSuccessListener(doc -> {
                    Bitmap profileBitmap = null;
                    if (doc.exists()) {
                        String base64 = doc.getString("imageBase64");
                        if (base64 != null && !base64.isEmpty()) {
                            profileBitmap = decodeBase64ToBitmap(base64);
                        }
                    }
                    User user = new User(uid, username, profileBitmap, followStatus);
                    newSuggestions.add(user);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching profile image: " + e.getMessage());
                    User user = new User(uid, username, null, followStatus);
                    newSuggestions.add(user);
                });
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
                    if (doc.exists()) {
                        String base64 = doc.getString("imageBase64");
                        if (base64 != null && !base64.isEmpty()) {
                            profileBitmap = decodeBase64ToBitmap(base64);
                        }
                    }
                    User user = new User(uid, username, profileBitmap, followStatus);
                    followingList.add(user);
                    followingAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching profile image: " + e.getMessage());
                    User user = new User(uid, username, null, followStatus);
                    followingList.add(user);
                    followingAdapter.notifyDataSetChanged();
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

    // Add this method to check and request permission
    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                // Show rationale if needed
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.POST_NOTIFICATIONS)) {

                    new AlertDialog.Builder(this)
                            .setTitle("Notification Permission")
                            .setMessage("Allow notifications to get alerts when someone follows you back")
                            .setPositiveButton("Allow", (dialog, which) ->
                                    requestNotificationPermission())
                            .setNegativeButton("Later", null)
                            .show();
                } else {
                    requestNotificationPermission();
                }
            }
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_REQUEST_CODE);
        }
    }

    // Handle permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Call this in onCreate or onStart
    @Override
    protected void onStart() {
        super.onStart();
        checkNotificationPermission();
    }

    // UI Helper Methods
    private void showLoading(boolean show) {
        runOnUiThread(() -> {
            if (progressBar != null) {
                progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            if (errorTextView != null) {
                errorTextView.setText(message);
                errorTextView.setVisibility(View.VISIBLE);
            }
            findViewById(R.id.retryButton).setVisibility(View.VISIBLE);
        });
    }

    private void hideError() {
        runOnUiThread(() -> {
            if (errorTextView != null) {
                errorTextView.setVisibility(View.GONE);
            }
            findViewById(R.id.retryButton).setVisibility(View.GONE);
        });
    }

    private void showEmptyState(String message) {
        runOnUiThread(() -> {
            if (errorTextView != null) {
                errorTextView.setText(message);
                errorTextView.setVisibility(View.VISIBLE);
            }
        });
    }

    // Event Handlers
    public void onRetryClicked() {
        hideError();
        loadData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up resources
        if (followingAdapter != null) {
            followingAdapter.cleanup();
        }
        if (suggestionAdapter != null) {
            suggestionAdapter.cleanup();
        }
        followingList.clear();
        suggestionList.clear();
    }
}