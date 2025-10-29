package com.phamnhantucode.aicareercoach.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "user_profile_cache")
@TypeConverters(Converters::class)
data class UserProfileCacheEntity(
    @PrimaryKey
    val userId: String, // Neon user ID
    val clerkUserId: String,
    val industry: String?,
    val experienceYears: Int?,
    val skills: List<String>,
    val bio: String?,
    val cachedAt: Long = System.currentTimeMillis()
)
