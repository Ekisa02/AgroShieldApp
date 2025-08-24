package com.Joseph.agroshieldapp.Social;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.Joseph.agroshieldapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {
    private static final String TAG = "UserAdapter";

    private List<User> userList;
    private Context context;
    private FirebaseFirestore db;
    private String currentUserId;
    private Map<String, ListenerRegistration> listeners = new HashMap<>();

    public UserAdapter(Context context, List<User> userList) {
        this.context = context;
        this.userList = userList;
        this.db = FirebaseFirestore.getInstance();
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.user_item_with_follow, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position >= userList.size()) return;

        User user = userList.get(position);
        holder.username.setText(user.getUsername());

        // Set profile image
        if (user.getProfileImage() != null) {
            holder.profileImage.setImageBitmap(user.getProfileImage());
        } else {
            holder.profileImage.setImageResource(R.drawable.ic_profile_placeholder);
        }

        // Set follow button text and behavior
        holder.followButton.setText(user.getFollowStatus());
        setupFollowButton(holder.followButton, user);

        // Setup real-time listener for this user's follow status
        setupRealTimeListener(user.getUid(), holder.followButton, position);
    }

    private void setupFollowButton(Button followButton, User user) {
        followButton.setOnClickListener(v -> {
            String currentStatus = user.getFollowStatus();

            if ("follow".equals(currentStatus)) {
                followUser(user.getUid());
                user.setFollowStatus("following");
            } else if ("following".equals(currentStatus)) {
                unfollowUser(user.getUid());
                user.setFollowStatus("follow");
            } else if ("follow_back".equals(currentStatus)) {
                followUser(user.getUid());
                user.setFollowStatus("following");
            }

            followButton.setText(user.getFollowStatus());
        });
    }

    private void followUser(String targetUserId) {
        // Add to current user's following
        db.collection("following").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Map<String, Object> followingData = new HashMap<>();
                    if (documentSnapshot.exists()) {
                        List<String> followingIds = (List<String>) documentSnapshot.get("followingIds");
                        if (followingIds != null && !followingIds.contains(targetUserId)) {
                            followingIds.add(targetUserId);
                            followingData.put("followingIds", followingIds);
                        } else {
                            return; // Already following
                        }
                    } else {
                        followingData.put("followingIds", java.util.Arrays.asList(targetUserId));
                    }
                    db.collection("following").document(currentUserId).set(followingData)
                            .addOnFailureListener(e ->
                                    Log.e(TAG, "Error updating following: " + e.getMessage()));
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error checking following: " + e.getMessage()));

        // Add to target user's followers
        db.collection("followers").document(targetUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Map<String, Object> followerData = new HashMap<>();
                    if (documentSnapshot.exists()) {
                        List<String> followerIds = (List<String>) documentSnapshot.get("followerIds");
                        if (followerIds != null && !followerIds.contains(currentUserId)) {
                            followerIds.add(currentUserId);
                            followerData.put("followerIds", followerIds);
                        } else {
                            return; // Already a follower
                        }
                    } else {
                        followerData.put("followerIds", java.util.Arrays.asList(currentUserId));
                    }
                    db.collection("followers").document(targetUserId).set(followerData)
                            .addOnFailureListener(e ->
                                    Log.e(TAG, "Error updating followers: " + e.getMessage()));
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error checking followers: " + e.getMessage()));
    }

    private void unfollowUser(String targetUserId) {
        // Remove from current user's following
        db.collection("following").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> followingIds = (List<String>) documentSnapshot.get("followingIds");
                        if (followingIds != null && followingIds.contains(targetUserId)) {
                            followingIds.remove(targetUserId);
                            Map<String, Object> followingData = new HashMap<>();
                            followingData.put("followingIds", followingIds);
                            db.collection("following").document(currentUserId).set(followingData)
                                    .addOnFailureListener(e ->
                                            Log.e(TAG, "Error updating following: " + e.getMessage()));
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error checking following: " + e.getMessage()));

        // Remove from target user's followers
        db.collection("followers").document(targetUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> followerIds = (List<String>) documentSnapshot.get("followerIds");
                        if (followerIds != null && followerIds.contains(currentUserId)) {
                            followerIds.remove(currentUserId);
                            Map<String, Object> followerData = new HashMap<>();
                            followerData.put("followerIds", followerIds);
                            db.collection("followers").document(targetUserId).set(followerData)
                                    .addOnFailureListener(e ->
                                            Log.e(TAG, "Error updating followers: " + e.getMessage()));
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error checking followers: " + e.getMessage()));
    }

    private void setupRealTimeListener(String targetUserId, Button followButton, int position) {
        // Remove existing listener if any
        if (listeners.containsKey(targetUserId)) {
            listeners.get(targetUserId).remove();
            listeners.remove(targetUserId);
        }

        // Add new real-time listener
        ListenerRegistration listener = db.collection("following")
                .document(currentUserId)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen failed: " + error.getMessage());
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        List<String> followingIds = (List<String>) documentSnapshot.get("followingIds");
                        boolean isFollowing = followingIds != null && followingIds.contains(targetUserId);

                        // Update UI on main thread
                        if (context instanceof Activity) {
                            ((Activity) context).runOnUiThread(() -> {
                                if (position < userList.size() && position >= 0) {
                                    User user = userList.get(position);
                                    if (isFollowing) {
                                        user.setFollowStatus("following");
                                    } else {
                                        checkFollowBackStatus(targetUserId, user, followButton);
                                    }
                                    followButton.setText(user.getFollowStatus());
                                }
                            });
                        }
                    }
                });

        listeners.put(targetUserId, listener);
    }

    private void checkFollowBackStatus(String targetUserId, User user, Button followButton) {
        db.collection("followers")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> followerIds = (List<String>) documentSnapshot.get("followerIds");
                        boolean shouldFollowBack = followerIds != null && followerIds.contains(targetUserId);

                        if (context instanceof Activity) {
                            ((Activity) context).runOnUiThread(() -> {
                                user.setFollowStatus(shouldFollowBack ? "follow_back" : "follow");
                                followButton.setText(user.getFollowStatus());
                            });
                        }
                    } else {
                        if (context instanceof Activity) {
                            ((Activity) context).runOnUiThread(() -> {
                                user.setFollowStatus("follow");
                                followButton.setText(user.getFollowStatus());
                            });
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error checking follow back status: " + e.getMessage()));
    }

    @Override
    public int getItemCount() {
        return userList != null ? userList.size() : 0;
    }

    public void updateData(List<User> newUsers) {
        if (userList != null) {
            userList.clear();
            userList.addAll(newUsers);
            notifyDataSetChanged();
        }
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        // Clean up listeners when view is recycled
        int position = holder.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION && position < userList.size()) {
            User user = userList.get(position);
            if (listeners.containsKey(user.getUid())) {
                listeners.get(user.getUid()).remove();
                listeners.remove(user.getUid());
            }
        }
    }

    public void cleanup() {
        // Remove all listeners
        for (ListenerRegistration listener : listeners.values()) {
            if (listener != null) {
                listener.remove();
            }
        }
        listeners.clear();

        // Clear data
        if (userList != null) {
            userList.clear();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView username;
        public ImageView profileImage;
        public Button followButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.username);
            profileImage = itemView.findViewById(R.id.profileImage);
            followButton = itemView.findViewById(R.id.followButton);
        }
    }
}