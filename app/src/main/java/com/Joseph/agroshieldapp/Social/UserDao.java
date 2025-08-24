package com.Joseph.agroshieldapp.Social;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUser(CachedUser user);

    @Query("SELECT * FROM cached_users WHERE uid = :userId")
    CachedUser getUser(String userId);

    @Query("DELETE FROM cached_users WHERE lastUpdated < :timestamp")
    void deleteOldEntries(long timestamp);
}