package com.phamnhantucode.aicareercoach.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity for caching live interview questions and answers locally.
 */
@Entity(
    tableName = "live_interview_question_cache",
    foreignKeys = [
        ForeignKey(
            entity = LiveInterviewCacheEntity::class,
            parentColumns = ["id"],
            childColumns = ["liveMockInterviewId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["liveMockInterviewId"])]
)
data class LiveInterviewQuestionCacheEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val liveMockInterviewId: String,
    val questionText: String,
    val category: String,
    val correctAnswer: String,
    val userAnswer: String?,
    val audioTranscript: String?,
    val feedback: String?,
    val rating: Int?,
    val duration: Long?,
    val createdAt: Long = System.currentTimeMillis(),
    val cachedAt: Long = System.currentTimeMillis()
)
