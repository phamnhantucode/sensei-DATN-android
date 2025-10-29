package com.phamnhantucode.aicareercoach.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "tips_cache")
@TypeConverters(Converters::class)
data class TipsCacheEntity(
    @PrimaryKey
    val userId: String, // Neon user ID
    val practiceTipsJson: String, // JSON array of practice tips
    val coachingSummary: String?,
    val improvementAreas: List<String>,
    val recommendedPracticeFrequency: String?,
    val cachedAt: Long = System.currentTimeMillis()
)
