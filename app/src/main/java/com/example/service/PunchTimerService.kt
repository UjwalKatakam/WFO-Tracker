package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.preferences.WfoPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PunchTimerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var targetCheckJob: Job? = null
    private var hasAlertedTarget = false
    private var hasAlertedDeparture = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TIMER -> {
                val startMillis = intent.getLongExtra(EXTRA_START_MILLIS, System.currentTimeMillis())
                val targetHours = intent.getDoubleExtra(EXTRA_TARGET_HOURS, 8.0)
                startForegroundTimer(startMillis, targetHours)
            }
            ACTION_STOP_SERVICE -> {
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(DEPARTURE_NOTIFICATION_ID)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startForegroundTimer(startMillis: Long, targetHours: Double) {
        val targetMillis = startMillis + (targetHours * 3600 * 1000L).toLong()
        val targetTimeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(targetMillis))

        // Open app intent
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "dashboard")
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 101, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Punch Out action
        val punchOutIntent = Intent(ACTION_PUNCH_OUT).apply {
            setPackage(packageName)
        }
        val punchOutPendingIntent = PendingIntent.getBroadcast(
            this, 102, punchOutIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // View Target action
        val viewTargetIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("show_target_dialog", true)
        }
        val viewTargetPendingIntent = PendingIntent.getActivity(
            this, 103, viewTargetIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🏢 Punched In — Working at Office")
            .setContentText("Target Goal (${String.format(Locale.US, "%.1f", targetHours)}h): $targetTimeStr")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setUsesChronometer(true)
            .setWhen(startMillis)
            .setShowWhen(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                android.R.drawable.ic_media_pause,
                "Stop / Punch Out",
                punchOutPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_my_calendar,
                "Target: $targetTimeStr",
                viewTargetPendingIntent
            )
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        // Monitor target arrival and departure reminder in background
        targetCheckJob?.cancel()
        targetCheckJob = serviceScope.launch {
            val repository = com.example.data.repository.WfoRepository(applicationContext)
            while (isActive) {
                val now = System.currentTimeMillis()
                val settings = repository.settingsFlow.first()

                if (now >= targetMillis && !hasAlertedTarget && settings.notifyOnTarget) {
                    hasAlertedTarget = true
                    triggerTargetReachedAlert(targetHours)
                }

                // Check "Did you forget to punch out?" departure threshold
                val departureReminderMillis = startMillis + (settings.departureReminderHours * 3600 * 1000L).toLong()
                if (now >= departureReminderMillis && !hasAlertedDeparture && settings.departureReminderEnabled) {
                    hasAlertedDeparture = true
                    triggerDepartureReminderAlert(settings.departureReminderHours)
                }

                delay(15_000L) // check every 15 seconds
            }
        }
    }

    private fun triggerDepartureReminderAlert(hours: Double) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 250, 100, 250), -1)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 250, 100, 250), -1)
        }

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("show_departure_prompt", true)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 105, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val punchOutNowIntent = Intent(this, PunchActionReceiver::class.java).apply {
            action = ACTION_PUNCH_OUT
        }
        val punchOutNowPendingIntent = PendingIntent.getBroadcast(
            this, 106, punchOutNowIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val reminderNotification = NotificationCompat.Builder(this, DEPARTURE_CHANNEL_ID)
            .setContentTitle("🚪 Still at the Office? (${String.format(Locale.US, "%.1f", hours)}h active)")
            .setContentText("Did you forget to punch out? Tap to review or punch out now.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Your timer has been running for over ${String.format(Locale.US, "%.1f", hours)} hours. If you've already left, tap to punch out or backdate your departure time.")
            )
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri)
            .addAction(android.R.drawable.ic_lock_power_off, "Punch Out Now", punchOutNowPendingIntent)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(DEPARTURE_NOTIFICATION_ID, reminderNotification)
    }

    private fun triggerTargetReachedAlert(targetHours: Double) {
        com.example.util.HapticHelper.performMilestoneCelebration(this)

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("target_reached", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 104, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val alertNotification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setContentTitle("🎉 Daily WFO Quota Achieved!")
            .setContentText("Congratulations! You have completed your ${String.format(Locale.US, "%.1f", targetHours)} hours target.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(ALERT_NOTIFICATION_ID, alertNotification)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Timer channel (silent, low priority chronometer)
            val timerChannel = NotificationChannel(
                CHANNEL_ID,
                "WFO Active Chronometer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Live ticking notification for active office session"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(timerChannel)

            // Alert channel (sound and vibration for target reached)
            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
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
            notificationManager.createNotificationChannel(alertChannel)

            // Departure Reminder channel (Did you forget to punch out?)
            val departureChannel = NotificationChannel(
                DEPARTURE_CHANNEL_ID,
                "WFO Departure Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminds you if punch timer is running past normal departure hours"
                enableVibration(true)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                )
            }
            notificationManager.createNotificationChannel(departureChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        targetCheckJob?.cancel()
    }

    companion object {
        const val CHANNEL_ID = "wfo_live_timer_channel"
        const val ALERT_CHANNEL_ID = "wfo_target_reached_channel"
        const val DEPARTURE_CHANNEL_ID = "wfo_departure_reminder_channel"
        const val NOTIFICATION_ID = 2024
        const val ALERT_NOTIFICATION_ID = 2025
        const val DEPARTURE_NOTIFICATION_ID = 2026

        const val ACTION_START_TIMER = "com.aistudio.wfotracker.START_TIMER"
        const val ACTION_STOP_SERVICE = "com.aistudio.wfotracker.STOP_SERVICE"
        const val ACTION_PUNCH_OUT = "com.aistudio.wfotracker.ACTION_PUNCH_OUT"
        const val ACTION_VIEW_TARGET = "com.aistudio.wfotracker.ACTION_VIEW_TARGET"

        const val EXTRA_START_MILLIS = "extra_start_millis"
        const val EXTRA_TARGET_HOURS = "extra_target_hours"

        fun start(context: Context, startMillis: Long, targetHours: Double) {
            val intent = Intent(context, PunchTimerService::class.java).apply {
                action = ACTION_START_TIMER
                putExtra(EXTRA_START_MILLIS, startMillis)
                putExtra(EXTRA_TARGET_HOURS, targetHours)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, PunchTimerService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }
}
