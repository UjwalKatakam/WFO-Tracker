package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.util.SecurePreferencesHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wfo_settings")

data class WfoSettings(
    val minHours: Double = 8.0,
    val targetDaysPerWeek: Int = 3,
    val targetDaysPerMonth: Int = 12,
    val shiftStart: String = "12:00",
    val lat: Double = 17.44832,
    val lon: Double = 78.37411,
    val radiusKm: Double = 0.5,
    val employeeName: String = "Employee",
    val gitToken: String = "",
    val gistId: String = "",
    val notifyOnTarget: Boolean = true,
    val gpsVerificationEnabled: Boolean = false,
    val passiveGeofenceArrivalEnabled: Boolean = true,
    val departureReminderEnabled: Boolean = true,
    val departureReminderHours: Double = 9.0,
    val autoMuteHolidays: Boolean = true,
    val activePunchStartMillis: Long? = null,
    val lastDeletedUndoJson: String? = null,
    val lastUsedTransitOffsetMinutes: Int = 15,
    val batteryOptNudgeDismissed: Boolean = false,
    val persistentUndoSummary: String? = null
)

class WfoPreferences(private val context: Context) {

    private object Keys {
        val MIN_HOURS = doublePreferencesKey("min_hours")
        val TARGET_DAYS_WEEK = intPreferencesKey("target_days_week")
        val TARGET_DAYS_MONTH = intPreferencesKey("target_days_month")
        val SHIFT_START = stringPreferencesKey("shift_start")
        val LAT = doublePreferencesKey("office_lat")
        val LON = doublePreferencesKey("office_lon")
        val RADIUS_KM = doublePreferencesKey("radius_km")
        val EMPLOYEE_NAME = stringPreferencesKey("employee_name")
        val ENCRYPTED_GIT_TOKEN = stringPreferencesKey("enc_git_token")
        val LEGACY_GIT_TOKEN = stringPreferencesKey("git_token")
        val GIST_ID = stringPreferencesKey("gist_id")
        val NOTIFY_ON_TARGET = booleanPreferencesKey("notify_on_target")
        val GPS_VERIFICATION = booleanPreferencesKey("gps_verification")
        val PASSIVE_GEOFENCE = booleanPreferencesKey("passive_geofence_arrival")
        val DEPARTURE_REMINDER = booleanPreferencesKey("departure_reminder_enabled")
        val DEPARTURE_REMINDER_HOURS = doublePreferencesKey("departure_reminder_hours")
        val AUTO_MUTE_HOLIDAYS = booleanPreferencesKey("auto_mute_holidays")
        val ACTIVE_PUNCH_START = longPreferencesKey("active_punch_start")
        val LAST_DELETED_UNDO = stringPreferencesKey("last_deleted_undo")
        val LAST_TRANSIT_OFFSET = intPreferencesKey("last_transit_offset_mins")
        val BATTERY_OPT_DISMISSED = booleanPreferencesKey("battery_opt_dismissed")
        val PERSISTENT_UNDO_SUMMARY = stringPreferencesKey("persistent_undo_summary")
    }

    val settingsFlow: Flow<WfoSettings> = context.dataStore.data.map { prefs ->
        val activeStart = prefs[Keys.ACTIVE_PUNCH_START]

        // Transparent token decryption with fallback migration
        val encryptedToken = prefs[Keys.ENCRYPTED_GIT_TOKEN]
        val legacyToken = prefs[Keys.LEGACY_GIT_TOKEN] ?: ""
        val decryptedToken = when {
            !encryptedToken.isNullOrEmpty() -> SecurePreferencesHelper.decrypt(encryptedToken)
            legacyToken.isNotEmpty() -> legacyToken
            else -> ""
        }

        WfoSettings(
            minHours = prefs[Keys.MIN_HOURS] ?: 8.0,
            targetDaysPerWeek = prefs[Keys.TARGET_DAYS_WEEK] ?: 3,
            targetDaysPerMonth = prefs[Keys.TARGET_DAYS_MONTH] ?: 12,
            shiftStart = prefs[Keys.SHIFT_START] ?: "12:00",
            lat = prefs[Keys.LAT] ?: 17.44832,
            lon = prefs[Keys.LON] ?: 78.37411,
            radiusKm = prefs[Keys.RADIUS_KM] ?: 0.5,
            employeeName = prefs[Keys.EMPLOYEE_NAME] ?: "Employee",
            gitToken = decryptedToken,
            gistId = prefs[Keys.GIST_ID] ?: "",
            notifyOnTarget = prefs[Keys.NOTIFY_ON_TARGET] ?: true,
            gpsVerificationEnabled = prefs[Keys.GPS_VERIFICATION] ?: false,
            passiveGeofenceArrivalEnabled = prefs[Keys.PASSIVE_GEOFENCE] ?: true,
            departureReminderEnabled = prefs[Keys.DEPARTURE_REMINDER] ?: true,
            departureReminderHours = prefs[Keys.DEPARTURE_REMINDER_HOURS] ?: 9.0,
            autoMuteHolidays = prefs[Keys.AUTO_MUTE_HOLIDAYS] ?: true,
            activePunchStartMillis = if (activeStart != null && activeStart > 0) activeStart else null,
            lastDeletedUndoJson = prefs[Keys.LAST_DELETED_UNDO],
            lastUsedTransitOffsetMinutes = prefs[Keys.LAST_TRANSIT_OFFSET] ?: 15,
            batteryOptNudgeDismissed = prefs[Keys.BATTERY_OPT_DISMISSED] ?: false,
            persistentUndoSummary = prefs[Keys.PERSISTENT_UNDO_SUMMARY]
        )
    }

