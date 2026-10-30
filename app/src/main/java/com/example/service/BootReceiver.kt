package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.preferences.WfoPreferences
import com.example.widget.WfoAppWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Re-registers background jobs, geofences, widgets, and resumes active punch sessions
 * after the device reboots or the package is updated.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val appContext = context.applicationContext
            CoroutineScope(Dispatchers.IO).launch {
                val preferences = WfoPreferences(appContext)
                val settings = preferences.settingsFlow.first()

                // 1. Resume ongoing live timer service if the user was punched in prior to reboot
                val activeStart = settings.activePunchStartMillis
                if (activeStart != null && activeStart > 0) {
                    PunchTimerService.start(appContext, activeStart, settings.minHours)
                }

                // 2. Re-arm geofence periodic background check if enabled
                if (settings.passiveGeofenceArrivalEnabled) {
                    GeofencePeriodicWorker.enqueuePeriodicCheck(appContext)
                }

                // 3. Refresh home screen widgets
                WfoAppWidgetProvider.updateAllWidgets(appContext)
            }
        }
    }
}
