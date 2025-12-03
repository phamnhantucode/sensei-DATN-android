package com.phamnhantucode.aicareercoach.data.resume

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.gson.Gson
import com.phamnhantucode.aicareercoach.data.neon.NeonAuth
import com.phamnhantucode.aicareercoach.data.neon.NeonResumeService
import com.phamnhantucode.aicareercoach.ui.resumebuilder.Resume
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker for reliable resume sync to Neon.
 * Survives app process death and retries with exponential backoff on failure.
 */
class ResumeSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "ResumeSyncWorker"
        private const val KEY_RESUME_JSON = "resume_json"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_IS_UPDATE = "is_update"

        /**
         * Enqueues a resume sync operation.
         * Uses REPLACE policy to coalesce rapid auto-save requests for the same resume.
         */
        fun enqueue(
            context: Context,
            resume: Resume,
            userId: String,
            isUpdate: Boolean = false
        ) {
            val resumeJson = Gson().toJson(resume)
            
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<ResumeSyncWorker>()
                .setConstraints(constraints)
                .setInputData(
                    workDataOf(
                        KEY_RESUME_JSON to resumeJson,
                        KEY_USER_ID to userId,
                        KEY_IS_UPDATE to isUpdate
                    )
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.SECONDS
                )
                .build()

            // Use resume ID as unique work name to coalesce rapid saves
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "resume_sync_${resume.id}",
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )

            Log.d(TAG, "Enqueued sync for resume ${resume.id}")
        }
    }

    override suspend fun doWork(): Result {
        val resumeJson = inputData.getString(KEY_RESUME_JSON)
        val userId = inputData.getString(KEY_USER_ID)
        val isUpdate = inputData.getBoolean(KEY_IS_UPDATE, false)

        if (resumeJson == null || userId == null) {
            Log.e(TAG, "Missing required input data")
            return Result.failure()
        }

        return try {
            val resume = Gson().fromJson(resumeJson, Resume::class.java)
            val authToken = NeonAuth.fetchNeonAuthToken()

            val result = if (isUpdate) {
                NeonResumeService.updateResume(resume, authToken, null)
            } else {
                NeonResumeService.saveResume(resume, userId, authToken, null)
            }

            if (result.isSuccess) {
                Log.d(TAG, "Successfully synced resume ${resume.id}")
                Result.success()
            } else {
                Log.w(TAG, "Failed to sync resume, will retry", result.exceptionOrNull())
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing resume", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Log.e(TAG, "Max retries exceeded for resume sync")
                Result.failure()
            }
        }
    }
}
