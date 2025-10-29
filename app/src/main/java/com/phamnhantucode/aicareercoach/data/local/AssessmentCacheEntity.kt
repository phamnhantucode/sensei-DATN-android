package com.phamnhantucode.aicareercoach.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assessment_cache")
data class AssessmentCacheEntity(
    @PrimaryKey
    val id: String,
    val userId: String, // Neon user ID
    val createdAt: Long, // Unix timestamp in milliseconds
    val quizScore: Double,
    val category: String,
    val improvementTip: String?,
    val cachedAt: Long = System.currentTimeMillis()
)
