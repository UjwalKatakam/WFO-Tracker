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
import androidx.core.content.ContextCompat
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
                val presentColor = ContextCompat.getColor(context, R.color.app_present)
                val mutedColor = ContextCompat.getColor(context, R.color.app_muted)
                val minHoursStr = String.format(Locale.US, "%.1f", settings.minHours)

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

                        // Status label
                        views.setTextViewText(R.id.widget_status_pill, context.getString(R.string.widget_status_in_office))
                        views.setTextColor(R.id.widget_status_pill, presentColor)

                        // Chronometer
                        views.setViewVisibility(R.id.widget_chronometer, View.VISIBLE)
                        views.setViewVisibility(R.id.widget_static_time, View.GONE)
                        val elapsedRealtimeNow = SystemClock.elapsedRealtime()
                        val wallClockNow = System.currentTimeMillis()
                        val baseRealtime = elapsedRealtimeNow - (wallClockNow - startMillis)
                        views.setChronometer(R.id.widget_chronometer, baseRealtime, null, true)

                        // Subtitle
                        views.setTextViewText(
                            R.id.widget_sub_info,
                            context.getString(R.string.widget_target_time, minHoursStr, targetTimeStr)
                        )

                        // Action Button: Punch out
                        views.setTextViewText(R.id.widget_action_btn, context.getString(R.string.widget_action_punch_out))
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
                        views.setTextViewText(R.id.widget_status_pill, context.getString(R.string.widget_status_idle))
                        views.setTextColor(R.id.widget_status_pill, mutedColor)

                        views.setViewVisibility(R.id.widget_chronometer, View.GONE)
                        views.setViewVisibility(R.id.widget_static_time, View.VISIBLE)
                        views.setTextViewText(R.id.widget_static_time, context.getString(R.string.widget_status_idle))
                        views.setTextViewText(
                            R.id.widget_sub_info,
                            context.getString(R.string.widget_daily_target, minHoursStr)
                        )

                        // Action Button: Punch in
                        views.setTextViewText(R.id.widget_action_btn, context.getString(R.string.widget_action_punch_in))
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
