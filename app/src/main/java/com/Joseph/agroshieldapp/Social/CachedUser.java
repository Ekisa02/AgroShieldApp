package com.Joseph.agroshieldapp.Social;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "cached_users")
public class CachedUser {
    @PrimaryKey
    @NonNull
    public String uid;

    public String username;
    public String profileImageBase64;
    public long lastUpdated;

    public CachedUser(@NonNull String uid, String username, String profileImageBase64) {
        this.uid = uid;
        this.username = username;
        this.profileImageBase64 = profileImageBase64;
        this.lastUpdated = System.currentTimeMillis();
    }

    @NonNull
    public String getUid() {
        return uid;
    }

    public void setUid(@NonNull String uid) {
        this.uid = uid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getProfileImageBase64() {
        return profileImageBase64;
    }

    public void setProfileImageBase64(String profileImageBase64) {
        this.profileImageBase64 = profileImageBase64;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}