package com.phamnhantucode.aicareercoach.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TipsCacheDao {

    @Query("SELECT * FROM tips_cache WHERE userId = :userId LIMIT 1")
    suspend fun getTips(userId: String): TipsCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTips(tips: TipsCacheEntity)

    @Query("DELETE FROM tips_cache WHERE userId = :userId")
    suspend fun clearTips(userId: String)

    @Query("SELECT cachedAt FROM tips_cache WHERE userId = :userId")
    suspend fun getCacheTimestamp(userId: String): Long?
}
