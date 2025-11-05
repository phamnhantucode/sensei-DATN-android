package com.phamnhantucode.aicareercoach.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ResumeDao {

    @Query("SELECT * FROM resumes WHERE userId = :userId ORDER BY updatedAt DESC")
    suspend fun getAllResumesByUser(userId: String): List<ResumeEntity>

    @Query("SELECT * FROM resumes WHERE id = :resumeId LIMIT 1")
    suspend fun getResumeById(resumeId: String): ResumeEntity?

    @Query("SELECT * FROM resumes WHERE userId = :userId ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestResume(userId: String): ResumeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResume(resume: ResumeEntity)

    @Update
    suspend fun updateResume(resume: ResumeEntity)

    @Delete
    suspend fun deleteResume(resume: ResumeEntity)

    @Query("DELETE FROM resumes WHERE id = :resumeId")
    suspend fun deleteResumeById(resumeId: String)

    @Query("DELETE FROM resumes WHERE userId = :userId")
    suspend fun deleteAllResumesForUser(userId: String)

    @Query("SELECT COUNT(*) FROM resumes WHERE userId = :userId")
    suspend fun getResumeCount(userId: String): Int

    @Query("SELECT updatedAt FROM resumes WHERE id = :resumeId")
    suspend fun getLastModifiedTime(resumeId: String): Long?
}
