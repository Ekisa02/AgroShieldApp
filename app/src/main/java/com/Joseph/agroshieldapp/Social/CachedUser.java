package com.Joseph.agroshieldapp.Social;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "cached_users")
public class CachedUser {
    @PrimaryKey
    public String uid;

    public String username;
    public String profileImageBase64;
    public long lastUpdated;

    public CachedUser(String uid, String username, String profileImageBase64) {
        this.uid = uid;
        this.username = username;
        this.profileImageBase64 = profileImageBase64;
        this.lastUpdated = System.currentTimeMillis();
    }
}