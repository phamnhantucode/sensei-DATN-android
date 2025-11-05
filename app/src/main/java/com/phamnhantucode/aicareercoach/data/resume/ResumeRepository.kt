package com.phamnhantucode.aicareercoach.data.resume

import android.content.Context
import android.util.Log
import com.clerk.api.Clerk
import com.phamnhantucode.aicareercoach.data.local.AppDatabase
import com.phamnhantucode.aicareercoach.data.local.ResumeEntity
import com.phamnhantucode.aicareercoach.data.neon.NeonResumeService
import com.phamnhantucode.aicareercoach.data.neon.NeonUserService
import com.phamnhantucode.aicareercoach.ui.resumebuilder.Resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * Repository responsible for managing resume data.
 * Coordinates between Room (local cache) and Neon (remote database).
 */
class ResumeRepository private constructor(context: Context) {

    private val resumeDao = AppDatabase.getDatabase(context).resumeDao()

    companion object {
        private const val TAG = "ResumeRepository"
        private const val CACHE_VALIDITY_MS = 5 * 60 * 1000L // 5 minutes

        @Volatile
        private var INSTANCE: ResumeRepository? = null

        fun getInstance(context: Context): ResumeRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ResumeRepository(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    /**
     * Gets the current user's Neon ID
     */
    private suspend fun getCurrentUserId(): String {
        val clerkUser = Clerk.user
            ?: throw IllegalStateException("User not logged in")

        // Try to get Neon user ID
        val neonUser = NeonUserService.getUser(clerkUser.id)
        return neonUser?.clerkUserId ?: clerkUser.id
    }

    /**
     * Saves a resume locally and syncs to remote
     */
    suspend fun saveResume(resume: Resume, syncToRemote: Boolean = true): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val userId = getCurrentUserId()
                val now = System.currentTimeMillis()

                // Save to local database
                val entity = ResumeEntity(
                    id = resume.id,
                    userId = userId,
                    resumeData = resume,
                    createdAt = now,
                    updatedAt = now
                )
                resumeDao.insertResume(entity)
                Log.d(TAG, "Saved resume ${resume.id} locally")

                // Sync to remote if requested
                if (syncToRemote) {
                    val remoteResult = NeonResumeService.saveResume(resume, userId)
                    if (remoteResult.isFailure) {
                        Log.w(TAG, "Failed to sync resume to remote", remoteResult.exceptionOrNull())
                        // Don't fail the whole operation if remote sync fails
                    } else {
                        Log.d(TAG, "Synced resume ${resume.id} to remote")
                    }
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error saving resume", e)
                Result.failure(e)
            }
        }

    /**
     * Updates an existing resume locally and syncs to remote
     */
    suspend fun updateResume(resume: Resume, syncToRemote: Boolean = true): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val userId = getCurrentUserId()
                val now = System.currentTimeMillis()

                // Get existing entity to preserve createdAt
                val existing = resumeDao.getResumeById(resume.id)
                val createdAt = existing?.createdAt ?: now

                // Update local database
                val entity = ResumeEntity(
                    id = resume.id,
                    userId = userId,
                    resumeData = resume,
                    createdAt = createdAt,
                    updatedAt = now
                )
                resumeDao.updateResume(entity)
                Log.d(TAG, "Updated resume ${resume.id} locally")

                // Sync to remote if requested
                if (syncToRemote) {
                    val remoteResult = NeonResumeService.updateResume(resume)
                    if (remoteResult.isFailure) {
                        Log.w(TAG, "Failed to sync update to remote", remoteResult.exceptionOrNull())
                    } else {
                        Log.d(TAG, "Synced resume update ${resume.id} to remote")
                    }
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating resume", e)
                Result.failure(e)
            }
        }

    /**
     * Gets a resume by ID, preferring local cache
     */
    suspend fun getResume(resumeId: String, forceRemote: Boolean = false): Result<Resume?> =
        withContext(Dispatchers.IO) {
            try {
                // Try local first unless force remote is requested
                if (!forceRemote) {
                    val local = resumeDao.getResumeById(resumeId)
                    if (local != null) {
                        Log.d(TAG, "Retrieved resume $resumeId from local cache")
                        return@withContext Result.success(local.resumeData)
                    }
                }

                // Fetch from remote
                val remoteResult = NeonResumeService.getResume(resumeId)
                if (remoteResult.isSuccess) {
                    val resume = remoteResult.getOrNull()
                    if (resume != null) {
                        // Cache locally
                        saveResume(resume, syncToRemote = false)
                        Log.d(TAG, "Retrieved and cached resume $resumeId from remote")
                        return@withContext Result.success(resume)
                    }
                }

                Result.success(null)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting resume", e)
                Result.failure(e)
            }
        }

