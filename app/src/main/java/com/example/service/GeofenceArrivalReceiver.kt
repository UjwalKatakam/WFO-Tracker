package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.preferences.WfoPreferences
import com.example.data.repository.WfoRepository
import com.example.widget.WfoAppWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class GeofenceArrivalReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        val repository = WfoRepository(appContext)

        when (intent.action) {
            ACTION_PUNCH_NOW_EXPLICIT -> {
                // User clicked "Punch In Now" (starts at current time - NO automatic offset added)
                val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(NOTIFICATION_ID_ARRIVAL)

                CoroutineScope(Dispatchers.IO).launch {
                    val settings = repository.settingsFlow.first()
                    val startMillis = System.currentTimeMillis()
                    repository.punchIn(startMillis)
                    PunchTimerService.start(appContext, startMillis, settings.minHours)
                    WfoAppWidgetProvider.updateAllWidgets(appContext)
                }
            }

            ACTION_ADD_OFFSET_CONFIRM -> {
                // User explicitly selected the action to add an offset (e.g., 15 mins transit)
                val offsetMins = intent.getIntExtra(EXTRA_OFFSET_MINS, 15)
                val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(NOTIFICATION_ID_ARRIVAL)

                CoroutineScope(Dispatchers.IO).launch {
                    val settings = repository.settingsFlow.first()
                    val startMillis = System.currentTimeMillis() - (offsetMins * 60 * 1000L)
                    repository.punchIn(startMillis)
                    PunchTimerService.start(appContext, startMillis, settings.minHours)
                    WfoAppWidgetProvider.updateAllWidgets(appContext)
                }
            }

            ACTION_DISMISS -> {
                val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(NOTIFICATION_ID_ARRIVAL)
            }
        }
    }

    companion object {
        const val GEOFENCE_CHANNEL_ID = "wfo_geofence_arrival_channel"
        const val NOTIFICATION_ID_ARRIVAL = 3030

        const val ACTION_PUNCH_NOW_EXPLICIT = "com.aistudio.wfotracker.ACTION_PUNCH_NOW_EXPLICIT"
        const val ACTION_ADD_OFFSET_CONFIRM = "com.aistudio.wfotracker.ACTION_ADD_OFFSET_CONFIRM"
        const val ACTION_DISMISS = "com.aistudio.wfotracker.ACTION_GEOFENCE_DISMISS"
        const val EXTRA_OFFSET_MINS = "extra_offset_mins"

        fun notifyOfficeArrival(context: Context, distanceKm: Double) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    GEOFENCE_CHANNEL_ID,
                    "Office Arrival Notifications",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Prompts to confirm punch-in and asks before applying any transit offsets"
                    enableVibration(true)
                    setSound(
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .build()
                    )
                }
                notificationManager.createNotificationChannel(channel)
            }

            // Tap notification opens app directly to the dashboard with offset selector dialog
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("show_arrival_prompt", true)
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                301,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Action 1: "Punch In Now" (current time, 0 offset)
            val punchNowIntent = Intent(context, GeofenceArrivalReceiver::class.java).apply {
                action = ACTION_PUNCH_NOW_EXPLICIT
            }
            val punchNowPendingIntent = PendingIntent.getBroadcast(
                context,
                302,
                punchNowIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Action 2: "Add 15m Offset?" (explicit user approval to backdate by 15 mins)
            val addOffsetIntent = Intent(context, GeofenceArrivalReceiver::class.java).apply {
                action = ACTION_ADD_OFFSET_CONFIRM
                putExtra(EXTRA_OFFSET_MINS, 15)
            }
            val addOffsetPendingIntent = PendingIntent.getBroadcast(
                context,
                303,
                addOffsetIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val distanceText = if (distanceKm < 0.1) "at office" else String.format("%.2f km away", distanceKm)
            val notification = NotificationCompat.Builder(context, GEOFENCE_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("📍 Arrived at Office ($distanceText)")
                .setContentText("You've arrived! Ready to punch in? Tap to customize offset or pick below.")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("You've arrived at the office! Punch in now, or choose whether to backdate with your transit time.")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(openAppPendingIntent)
                .addAction(R.drawable.widget_button_bg, "Punch In (Now)", punchNowPendingIntent)
                .addAction(R.drawable.widget_button_bg, "+15m Transit?", addOffsetPendingIntent)
                .build()

            notificationManager.notify(NOTIFICATION_ID_ARRIVAL, notification)
        }
    }
}
