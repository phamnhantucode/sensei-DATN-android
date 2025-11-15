package com.phamnhantucode.aicareercoach.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.GridResume
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.ResumeElement
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.ResumeElementTypeAdapter

/**
 * Room Entity for storing grid-based resume designs
 * Uses JSON serialization for the GridResume data structure
 */
@Entity(tableName = "grid_resumes")
@TypeConverters(GridResumeConverters::class)
data class GridResumeEntity(
    @PrimaryKey
    val id: String,
    val userId: String, // User who owns this design
    val name: String, // Auto-generated name like "Resume Design 1"
    val designData: GridResume, // Full grid resume data
    val thumbnail: String, // Base64 encoded thumbnail image
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Type converters for GridResume JSON serialization
 */
class GridResumeConverters {

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(ResumeElement::class.java, ResumeElementTypeAdapter())
        .serializeNulls()
        .create()

    @TypeConverter
    fun fromGridResume(gridResume: GridResume): String {
        return gson.toJson(gridResume)
    }

    @TypeConverter
    fun toGridResume(json: String): GridResume {
        return gson.fromJson(json, GridResume::class.java)
    }
}
