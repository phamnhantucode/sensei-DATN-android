package com.phamnhantucode.aicareercoach.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface QuestionPoolDao {

    @Query("SELECT COUNT(*) FROM question_pool WHERE userId = :userId AND category = :category AND isUsed = 0")
    suspend fun getUnusedQuestionsCount(userId: String, category: String): Int

    @Query("SELECT * FROM question_pool WHERE userId = :userId AND category = :category AND isUsed = 0 ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getUnusedQuestions(userId: String, category: String, limit: Int): List<QuestionPoolEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionPoolEntity>)

    @Query("UPDATE question_pool SET isUsed = 1, usedAt = :usedAt WHERE id IN (:questionIds)")
    suspend fun markQuestionsAsUsed(questionIds: List<String>, usedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM question_pool WHERE userId = :userId")
    suspend fun clearUserQuestions(userId: String)

    @Query("DELETE FROM question_pool WHERE userId = :userId AND isUsed = 1 AND usedAt < :beforeTimestamp")
    suspend fun deleteOldUsedQuestions(userId: String, beforeTimestamp: Long)
}
