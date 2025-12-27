package com.phamnhantucode.aicareercoach.data.resume

import android.content.Context
import android.util.Log
import com.clerk.api.Clerk
import com.phamnhantucode.aicareercoach.data.local.AppDatabase
import com.phamnhantucode.aicareercoach.data.local.ResumeEntity
import com.phamnhantucode.aicareercoach.data.neon.NeonAuth
import com.phamnhantucode.aicareercoach.data.neon.NeonResumeService
import com.phamnhantucode.aicareercoach.data.neon.NeonUserService
import com.phamnhantucode.aicareercoach.ui.resumebuilder.Resume
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.GridResume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Manages resume data
class ResumeRepository private constructor(context: Context) {

    // Keep DAO reference for potential future use, but don't use it for now
    private val resumeDao = AppDatabase.getDatabase(context).resumeDao()

    companion object {
        private const val TAG = "ResumeRepository"

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
     * Gets the current user's Neon ID (the auto-generated hex ID, not Clerk ID).
     * If the user doesn't exist in Neon yet, creates them first.
     */
    // Get current Neon user ID
    suspend fun getCurrentUserId(): String? {
        return try {
            val clerkUser = Clerk.user ?: return null

            val email = clerkUser.emailAddresses.firstOrNull()?.emailAddress ?: throw IllegalStateException("User email not found")
            val result = NeonUserService.syncUser(clerkUser.id, email)
            val neonUser = result.getOrNull()
            
            neonUser?.id
        } catch (e: CancellationException) {
            // Rethrow cancellation to properly propagate coroutine cancellation
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "[ResumeRepository] Error getting current user ID", e)
            null
        }
    }

    
    // Require current user ID
    private suspend fun requireCurrentUserId(): String {
        return getCurrentUserId() ?: throw IllegalStateException("User not logged in or failed to get user ID")
    }

