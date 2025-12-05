package com.phamnhantucode.aicareercoach.data.resume

import android.content.Context
import android.util.Log
import com.clerk.api.Clerk
import com.phamnhantucode.aicareercoach.data.local.AppDatabase
import com.phamnhantucode.aicareercoach.data.local.GridResumeEntity
import com.phamnhantucode.aicareercoach.data.neon.NeonAuth
import com.phamnhantucode.aicareercoach.data.neon.NeonGridResumeService
import com.phamnhantucode.aicareercoach.data.neon.NeonUserService
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.GridResume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository responsible for managing grid-based resume designs.
 * Now operates in Neon-only mode (local caching disabled to fix sync corruption issues).
 */
class GridResumeRepository private constructor(context: Context) {

    // Keep DAO reference for potential future use, but don't use it for now
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
     * Gets the current user's Clerk ID for local storage
     */
    private fun getLocalUserId(): String {
        val clerkUser = Clerk.user
            ?: throw IllegalStateException("User not logged in")
        return clerkUser.id
    }

    /**
     * Gets the current user's Neon ID (the auto-generated hex ID, not Clerk ID).
     * If the user doesn't exist in Neon yet, creates them first.
     * This is used for remote sync to Neon database.
     */
    /**
     * Gets the current user's Neon ID (the auto-generated hex ID, not Clerk ID).
     * If the user doesn't exist in Neon yet, creates them first.
     * This is used for remote sync to Neon database.
     */
    private suspend fun getNeonUserId(): String {
        val clerkUser = Clerk.user
            ?: throw IllegalStateException("User not logged in")
        
        val authToken = NeonAuth.fetchNeonAuthToken()
        
        // Get Neon user's internal ID (not the Clerk ID) for foreign key references
        var neonUser = NeonUserService.getUser(clerkUser.id, authToken)
        
        // If user doesn't exist in Neon, create them
        if (neonUser == null) {
            Log.d(TAG, "[GridResumeRepository] User not found in Neon, creating...")
            NeonUserService.upsertUser(clerkUser, authToken)
            neonUser = NeonUserService.getUser(clerkUser.id, authToken)
        }
        
        return neonUser?.id ?: throw IllegalStateException("Failed to get Neon user ID")
    }

