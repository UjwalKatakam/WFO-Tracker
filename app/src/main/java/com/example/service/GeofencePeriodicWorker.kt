package com.example.service

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.preferences.WfoPreferences
import com.example.util.LocationHelper
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

class GeofencePeriodicWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        try {
            val preferences = WfoPreferences(appContext)
            val settings = preferences.settingsFlow.first()

            // Only run if passive notifications are enabled and user is not currently punched in
            if (!settings.passiveGeofenceArrivalEnabled || settings.activePunchStartMillis != null) {
                return Result.success()
            }

            // Exclude weekends (Sat & Sun) and Holidays when autoMuteHolidays is enabled
            val cal = Calendar.getInstance()
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            if (settings.autoMuteHolidays && (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY)) {
                return Result.success()
            }

            // Check if today is registered as an approved Holiday / Leave exclusion
            if (settings.autoMuteHolidays) {
                val db = com.example.data.local.WfoDatabase.getInstance(appContext)
                val allLeaves = db.leaveHolidayDao().getAllLeaves().first()
                val todayStr = com.example.util.DateUtils.todayDateStr()
                val isExcludedDay = allLeaves.any { it.date == todayStr }
                if (isExcludedDay) {
                    return Result.success()
                }
            }

            // Check location against office coordinates
            val geofenceStatus = LocationHelper.checkGeofence(
                context = appContext,
                officeLat = settings.lat,
                officeLon = settings.lon,
                radiusKm = settings.radiusKm
            )

            if (geofenceStatus != null && geofenceStatus.isWithinRadius) {
                GeofenceArrivalReceiver.notifyOfficeArrival(appContext, geofenceStatus.distanceKm)
            }

            return Result.success()
        } catch (e: Exception) {
            return Result.success() // Do not retry periodically on failure
        }
    }

    companion object {
        private const val WORK_TAG = "wfo_geofence_periodic_worker"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val request = PeriodicWorkRequestBuilder<GeofencePeriodicWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun enqueuePeriodicCheck(context: Context) = schedule(context)

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_TAG)
        }
    }
}
