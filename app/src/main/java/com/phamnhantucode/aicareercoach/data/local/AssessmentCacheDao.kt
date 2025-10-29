package com.phamnhantucode.aicareercoach.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AssessmentCacheDao {

    @Query("SELECT * FROM assessment_cache WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getAssessmentsByUser(userId: String): List<AssessmentCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssessments(assessments: List<AssessmentCacheEntity>)

    @Query("DELETE FROM assessment_cache WHERE userId = :userId")
    suspend fun clearUserAssessments(userId: String)

    @Query("DELETE FROM assessment_cache WHERE cachedAt < :beforeTimestamp")
    suspend fun deleteOldCache(beforeTimestamp: Long)

    @Query("SELECT cachedAt FROM assessment_cache WHERE userId = :userId ORDER BY cachedAt DESC LIMIT 1")
    suspend fun getLatestCacheTimestamp(userId: String): Long?
}