    /**
     * Saves a grid resume design directly to Neon (local caching disabled)
     */
    suspend fun saveDesign(
        gridResume: GridResume,
        thumbnail: String = "",
        syncToRemote: Boolean = true
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val neonUserId = getNeonUserId()
            
            // Check if design exists in Neon to determine name
            val authToken = NeonAuth.fetchNeonAuthToken()
            val existing = NeonGridResumeService.getGridResume(gridResume.id, authToken).getOrNull()
            
            val name = if (existing != null) {
                existing.name
            } else {
                generateDesignName(neonUserId, authToken)
            }

            // Save directly to Neon
            val remoteResult = NeonGridResumeService.saveGridResume(
                gridResume = gridResume.copy(name = name, userId = neonUserId),
                userId = neonUserId,
                thumbnail = thumbnail,
                authToken = authToken
            )
            
            if (remoteResult.isFailure) {
                Log.e(TAG, "[GridResumeRepository] Failed to save grid resume to Neon", remoteResult.exceptionOrNull())
                return@withContext Result.failure(remoteResult.exceptionOrNull() ?: Exception("Failed to save design"))
            }
            
            Log.d(TAG, "[GridResumeRepository] Saved grid resume ${gridResume.id} to Neon with name: $name")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "[GridResumeRepository] Error saving grid resume design", e)
            Result.failure(e)
        }
    }

    /**
     * Updates an existing grid resume design directly in Neon (local caching disabled)
     */
    suspend fun updateDesign(
        gridResume: GridResume,
        thumbnail: String? = null,
        syncToRemote: Boolean = true
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val neonUserId = getNeonUserId()
            val authToken = NeonAuth.fetchNeonAuthToken()
            
            // Get existing to preserve name
            val existing = NeonGridResumeService.getGridResume(gridResume.id, authToken).getOrNull()
            val name = existing?.name ?: gridResume.name

            // Update directly in Neon
            val remoteResult = NeonGridResumeService.updateGridResume(
                gridResume = gridResume.copy(name = name, userId = neonUserId),
                thumbnail = thumbnail?.takeIf { it.isNotBlank() },  // Pass null if blank to preserve existing
                authToken = authToken
            )
            
            if (remoteResult.isFailure) {
                Log.e(TAG, "[GridResumeRepository] Failed to update grid resume in Neon", remoteResult.exceptionOrNull())
                return@withContext Result.failure(remoteResult.exceptionOrNull() ?: Exception("Failed to update design"))
            }
            
            Log.d(TAG, "[GridResumeRepository] Updated grid resume ${gridResume.id} in Neon")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "[GridResumeRepository] Error updating grid resume design", e)
            Result.failure(e)
        }
    }

    /**
     * Gets a design by ID directly from Neon (local caching disabled)
     */
    suspend fun getDesign(designId: String): Result<GridResume?> =
        withContext(Dispatchers.IO) {
            try {
                val authToken = NeonAuth.fetchNeonAuthToken()
                val result = NeonGridResumeService.getGridResume(designId, authToken)
                
                if (result.isSuccess) {
                    val design = result.getOrNull()
                    Log.d(TAG, "[GridResumeRepository] Retrieved design $designId from Neon")
                    Result.success(design)
                } else {
                    Log.w(TAG, "[GridResumeRepository] Failed to get design from Neon", result.exceptionOrNull())
                    Result.success(null)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "[GridResumeRepository] Error getting design", e)
                Result.failure(e)
            }
        }

    /**
     * Gets all designs for the current user directly from Neon (local caching disabled)
     * Returns GridResumeEntity list for compatibility with existing code
     */
    suspend fun getAllDesigns(): Result<List<GridResumeEntity>> =
        withContext(Dispatchers.IO) {
            try {
                val neonUserId = getNeonUserId()
                val authToken = NeonAuth.fetchNeonAuthToken()
                val result = NeonGridResumeService.getAllGridResumesForUser(neonUserId, authToken)
                
                if (result.isSuccess) {
                    val designs = result.getOrNull() ?: emptyList()
                    // Convert to GridResumeEntity for compatibility
                    val entities = designs.map { design ->
                        GridResumeEntity(
                            id = design.id,
                            userId = getLocalUserId(),
                            name = design.name,
                            designData = design,
                            thumbnail = design.thumbnail,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                    }
                    Log.d(TAG, "[GridResumeRepository] Retrieved ${entities.size} designs from Neon for user $neonUserId")
                    Result.success(entities)
                } else {
                    Log.e(TAG, "[GridResumeRepository] Failed to get designs from Neon", result.exceptionOrNull())
                    Result.failure(result.exceptionOrNull() ?: Exception("Failed to get designs"))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "[GridResumeRepository] Error getting all designs", e)
                Result.failure(e)
            }
        }

    /**
     * Gets the most recently updated design from Neon
     */
    suspend fun getLatestDesign(): Result<GridResume?> = withContext(Dispatchers.IO) {
        try {
            val allDesigns = getAllDesigns()
            val designs = allDesigns.getOrNull() ?: emptyList()
            Result.success(designs.firstOrNull()?.designData)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "[GridResumeRepository] Error getting latest design", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes a design from Neon (local caching disabled)
     */
    suspend fun deleteDesign(
        designId: String,
        syncToRemote: Boolean = true
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val authToken = NeonAuth.fetchNeonAuthToken()
            val remoteResult = NeonGridResumeService.deleteGridResume(
                resumeId = designId,
                authToken = authToken
            )
            
            if (remoteResult.isFailure) {
                Log.e(TAG, "[GridResumeRepository] Failed to delete grid resume from Neon", remoteResult.exceptionOrNull())
                return@withContext Result.failure(remoteResult.exceptionOrNull() ?: Exception("Failed to delete design"))
            }
            
            Log.d(TAG, "[GridResumeRepository] Deleted design $designId from Neon")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "[GridResumeRepository] Error deleting design", e)
            Result.failure(e)
        }
    }

    /**
     * Gets the count of designs for the current user (from Neon)
     */
    suspend fun getDesignCount(): Int = withContext(Dispatchers.IO) {
        try {
            val allDesigns = getAllDesigns()
            allDesigns.getOrNull()?.size ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "[GridResumeRepository] Error getting design count", e)
            0
        }
    }

    /**
     * Clears all designs for the current user (no-op in Neon-only mode)
     */
    suspend fun clearAllDesigns(): Result<Unit> = withContext(Dispatchers.IO) {
        // No-op in Neon-only mode
        Log.d(TAG, "[GridResumeRepository] clearAllDesigns called but local caching is disabled")
        Result.success(Unit)
    }

    /**
     * Generates auto-incremented design name like "Resume Design 1", "Resume Design 2", etc.
     */
    private suspend fun generateDesignName(userId: String, authToken: String?): String {
        val count = try {
            val result = NeonGridResumeService.getAllGridResumesForUser(userId, authToken)
            result.getOrNull()?.size ?: 0
        } catch (e: Exception) {
            0
        }
        return "Resume Design ${count + 1}"
    }

    /**
     * Syncs all local grid resumes to remote Neon database (no-op in Neon-only mode)
     */
    suspend fun syncAllToRemote(): Result<Int> = withContext(Dispatchers.IO) {
        // No-op in Neon-only mode - data is already in Neon
        Log.d(TAG, "[GridResumeRepository] syncAllToRemote called but local caching is disabled")
        Result.success(0)
    }
}
