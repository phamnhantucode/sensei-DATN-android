package com.phamnhantucode.aicareercoach.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

/**
 * DAO for accessing live interview cache data.
 */
@Dao
interface LiveInterviewCacheDao {

    // Interview Session Queries

    @Query("SELECT * FROM live_interview_cache WHERE id = :interviewId")
    suspend fun getInterviewById(interviewId: String): LiveInterviewCacheEntity?

    @Query("SELECT * FROM live_interview_cache WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getInterviewsByUser(userId: String): List<LiveInterviewCacheEntity>

    @Query("SELECT * FROM live_interview_cache WHERE userId = :userId AND status = :status ORDER BY createdAt DESC")
    suspend fun getInterviewsByUserAndStatus(userId: String, status: String): List<LiveInterviewCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInterview(interview: LiveInterviewCacheEntity)

    @Query("UPDATE live_interview_cache SET status = :status, completedAt = :completedAt, overallScore = :overallScore WHERE id = :interviewId")
    suspend fun updateInterviewStatus(interviewId: String, status: String, completedAt: Long?, overallScore: Float?)

    @Query("UPDATE live_interview_cache SET currentQuestionIndex = :index WHERE id = :interviewId")
    suspend fun updateQuestionIndex(interviewId: String, index: Int)

    @Query("DELETE FROM live_interview_cache WHERE id = :interviewId")
    suspend fun deleteInterview(interviewId: String)

    @Query("DELETE FROM live_interview_cache WHERE userId = :userId")
    suspend fun clearUserInterviews(userId: String)

    @Query("DELETE FROM live_interview_cache WHERE cachedAt < :beforeTimestamp")
    suspend fun deleteOldCache(beforeTimestamp: Long)

    // Question Queries

    @Query("SELECT * FROM live_interview_question_cache WHERE liveMockInterviewId = :interviewId ORDER BY createdAt ASC")
    suspend fun getQuestionsByInterview(interviewId: String): List<LiveInterviewQuestionCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: LiveInterviewQuestionCacheEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<LiveInterviewQuestionCacheEntity>)

    @Query("UPDATE live_interview_question_cache SET userAnswer = :userAnswer, audioTranscript = :transcript, feedback = :feedback, rating = :rating, duration = :duration WHERE id = :questionId")
    suspend fun updateQuestionAnswer(
        questionId: Long,
        userAnswer: String,
        transcript: String,
        feedback: String,
        rating: Int,
        duration: Long
    )

    @Query("DELETE FROM live_interview_question_cache WHERE liveMockInterviewId = :interviewId")
    suspend fun deleteQuestionsByInterview(interviewId: String)

    // Statistics Queries

    @Query("SELECT COUNT(*) FROM live_interview_question_cache WHERE liveMockInterviewId = :interviewId")
    suspend fun getQuestionCount(interviewId: String): Int

    @Query("SELECT AVG(rating) FROM live_interview_question_cache WHERE liveMockInterviewId = :interviewId AND rating IS NOT NULL")
    suspend fun getAverageRating(interviewId: String): Float?
}
