package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.preferences.WfoPreferences
import com.example.service.PunchActionReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WfoAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateAllWidgets(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        fun updateAllWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager? = null,
            widgetIds: IntArray? = null
        ) {
            val manager = appWidgetManager ?: AppWidgetManager.getInstance(context)
            val ids = widgetIds ?: manager.getAppWidgetIds(
                ComponentName(context, WfoAppWidgetProvider::class.java)
            )
            if (ids.isEmpty()) return

            CoroutineScope(Dispatchers.IO).launch {
                val preferences = WfoPreferences(context)
                val settings = preferences.settingsFlow.first()
                val isPunchedIn = settings.activePunchStartMillis != null && settings.activePunchStartMillis > 0

                for (id in ids) {
                    val views = RemoteViews(context.packageName, R.layout.widget_wfo_tracker)

                    // Root click opens main app
                    val openAppIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val openAppPendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        openAppIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)

                    if (isPunchedIn) {
                        val startMillis = settings.activePunchStartMillis!!
                        val targetMillis = startMillis + (settings.minHours * 3600 * 1000L).toLong()
                        val targetTimeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(targetMillis))

                        // Status Pill
                        views.setTextViewText(R.id.widget_status_pill, "● LIVE AT OFFICE")
                        views.setTextColor(R.id.widget_status_pill, 0xFF10B981.toInt())

                        // Chronometer
                        views.setViewVisibility(R.id.widget_chronometer, View.VISIBLE)
                        views.setViewVisibility(R.id.widget_static_time, View.GONE)
                        val elapsedRealtimeNow = SystemClock.elapsedRealtime()
                        val wallClockNow = System.currentTimeMillis()
                        val baseRealtime = elapsedRealtimeNow - (wallClockNow - startMillis)
                        views.setChronometer(R.id.widget_chronometer, baseRealtime, null, true)

                        // Subtitle
                        views.setTextViewText(R.id.widget_sub_info, "Target (${String.format(Locale.US, "%.1f", settings.minHours)}h): $targetTimeStr")

                        // Action Button: Punch Out
                        views.setTextViewText(R.id.widget_action_btn, "Punch Out")
                        val punchOutIntent = Intent(context, PunchActionReceiver::class.java).apply {
                            action = PunchActionReceiver.ACTION_WIDGET_PUNCH_OUT
                        }
                        val punchOutPendingIntent = PendingIntent.getBroadcast(
                            context,
                            201,
                            punchOutIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.widget_action_btn, punchOutPendingIntent)
                    } else {
                        // Inactive state
                        views.setTextViewText(R.id.widget_status_pill, "OFFLINE")
                        views.setTextColor(R.id.widget_status_pill, 0xFF94A3B8.toInt())

                        views.setViewVisibility(R.id.widget_chronometer, View.GONE)
                        views.setViewVisibility(R.id.widget_static_time, View.VISIBLE)
                        views.setTextViewText(R.id.widget_static_time, "Not Punched In")
                        views.setTextViewText(R.id.widget_sub_info, "Daily Target: ${String.format(Locale.US, "%.1f", settings.minHours)} hours")

                        // Action Button: Punch In (Opens app with dialog to confirm or punches in directly)
                        views.setTextViewText(R.id.widget_action_btn, "Punch In")
                        val punchInIntent = Intent(context, PunchActionReceiver::class.java).apply {
                            action = PunchActionReceiver.ACTION_WIDGET_PUNCH_IN
                        }
                        val punchInPendingIntent = PendingIntent.getBroadcast(
                            context,
                            202,
                            punchInIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.widget_action_btn, punchInPendingIntent)
                    }

                    manager.updateAppWidget(id, views)
                }
            }
        }
    }
}
