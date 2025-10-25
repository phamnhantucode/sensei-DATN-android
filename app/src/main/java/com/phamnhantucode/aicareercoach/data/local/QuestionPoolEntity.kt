package com.phamnhantucode.aicareercoach.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import org.json.JSONArray

@Entity(tableName = "question_pool")
@TypeConverters(Converters::class)
data class QuestionPoolEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val category: String, // 'quiz' or 'interview'
    val questionType: String, // 'MULTIPLE_CHOICE' or 'ESSAY'
    val questionCategory: String?, // 'TECHNICAL', 'BEHAVIORAL', 'SITUATIONAL'
    val question: String,
    val options: List<String>?,
    val correctAnswerIndex: Int?,
    val explanation: String?,
    val placeholder: String?,
    val isUsed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val usedAt: Long? = null
)

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.let { JSONArray(it).toString() }
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.let {
            val jsonArray = JSONArray(it)
            List(jsonArray.length()) { index ->
                jsonArray.getString(index)
            }
        }
    }
}
