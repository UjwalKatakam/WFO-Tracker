package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.example.MainActivity
import com.example.data.repository.WfoRepository
import com.example.util.HapticHelper
import com.example.widget.WfoAppWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PunchActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        val repository = WfoRepository(appContext)

        when (intent.action) {
            PunchTimerService.ACTION_PUNCH_OUT, ACTION_WIDGET_PUNCH_OUT -> {
                HapticHelper.performClick(appContext)
                CoroutineScope(Dispatchers.IO).launch {
                    val log = repository.punchOut(addTransitMins = 0)
                    PunchTimerService.stop(appContext)
                    WfoAppWidgetProvider.updateAllWidgets(appContext)

                    Handler(Looper.getMainLooper()).post {
                        val msg = if (log != null) {
                            "🚪 Punched Out! Logged ${log.formattedDuration} total."
                        } else {
                            "🚪 Punched Out recorded."
                        }
                        Toast.makeText(appContext, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            ACTION_WIDGET_PUNCH_IN -> {
                HapticHelper.performClick(appContext)
                CoroutineScope(Dispatchers.IO).launch {
                    val settings = repository.settingsFlow.first()
                    val startMillis = System.currentTimeMillis()
                    repository.punchIn(startMillis)
                    PunchTimerService.start(appContext, startMillis, settings.minHours)
                    WfoAppWidgetProvider.updateAllWidgets(appContext)

                    Handler(Looper.getMainLooper()).post {
                        val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(startMillis))
                        Toast.makeText(appContext, "🏢 Punched In at $timeStr!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            PunchTimerService.ACTION_VIEW_TARGET -> {
                val activityIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("show_target_dialog", true)
                }
                context.startActivity(activityIntent)
            }
        }
    }

    companion object {
        const val ACTION_WIDGET_PUNCH_IN = "com.aistudio.wfotracker.ACTION_WIDGET_PUNCH_IN"
        const val ACTION_WIDGET_PUNCH_OUT = "com.aistudio.wfotracker.ACTION_WIDGET_PUNCH_OUT"
    }
}
