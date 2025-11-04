package com.phamnhantucode.aicareercoach.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for caching live interview sessions locally.
 */
@Entity(tableName = "live_interview_cache")
data class LiveInterviewCacheEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val interviewType: String, // TECHNICAL, BEHAVIORAL, GENERAL
    val status: String, // NOT_STARTED, IN_PROGRESS, PAUSED, COMPLETED, ABANDONED
    val targetQuestionCount: Int,
    val currentQuestionIndex: Int,
    val overallScore: Float?,
    val startedAt: Long?,
    val completedAt: Long?,
    val createdAt: Long = System.currentTimeMillis(),
    val cachedAt: Long = System.currentTimeMillis()
)
