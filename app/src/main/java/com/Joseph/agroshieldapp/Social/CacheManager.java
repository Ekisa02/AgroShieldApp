package com.Joseph.agroshieldapp.Social;

import android.content.Context;
import android.util.Log;



public class CacheManager {
    private static final String TAG = "CacheManager";
    private static final long CACHE_DURATION = 7 * 24 * 60 * 60 * 1000; // 7 days

    private final UserDao userDao;

    public CacheManager(Context context) {
        AppDatabase database = AppDatabase.getDatabase(context);
        this.userDao = database.userDao();
    }

    public void cacheUser(String uid, String username, String profileImageBase64) {
        try {
            CachedUser cachedUser = new CachedUser(uid, username, profileImageBase64);
            userDao.insertUser(cachedUser);
            Log.d(TAG, "User cached: " + username);
        } catch (Exception e) {
            Log.e(TAG, "Error caching user: " + e.getMessage());
        }
    }

    public CachedUser getCachedUser(String uid) {
        try {
            return userDao.getUser(uid);
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving cached user: " + e.getMessage());
            return null;
        }
    }

    public void clearOldCache() {
        try {
            long threshold = System.currentTimeMillis() - CACHE_DURATION;
            userDao.deleteOldEntries(threshold);
            Log.d(TAG, "Old cache cleared");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing old cache: " + e.getMessage());
        }
    }

    public void clearAllCache() {
        try {
            userDao.clearAll();
            Log.d(TAG, "All cache cleared");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing all cache: " + e.getMessage());
        }
    }
}