    suspend fun updateSettings(
        minHours: Double,
        targetDaysPerWeek: Int,
        shiftStart: String,
        lat: Double,
        lon: Double,
        radiusKm: Double,
        employeeName: String,
        gitToken: String,
        gistId: String,
        notifyOnTarget: Boolean,
        gpsVerificationEnabled: Boolean,
        passiveGeofenceArrivalEnabled: Boolean = true,
        departureReminderEnabled: Boolean = true,
        departureReminderHours: Double = 9.0,
        autoMuteHolidays: Boolean = true
    ) {
        val trimmedToken = gitToken.trim()
        val encryptedToken = SecurePreferencesHelper.encrypt(trimmedToken)

        context.dataStore.edit { prefs ->
            prefs[Keys.MIN_HOURS] = minHours
            prefs[Keys.TARGET_DAYS_WEEK] = targetDaysPerWeek
            prefs[Keys.TARGET_DAYS_MONTH] = targetDaysPerWeek * 4
            prefs[Keys.SHIFT_START] = shiftStart
            prefs[Keys.LAT] = lat
            prefs[Keys.LON] = lon
            prefs[Keys.RADIUS_KM] = radiusKm
            prefs[Keys.EMPLOYEE_NAME] = employeeName
            prefs[Keys.ENCRYPTED_GIT_TOKEN] = encryptedToken
            // Remove legacy plaintext token from disk
            prefs.remove(Keys.LEGACY_GIT_TOKEN)
            prefs[Keys.GIST_ID] = gistId.trim()
            prefs[Keys.NOTIFY_ON_TARGET] = notifyOnTarget
            prefs[Keys.GPS_VERIFICATION] = gpsVerificationEnabled
            prefs[Keys.PASSIVE_GEOFENCE] = passiveGeofenceArrivalEnabled
            prefs[Keys.DEPARTURE_REMINDER] = departureReminderEnabled
            prefs[Keys.DEPARTURE_REMINDER_HOURS] = departureReminderHours
            prefs[Keys.AUTO_MUTE_HOLIDAYS] = autoMuteHolidays
        }
    }

    suspend fun setActivePunchStart(startMillis: Long?) {
        context.dataStore.edit { prefs ->
            if (startMillis == null) {
                prefs.remove(Keys.ACTIVE_PUNCH_START)
            } else {
                prefs[Keys.ACTIVE_PUNCH_START] = startMillis
            }
        }
    }

    suspend fun setLastDeletedUndo(undoJson: String?) {
        context.dataStore.edit { prefs ->
            if (undoJson == null) {
                prefs.remove(Keys.LAST_DELETED_UNDO)
            } else {
                prefs[Keys.LAST_DELETED_UNDO] = undoJson
            }
        }
    }

    suspend fun setLastUsedTransitOffset(offsetMins: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_TRANSIT_OFFSET] = offsetMins
        }
    }

    suspend fun setBatteryOptDismissed(dismissed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BATTERY_OPT_DISMISSED] = dismissed
        }
    }

    suspend fun setPersistentUndoSummary(summary: String?) {
        context.dataStore.edit { prefs ->
            if (summary == null) {
                prefs.remove(Keys.PERSISTENT_UNDO_SUMMARY)
            } else {
                prefs[Keys.PERSISTENT_UNDO_SUMMARY] = summary
            }
        }
    }
}
