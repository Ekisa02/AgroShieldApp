package com.Joseph.agroshieldapp.Social;

import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;
import androidx.cardview.widget.CardView;

public class Follower {
    private String userId;
    private String userName;
    private String profileImageUrl;
    private boolean isFollowing;

    // Empty constructor for Firebase
    public Follower() {
    }

    public Follower(String userId, String userName, String profileImageUrl, boolean isFollowing) {
        this.userId = userId;
        this.userName = userName;
        this.profileImageUrl = profileImageUrl;
        this.isFollowing = isFollowing;
    }

    // Getters and setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public boolean isFollowing() {
        return isFollowing;
    }

    public void setFollowing(boolean following) {
        isFollowing = following;
    }
}