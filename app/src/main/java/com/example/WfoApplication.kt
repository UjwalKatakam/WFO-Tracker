package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import com.example.data.local.WfoDatabase
import com.example.service.PunchTimerService
import com.example.work.GistSyncWorker

class WfoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize Room database
        WfoDatabase.getInstance(this)

        // Setup notification channels
        createNotificationChannels()

        // Schedule periodic cloud sync via WorkManager
        try {
            GistSyncWorker.schedule(this)
            com.example.service.GeofencePeriodicWorker.schedule(this)
        } catch (e: Exception) {
            // Ignored in headless test environments
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            val liveTimerChannel = NotificationChannel(
                PunchTimerService.CHANNEL_ID,
                "WFO Active Chronometer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Live ticking notification for ongoing office check-in"
                setShowBadge(false)
            }

            val alertChannel = NotificationChannel(
                PunchTimerService.ALERT_CHANNEL_ID,
                "WFO Target Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Audible & vibration alerts when daily target hours are reached"
                enableVibration(true)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                )
            }

            notificationManager.createNotificationChannel(liveTimerChannel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }
}
