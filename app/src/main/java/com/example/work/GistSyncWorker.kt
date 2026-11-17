package com.example.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.repository.WfoRepository
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class GistSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = WfoRepository(applicationContext)
        return runSync(repository)
    }

    companion object {
        private const val WORK_NAME = "wfo_periodic_gist_sync"

        internal suspend fun runSync(repository: WfoRepository): Result {
            val settings = repository.settingsFlow.first()
            if (settings.gitToken.trim().isEmpty()) {
                return Result.success()
            }
            val pushResult = repository.pushToGist()
            return if (pushResult.isSuccess) {
                Result.success()
            } else {
                Result.retry()
            }
        }

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<GistSyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }
    }
}
