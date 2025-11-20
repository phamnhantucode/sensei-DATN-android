package com.phamnhantucode.aicareercoach.data.resume

import android.content.Context
import android.util.Log
import com.clerk.api.Clerk
import com.phamnhantucode.aicareercoach.data.local.AppDatabase
import com.phamnhantucode.aicareercoach.data.local.GridResumeEntity
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.GridResume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository responsible for managing grid-based resume designs.
 * Manages local storage only (no remote sync).
 */
class GridResumeRepository private constructor(context: Context) {

    private val gridResumeDao = AppDatabase.getDatabase(context).gridResumeDao()

    companion object {
        private const val TAG = "GridResumeRepository"

        @Volatile
        private var INSTANCE: GridResumeRepository? = null

        fun getInstance(context: Context): GridResumeRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GridResumeRepository(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    /**
     * Gets the current user's ID
     */
    private suspend fun getCurrentUserId(): String {
        val clerkUser = Clerk.user
            ?: throw IllegalStateException("User not logged in")
        return clerkUser.id
    }

    /**
     * Saves a grid resume design
     */
    suspend fun saveDesign(
        gridResume: GridResume,
        thumbnail: String = ""
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId()
            val now = System.currentTimeMillis()

            // Get existing entity to preserve createdAt
            val existing = gridResumeDao.getDesignById(gridResume.id)
            val createdAt = existing?.createdAt ?: now

            // Generate auto name if this is a new design
            val name = if (existing != null) {
                existing.name
            } else {
                generateDesignName(userId)
            }

            val entity = GridResumeEntity(
                id = gridResume.id,
                userId = userId,
                name = name,
                designData = gridResume.copy(name = name),
                thumbnail = thumbnail,
                createdAt = createdAt,
                updatedAt = now
            )

            gridResumeDao.insertDesign(entity)
            Log.d(TAG, "Saved grid resume design ${gridResume.id} with name: $name")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving grid resume design", e)
            Result.failure(e)
        }
    }

    /**
     * Updates an existing grid resume design
     */
    suspend fun updateDesign(
        gridResume: GridResume,
        thumbnail: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId()
            val now = System.currentTimeMillis()

            // Get existing entity
            val existing = gridResumeDao.getDesignById(gridResume.id)
                ?: return@withContext Result.failure(Exception("Design not found: ${gridResume.id}"))

            val entity = GridResumeEntity(
                id = gridResume.id,
                userId = userId,
                name = existing.name, // Preserve name
                designData = gridResume.copy(name = existing.name),
                thumbnail = thumbnail ?: existing.thumbnail,
                createdAt = existing.createdAt,
                updatedAt = now
            )

            gridResumeDao.updateDesign(entity)
            Log.d(TAG, "Updated grid resume design ${gridResume.id}")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating grid resume design", e)
            Result.failure(e)
        }
    }

    /**
     * Gets a design by ID
     */
    suspend fun getDesign(designId: String): Result<GridResume?> =
        withContext(Dispatchers.IO) {
            try {
                val entity = gridResumeDao.getDesignById(designId)
                if (entity != null) {
                    Log.d(TAG, "Retrieved design $designId")
                    Result.success(entity.designData)
                } else {
                    Result.success(null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting design", e)
                Result.failure(e)
            }
        }

    /**
     * Gets all designs for the current user
     */
    suspend fun getAllDesigns(): Result<List<GridResumeEntity>> =
        withContext(Dispatchers.IO) {
            try {
                val userId = getCurrentUserId()
                val designs = gridResumeDao.getAllDesignsByUser(userId)
                Log.d(TAG, "Retrieved ${designs.size} designs for user $userId")
                Result.success(designs)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting all designs", e)
                Result.failure(e)
            }
        }

    /**
     * Gets the most recently updated design
     */
    suspend fun getLatestDesign(): Result<GridResume?> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId()
            val latest = gridResumeDao.getLatestDesign(userId)

            if (latest != null) {
                Log.d(TAG, "Retrieved latest design ${latest.id}")
                Result.success(latest.designData)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting latest design", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes a design
     */
    suspend fun deleteDesign(designId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                gridResumeDao.deleteDesignById(designId)
                Log.d(TAG, "Deleted design $designId")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting design", e)
                Result.failure(e)
            }
        }

    /**
     * Gets the count of designs for the current user
     */
    suspend fun getDesignCount(): Int = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId()
            gridResumeDao.getDesignCount(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting design count", e)
            0
        }
    }

    /**
     * Clears all designs for the current user
     */
    suspend fun clearAllDesigns(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId()
            gridResumeDao.deleteAllDesignsForUser(userId)
            Log.d(TAG, "Cleared all designs for user $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing designs", e)
            Result.failure(e)
        }
    }

    /**
     * Generates auto-incremented design name like "Resume Design 1", "Resume Design 2", etc.
     */
    private suspend fun generateDesignName(userId: String): String {
        val count = gridResumeDao.getDesignCount(userId)
        return "Resume Design ${count + 1}"
    }
}
