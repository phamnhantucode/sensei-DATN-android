package com.phamnhantucode.aicareercoach.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserProfileCacheDao {

    @Query("SELECT * FROM user_profile_cache WHERE userId = :userId LIMIT 1")
    suspend fun getUserProfile(userId: String): UserProfileCacheEntity?

    @Query("SELECT * FROM user_profile_cache WHERE clerkUserId = :clerkUserId LIMIT 1")
    suspend fun getUserProfileByClerkId(clerkUserId: String): UserProfileCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfileCacheEntity)

    @Query("DELETE FROM user_profile_cache WHERE userId = :userId")
    suspend fun clearUserProfile(userId: String)

    @Query("SELECT cachedAt FROM user_profile_cache WHERE userId = :userId")
    suspend fun getCacheTimestamp(userId: String): Long?
}