    // Save resume
    @Deprecated(
        message = "Use GridResumeRepository.saveDesign() instead",
        replaceWith = ReplaceWith(
            expression = "GridResumeRepository.getInstance(context).saveDesign(gridResume, thumbnail, syncToRemote)",
            imports = ["com.phamnhantucode.aicareercoach.data.resume.GridResumeRepository"]
        )
    )
    suspend fun saveResume(
        resume: Resume,
        syncToRemote: Boolean = true,
        gridResume: GridResume? = null,
        preserveExistingJson: Boolean = true
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val userId = requireCurrentUserId()
                
                // Save directly to Neon (skip local)
                val authToken = NeonAuth.fetchNeonAuthToken()
                val remoteResult = NeonResumeService.saveResume(resume, userId, authToken, gridResume, preserveExistingJson)
                if (remoteResult.isFailure) {
                    Log.e(TAG, "[ResumeRepository] Failed to save resume to Neon", remoteResult.exceptionOrNull())
                    return@withContext Result.failure(remoteResult.exceptionOrNull() ?: Exception("Failed to save resume"))
                }
                
                Log.d(TAG, "[ResumeRepository] Saved resume ${resume.id} to Neon")
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "[ResumeRepository] Error saving resume", e)
                Result.failure(e)
            }
        }

    // Update resume
    @Deprecated(
        message = "Use GridResumeRepository.updateDesign() instead",
        replaceWith = ReplaceWith(
            expression = "GridResumeRepository.getInstance(context).updateDesign(gridResume, thumbnail, syncToRemote)",
            imports = ["com.phamnhantucode.aicareercoach.data.resume.GridResumeRepository"]
        )
    )
    suspend fun updateResume(
        resume: Resume,
        syncToRemote: Boolean = true,
        gridResume: GridResume? = null,
        preserveExistingJson: Boolean = true
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                // Update directly in Neon (skip local)
                val authToken = NeonAuth.fetchNeonAuthToken()
                val remoteResult = NeonResumeService.updateResume(resume, authToken, gridResume, preserveExistingJson)
                if (remoteResult.isFailure) {
                    Log.e(TAG, "[ResumeRepository] Failed to update resume in Neon", remoteResult.exceptionOrNull())
                    return@withContext Result.failure(remoteResult.exceptionOrNull() ?: Exception("Failed to update resume"))
                }
                
                Log.d(TAG, "[ResumeRepository] Updated resume ${resume.id} in Neon")
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "[ResumeRepository] Error updating resume", e)
                Result.failure(e)
            }
        }

    // Get resume
    suspend fun getResume(resumeId: String, forceRemote: Boolean = false): Result<Resume?> =
        withContext(Dispatchers.IO) {
            try {
                // Fetch directly from Neon (skip local)
                val authToken = NeonAuth.fetchNeonAuthToken()
                val remoteResult = NeonResumeService.getResume(resumeId, authToken)
                if (remoteResult.isSuccess) {
                    val resume = remoteResult.getOrNull()
                    Log.d(TAG, "[ResumeRepository] Retrieved resume $resumeId from Neon")
                    return@withContext Result.success(resume)
                }
                
                Log.w(TAG, "[ResumeRepository] Failed to get resume from Neon", remoteResult.exceptionOrNull())
                Result.success(null)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "[ResumeRepository] Error getting resume", e)
                Result.failure(e)
            }
        }

    // Get all resumes
    suspend fun getAllResumes(forceRemote: Boolean = false): Result<List<Resume>> =
        withContext(Dispatchers.IO) {
            try {
                val userId = requireCurrentUserId()

                // Fetch directly from Neon (skip local)
                val authToken = NeonAuth.fetchNeonAuthToken()
                val remoteResult = NeonResumeService.getAllResumesForUser(userId, authToken)
                if (remoteResult.isSuccess) {
                    val resumes = remoteResult.getOrNull() ?: emptyList()
                    Log.d(TAG, "[ResumeRepository] Retrieved ${resumes.size} resumes from Neon")
                    return@withContext Result.success(resumes)
                }

                Log.e(TAG, "[ResumeRepository] Failed to get resumes from Neon", remoteResult.exceptionOrNull())
                Result.failure(remoteResult.exceptionOrNull() ?: Exception("Failed to get resumes"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "[ResumeRepository] Error getting all resumes", e)
                Result.failure(e)
            }
        }

    // Get latest resume
    suspend fun getLatestResume(): Result<Resume?> = withContext(Dispatchers.IO) {
        try {
            val allResumes = getAllResumes(forceRemote = true)
            val resumes = allResumes.getOrNull() ?: emptyList()
            Result.success(resumes.firstOrNull())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "[ResumeRepository] Error getting latest resume", e)
            Result.failure(e)
        }
    }

    // Delete resume
    suspend fun deleteResume(resumeId: String, syncToRemote: Boolean = true): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                // Delete from Neon directly (skip local)
                val authToken = NeonAuth.fetchNeonAuthToken()
                val remoteResult = NeonResumeService.deleteResume(resumeId, authToken)
                if (remoteResult.isFailure) {
                    Log.e(TAG, "[ResumeRepository] Failed to delete from Neon", remoteResult.exceptionOrNull())
                    return@withContext Result.failure(remoteResult.exceptionOrNull() ?: Exception("Failed to delete resume"))
                }
                
                Log.d(TAG, "[ResumeRepository] Deleted resume $resumeId from Neon")
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "[ResumeRepository] Error deleting resume", e)
                Result.failure(e)
            }
        }

    // Get resume count
    suspend fun getResumeCount(): Int = withContext(Dispatchers.IO) {
        try {
            val allResumes = getAllResumes(forceRemote = true)
            allResumes.getOrNull()?.size ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "[ResumeRepository] Error getting resume count", e)
            0
        }
    }

    // Clear local cache
    suspend fun clearLocalCache(): Result<Unit> = withContext(Dispatchers.IO) {
        // No-op in Neon-only mode
        Log.d(TAG, "[ResumeRepository] clearLocalCache called but local caching is disabled")
        Result.success(Unit)
    }

    // Sync all to remote
    suspend fun syncAllToRemote(): Result<Int> = withContext(Dispatchers.IO) {
        // No-op in Neon-only mode - data is already in Neon
        Log.d(TAG, "[ResumeRepository] syncAllToRemote called but local caching is disabled")
        Result.success(0)
    }
}