    /**
     * Gets all resumes for the current user
     */
    suspend fun getAllResumes(forceRemote: Boolean = false): Result<List<Resume>> =
        withContext(Dispatchers.IO) {
            try {
                val userId = getCurrentUserId()

                // Try local first unless force remote is requested
                if (!forceRemote) {
                    val localResumes = resumeDao.getAllResumesByUser(userId)
                    if (localResumes.isNotEmpty()) {
                        // Check if cache is still valid
                        val newestUpdate = localResumes.maxOfOrNull { it.updatedAt } ?: 0L
                        val cacheAge = System.currentTimeMillis() - newestUpdate

                        if (cacheAge < CACHE_VALIDITY_MS) {
                            Log.d(TAG, "Retrieved ${localResumes.size} resumes from local cache")
                            return@withContext Result.success(localResumes.map { it.resumeData })
                        }
                    }
                }

                // Fetch from remote
                val remoteResult = NeonResumeService.getAllResumesForUser(userId)
                if (remoteResult.isSuccess) {
                    val resumes = remoteResult.getOrNull() ?: emptyList()

                    // Cache all locally
                    resumes.forEach { resume ->
                        saveResume(resume, syncToRemote = false)
                    }

                    Log.d(TAG, "Retrieved and cached ${resumes.size} resumes from remote")
                    return@withContext Result.success(resumes)
                }

                // Return local cache even if remote fails
                val localResumes = resumeDao.getAllResumesByUser(userId)
                Log.d(TAG, "Remote fetch failed, returning ${localResumes.size} cached resumes")
                Result.success(localResumes.map { it.resumeData })
            } catch (e: Exception) {
                Log.e(TAG, "Error getting all resumes", e)
                Result.failure(e)
            }
        }

    /**
     * Gets the most recently updated resume for the current user
     */
    suspend fun getLatestResume(): Result<Resume?> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId()
            val latest = resumeDao.getLatestResume(userId)

            if (latest != null) {
                Log.d(TAG, "Retrieved latest resume ${latest.id}")
                Result.success(latest.resumeData)
            } else {
                // Try remote
                val allRemote = getAllResumes(forceRemote = true)
                val resumes = allRemote.getOrNull() ?: emptyList()
                Result.success(resumes.firstOrNull())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting latest resume", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes a resume locally and from remote
     */
    suspend fun deleteResume(resumeId: String, syncToRemote: Boolean = true): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                // Delete from local
                resumeDao.deleteResumeById(resumeId)
                Log.d(TAG, "Deleted resume $resumeId locally")

                // Sync to remote if requested
                if (syncToRemote) {
                    val remoteResult = NeonResumeService.deleteResume(resumeId)
                    if (remoteResult.isFailure) {
                        Log.w(TAG, "Failed to delete from remote", remoteResult.exceptionOrNull())
                    } else {
                        Log.d(TAG, "Deleted resume $resumeId from remote")
                    }
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting resume", e)
                Result.failure(e)
            }
        }

    /**
     * Gets the count of resumes for the current user
     */
    suspend fun getResumeCount(): Int = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId()
            resumeDao.getResumeCount(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting resume count", e)
            0
        }
    }

    /**
     * Clears all local resume cache
     */
    suspend fun clearLocalCache(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId()
            resumeDao.deleteAllResumesForUser(userId)
            Log.d(TAG, "Cleared local resume cache for user $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing local cache", e)
            Result.failure(e)
        }
    }

    /**
     * Syncs all local resumes to remote
     */
    suspend fun syncAllToRemote(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId()
            val localResumes = resumeDao.getAllResumesByUser(userId)

            var syncedCount = 0
            localResumes.forEach { entity ->
                val result = NeonResumeService.saveResume(entity.resumeData, userId)
                if (result.isSuccess) {
                    syncedCount++
                }
            }

            Log.d(TAG, "Synced $syncedCount of ${localResumes.size} resumes to remote")
            Result.success(syncedCount)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing to remote", e)
            Result.failure(e)
        }
    }
}
