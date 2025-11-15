package com.phamnhantucode.aicareercoach.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/**
 * DAO for GridResume database operations
 */
@Dao
interface GridResumeDao {

    @Query("SELECT * FROM grid_resumes WHERE userId = :userId ORDER BY updatedAt DESC")
    suspend fun getAllDesignsByUser(userId: String): List<GridResumeEntity>

    @Query("SELECT * FROM grid_resumes WHERE id = :designId LIMIT 1")
    suspend fun getDesignById(designId: String): GridResumeEntity?

    @Query("SELECT * FROM grid_resumes WHERE userId = :userId ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestDesign(userId: String): GridResumeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDesign(design: GridResumeEntity)

    @Update
    suspend fun updateDesign(design: GridResumeEntity)

    @Delete
    suspend fun deleteDesign(design: GridResumeEntity)

    @Query("DELETE FROM grid_resumes WHERE id = :designId")
    suspend fun deleteDesignById(designId: String)

    @Query("DELETE FROM grid_resumes WHERE userId = :userId")
    suspend fun deleteAllDesignsForUser(userId: String)

    @Query("SELECT COUNT(*) FROM grid_resumes WHERE userId = :userId")
    suspend fun getDesignCount(userId: String): Int

    @Query("SELECT updatedAt FROM grid_resumes WHERE id = :designId")
    suspend fun getLastModifiedTime(designId: String): Long?
}
