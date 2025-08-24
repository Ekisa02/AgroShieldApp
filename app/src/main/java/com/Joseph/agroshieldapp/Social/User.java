package com.Joseph.agroshieldapp.Social;

import android.graphics.Bitmap;

public class User {
    private String uid;
    private String username;
    private Bitmap profileImage;
    private String followStatus; // "follow", "following", "follow_back"

    // Empty constructor (needed for Firebase)
    public User() {}

    // Full constructor
    public User(String uid, String username, Bitmap profileImage, String followStatus) {
        this.uid = uid;
        this.username = username;
        this.profileImage = profileImage;
        this.followStatus = followStatus;
    }

    // Getters and Setters
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Bitmap getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(Bitmap profileImage) {
        this.profileImage = profileImage;
    }

    public String getFollowStatus() {
        return followStatus;
    }

    public void setFollowStatus(String followStatus) {
        this.followStatus = followStatus;
    }
}