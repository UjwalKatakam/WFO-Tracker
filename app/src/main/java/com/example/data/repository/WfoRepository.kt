package com.example.data.repository

import android.content.Context
import com.example.data.local.CheckInLog
import com.example.data.local.LeaveHoliday
import com.example.data.local.WfoDatabase
import com.example.data.network.GistFileContent
import com.example.data.network.GistUpdateRequest
import com.example.data.network.GitHubGistApi
import com.example.data.preferences.WfoPreferences
import com.example.data.preferences.WfoSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class SyncSummary(
    val logsAdded: Int,
    val leavesAdded: Int,
    val message: String
)

class WfoRepository(
    private val context: Context,
    private val database: WfoDatabase = WfoDatabase.getInstance(context),
    private val preferences: WfoPreferences = WfoPreferences(context),
    private val gistApi: GitHubGistApi = GitHubGistApi.create()
) {
    val allLogs: Flow<List<CheckInLog>> = database.checkInLogDao().getAllLogs()
    val allLeaves: Flow<List<LeaveHoliday>> = database.leaveHolidayDao().getAllLeaves()
    val settingsFlow: Flow<WfoSettings> = preferences.settingsFlow

    suspend fun insertLog(log: CheckInLog) {
        withContext(Dispatchers.IO) {
            database.checkInLogDao().insertLog(log)
        }
    }

    suspend fun deleteLog(date: String) {
        withContext(Dispatchers.IO) {
            database.checkInLogDao().deleteByDate(date)
        }
    }

    suspend fun insertLeave(leave: LeaveHoliday) {
        withContext(Dispatchers.IO) {
            database.leaveHolidayDao().insertLeave(leave)
        }
    }

    suspend fun insertLeaveRange(
        startDateStr: String,
        endDateStr: String,
        type: String,
        title: String
    ): Int {
        return withContext(Dispatchers.IO) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val startCal = java.util.Calendar.getInstance().apply { time = sdf.parse(startDateStr) ?: Date() }
            val endCal = java.util.Calendar.getInstance().apply { time = sdf.parse(endDateStr) ?: Date() }

            if (endCal.before(startCal)) {
                val temp = startCal.time
                startCal.time = endCal.time
                endCal.time = temp
            }

            val leavesToAdd = mutableListOf<LeaveHoliday>()
            while (!startCal.after(endCal)) {
                val dayOfWeek = startCal.get(java.util.Calendar.DAY_OF_WEEK)
                // Exclude weekends (Sat & Sun) as they are already default non-working days
                if (dayOfWeek != java.util.Calendar.SATURDAY && dayOfWeek != java.util.Calendar.SUNDAY) {
                    val dateKey = sdf.format(startCal.time)
                    leavesToAdd.add(
                        LeaveHoliday(
                            id = UUID.randomUUID().toString(),
                            type = type,
                            title = title.ifBlank {
                                when (type) {
                                    "holiday" -> "Public Holiday"
                                    "sick" -> "Sick Leave"
                                    else -> "Annual Leave"
                                }
                            },
                            date = dateKey
                        )
                    )
                }
                startCal.add(java.util.Calendar.DAY_OF_MONTH, 1)
            }

            if (leavesToAdd.isNotEmpty()) {
                database.leaveHolidayDao().insertAll(leavesToAdd)
            }
            leavesToAdd.size
        }
    }

    suspend fun deleteLeave(id: String) {
        withContext(Dispatchers.IO) {
            database.leaveHolidayDao().deleteById(id)
        }
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
        preferences.updateSettings(
            minHours = minHours,
            targetDaysPerWeek = targetDaysPerWeek,
            shiftStart = shiftStart,
            lat = lat,
            lon = lon,
            radiusKm = radiusKm,
            employeeName = employeeName,
            gitToken = gitToken,
            gistId = gistId,
            notifyOnTarget = notifyOnTarget,
            gpsVerificationEnabled = gpsVerificationEnabled,
            passiveGeofenceArrivalEnabled = passiveGeofenceArrivalEnabled,
            departureReminderEnabled = departureReminderEnabled,
            departureReminderHours = departureReminderHours,
            autoMuteHolidays = autoMuteHolidays
        )
    }

    suspend fun clearAllData() {
        withContext(Dispatchers.IO) {
            database.checkInLogDao().clearAll()
            database.leaveHolidayDao().clearAll()
            preferences.setActivePunchStart(null)
            preferences.setLastDeletedUndo(null)
        }
    }

    // Punch In / Out Lifecycle
    suspend fun punchIn(startTimeMillis: Long, initialNote: String? = null) {
        preferences.setActivePunchStart(startTimeMillis)
        preferences.setPersistentUndoSummary(null)
    }

    suspend fun adjustArrivalStartTime(newStartMillis: Long) {
        preferences.setActivePunchStart(newStartMillis)
    }

    suspend fun clearActiveSessionOnly() {
        preferences.setActivePunchStart(null)
    }

    suspend fun punchOut(addTransitMins: Int = 0): CheckInLog? {
        val currentSettings = preferences.settingsFlow.first()
        val startMillis = currentSettings.activePunchStartMillis ?: return null
        val now = System.currentTimeMillis() + (addTransitMins * 60 * 1000L)

        val durationMillis = maxOf(0L, now - startMillis)
        val durationMins = (durationMillis / (1000 * 60)).toInt()

        val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        val inTime = timeFormat.format(Date(startMillis))
        val outTime = timeFormat.format(Date(now))
        val dateStr = dateFormat.format(Date(startMillis))

        val log = CheckInLog(
            date = dateStr,
            inTime = inTime,
            outTime = outTime,
            durationMinutes = durationMins,
            notes = if (addTransitMins > 0) "+${addTransitMins}m transit added" else null
        )

        withContext(Dispatchers.IO) {
            database.checkInLogDao().insertLog(log)
            preferences.setActivePunchStart(null)

            // Save undo snapshot
            val snapshotJson = JSONObject().apply {
                put("date", log.date)
                put("inTime", log.inTime)
                put("outTime", log.outTime)
                put("durationMinutes", log.durationMinutes)
                put("notes", log.notes ?: "")
                put("startMillis", startMillis)
            }.toString()
            preferences.setLastDeletedUndo(snapshotJson)
            preferences.setPersistentUndoSummary("Session logged (${log.formattedDuration} ending at ${log.formattedOutTime}). Mistake?")
        }

        return log
    }

    suspend fun undoPunchOut(): Boolean {
        val currentSettings = preferences.settingsFlow.first()
        val undoJson = currentSettings.lastDeletedUndoJson ?: return false

        return withContext(Dispatchers.IO) {
            try {
                val json = JSONObject(undoJson)
                val date = json.getString("date")
                val startMillis = json.getLong("startMillis")

                // Remove the inserted log
                database.checkInLogDao().deleteByDate(date)

                // Restore active start millis
                preferences.setActivePunchStart(startMillis)
                preferences.setLastDeletedUndo(null)
                preferences.setPersistentUndoSummary(null)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    suspend fun dismissUndo() {
        preferences.setPersistentUndoSummary(null)
        preferences.setLastDeletedUndo(null)
    }

    suspend fun setLastUsedTransitOffset(offsetMins: Int) {
        preferences.setLastUsedTransitOffset(offsetMins)
    }

    suspend fun setBatteryOptDismissed(dismissed: Boolean) {
        preferences.setBatteryOptDismissed(dismissed)
    }

    // Cloud Backup & GitHub Gist Sync
    suspend fun pushToGist(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val settings = preferences.settingsFlow.first()
            var token = settings.gitToken.trim()
            var gistId = settings.gistId.trim()

            if (gistId.contains("/")) {
                gistId = gistId.substringAfterLast("/").trim()
            }

            if (token.isEmpty() || gistId.isEmpty()) {
                return@withContext Result.failure(Exception("Please configure GitHub Token and Gist ID in Settings."))
            }

            val logs = database.checkInLogDao().getAllLogs().first()
            val leaves = database.leaveHolidayDao().getAllLeaves().first()

            // Build wfo_data.json array matching web app & android schema
            val logsArray = JSONArray()
            for (log in logs) {
                val obj = JSONObject().apply {
                    put("date", log.date)
                    put("inTime", log.inTime)
                    put("outTime", log.outTime)
                    put("durationMinutes", log.durationMinutes)
                    put("start", log.inTime) // Web app compatibility
                    put("end", log.outTime)   // Web app compatibility
                    put("totalHours", log.totalHours)
                    put("isValidWFODay", log.isValidWFODay(settings.minHours))
                    if (log.notes != null) put("notes", log.notes)
                }
                logsArray.put(obj)
            }

            // Build wfo_leaves.json array
            val leavesArray = JSONArray()
            for (leave in leaves) {
                val obj = JSONObject().apply {
                    put("id", leave.id)
                    put("type", leave.type)
                    put("title", leave.title)
                    put("desc", leave.title) // Web app compatibility
                    put("date", leave.date)
                }
                leavesArray.put(obj)
            }

            // Build sanitized wfo_settings.json (NEVER include token or gistId!)
            val settingsObj = JSONObject().apply {
                put("minHours", settings.minHours)
                put("targetDaysPerWeek", settings.targetDaysPerWeek)
                put("targetDaysPerMonth", settings.targetDaysPerMonth)
                put("shiftStart", settings.shiftStart)
                put("lat", settings.lat)
                put("lon", settings.lon)
                put("radiusKm", settings.radiusKm)
                put("employeeName", settings.employeeName)
                put("notifyOnTarget", settings.notifyOnTarget)
            }

            val filesMap = mapOf(
                "wfo_data.json" to GistFileContent(logsArray.toString(2)),
                "wfo_leaves.json" to GistFileContent(leavesArray.toString(2)),
                "wfo_settings.json" to GistFileContent(settingsObj.toString(2))
            )

            val authHeader = if (token.startsWith("token ", ignoreCase = true) || token.startsWith("Bearer ", ignoreCase = true)) {
                token
            } else {
                "token $token"
            }

            val response = gistApi.updateGist(
                gistId = gistId,
                authHeader = authHeader,
                request = GistUpdateRequest(
                    description = "WFO Tracker & Insights Cloud Backup",
                    files = filesMap
                )
            )

            if (response.isSuccessful) {
                Result.success("Backed up ${logs.size} check-ins, ${leaves.size} leaves to GitHub Gist! ☁️")
            } else {
                val code = response.code()
                val err = when (code) {
                    401 -> "GitHub Unauthorized (401): Personal Access Token is invalid or expired."
                    404 -> "Gist Not Found (404): Please check your Gist ID."
                    else -> "GitHub Sync failed (HTTP $code): ${response.message()}"
                }
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Cloud backup error: ${e.localizedMessage ?: e.message}"))
        }
    }

    suspend fun pullFromGist(): Result<SyncSummary> = withContext(Dispatchers.IO) {
        try {
            val settings = preferences.settingsFlow.first()
            val token = settings.gitToken.trim()
            var gistId = settings.gistId.trim()

            if (gistId.contains("/")) {
                gistId = gistId.substringAfterLast("/").trim()
            }

            if (token.isEmpty() || gistId.isEmpty()) {
                return@withContext Result.failure(Exception("Please configure GitHub Token and Gist ID in Settings."))
            }

            val authHeader = if (token.startsWith("token ", ignoreCase = true) || token.startsWith("Bearer ", ignoreCase = true)) {
                token
            } else {
                "token $token"
            }

            val response = gistApi.getGist(gistId = gistId, authHeader = authHeader)
            if (!response.isSuccessful) {
                val code = response.code()
                val err = when (code) {
                    401 -> "GitHub Unauthorized (401): Personal Access Token is invalid or expired."
                    404 -> "Gist Not Found (404): Please verify Gist ID."
                    else -> "Download failed (HTTP $code): ${response.message()}"
                }
                return@withContext Result.failure(Exception(err))
            }

            val gistData = response.body() ?: return@withContext Result.failure(Exception("Empty Gist response"))
            var logsMerged = 0
            var leavesMerged = 0

            // 1. Merge wfo_data.json
            gistData.files["wfo_data.json"]?.content?.let { content ->
                val logsList = parseLogsJson(content)
                val existingLogs = database.checkInLogDao().getAllLogs().first().associateBy { it.date }
                val newLogs = logsList.filter { !existingLogs.containsKey(it.date) }
                if (newLogs.isNotEmpty()) {
                    database.checkInLogDao().insertAll(newLogs)
                    logsMerged = newLogs.size
                }
            }

            // 2. Merge wfo_leaves.json
            gistData.files["wfo_leaves.json"]?.content?.let { content ->
                val leavesList = parseLeavesJson(content)
                val existingLeaves = database.leaveHolidayDao().getAllLeaves().first().associateBy { it.date }
                val newLeaves = leavesList.filter { !existingLeaves.containsKey(it.date) }
                if (newLeaves.isNotEmpty()) {
                    database.leaveHolidayDao().insertAll(newLeaves)
                    leavesMerged = newLeaves.size
                }
            }

            // 3. Merge wfo_settings.json (sanitized)
            gistData.files["wfo_settings.json"]?.content?.let { content ->
                try {
                    val obj = JSONObject(content)
                    val minHours = obj.optDouble("minHours", settings.minHours)
                    val targetDays = obj.optInt("targetDaysPerWeek", settings.targetDaysPerWeek)
                    val shiftStart = obj.optString("shiftStart", settings.shiftStart)
                    val lat = obj.optDouble("lat", settings.lat)
                    val lon = obj.optDouble("lon", settings.lon)
                    val radiusKm = obj.optDouble("radiusKm", settings.radiusKm)
                    val empName = obj.optString("employeeName", settings.employeeName)

                    preferences.updateSettings(
                        minHours = minHours,
                        targetDaysPerWeek = targetDays,
                        shiftStart = shiftStart,
                        lat = lat,
                        lon = lon,
                        radiusKm = radiusKm,
                        employeeName = empName,
                        gitToken = settings.gitToken, // preserve existing credentials
                        gistId = settings.gistId,
                        notifyOnTarget = obj.optBoolean("notifyOnTarget", settings.notifyOnTarget),
                        gpsVerificationEnabled = settings.gpsVerificationEnabled
                    )
                } catch (e: Exception) {
                    // Ignore settings parse failure
                }
            }

            Result.success(
                SyncSummary(
                    logsAdded = logsMerged,
                    leavesAdded = leavesMerged,
                    message = "Synced from Cloud! Merged $logsMerged new check-in(s) and $leavesMerged exclusion(s)."
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("Cloud pull error: ${e.localizedMessage ?: e.message}"))
        }
    }

    // JSON Import / Export for File Backups
    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val settings = preferences.settingsFlow.first()
        val logs = database.checkInLogDao().getAllLogs().first()
        val leaves = database.leaveHolidayDao().getAllLeaves().first()

        val root = JSONObject()

        val logsArr = JSONArray()
        for (log in logs) {
            val obj = JSONObject().apply {
                put("date", log.date)
                put("inTime", log.inTime)
                put("outTime", log.outTime)
                put("durationMinutes", log.durationMinutes)
                put("start", log.inTime)
                put("end", log.outTime)
                put("totalHours", log.totalHours)
                put("isValidWFODay", log.isValidWFODay(settings.minHours))
                if (log.notes != null) put("notes", log.notes)
            }
            logsArr.put(obj)
        }
        root.put("db", logsArr)

        val leavesArr = JSONArray()
        for (leave in leaves) {
            val obj = JSONObject().apply {
                put("id", leave.id)
                put("type", leave.type)
                put("title", leave.title)
                put("desc", leave.title)
                put("date", leave.date)
            }
            leavesArr.put(obj)
        }
        root.put("leaves", leavesArr)

        // Sanitized settings (never export token or gistId)
        val settingsObj = JSONObject().apply {
            put("minHours", settings.minHours)
            put("targetDaysPerWeek", settings.targetDaysPerWeek)
            put("targetDaysPerMonth", settings.targetDaysPerMonth)
            put("shiftStart", settings.shiftStart)
            put("lat", settings.lat)
            put("lon", settings.lon)
            put("radiusKm", settings.radiusKm)
            put("employeeName", settings.employeeName)
        }
        root.put("settings", settingsObj)
        root.put("exportVersion", 1)
        root.put("exportedAt", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))

        root.toString(2)
    }

    suspend fun importBackupJson(jsonString: String): Result<SyncSummary> = withContext(Dispatchers.IO) {
        try {
            var logsAdded = 0
            var leavesAdded = 0

            val trimmed = jsonString.trim()
            if (trimmed.startsWith("[")) {
                // Direct array of logs (legacy web format)
                val parsed = parseLogsJson(trimmed)
                val existing = database.checkInLogDao().getAllLogs().first().associateBy { it.date }
                val toInsert = parsed.filter { !existing.containsKey(it.date) }
                if (toInsert.isNotEmpty()) {
                    database.checkInLogDao().insertAll(toInsert)
                    logsAdded = toInsert.size
                }
            } else {
                val root = JSONObject(trimmed)
                if (root.has("db")) {
                    val logsJson = root.getJSONArray("db").toString()
                    val parsed = parseLogsJson(logsJson)
                    val existing = database.checkInLogDao().getAllLogs().first().associateBy { it.date }
                    val toInsert = parsed.filter { !existing.containsKey(it.date) }
                    if (toInsert.isNotEmpty()) {
                        database.checkInLogDao().insertAll(toInsert)
                        logsAdded = toInsert.size
                    }
                }
                if (root.has("leaves")) {
                    val leavesJson = root.getJSONArray("leaves").toString()
                    val parsed = parseLeavesJson(leavesJson)
                    val existing = database.leaveHolidayDao().getAllLeaves().first().associateBy { it.date }
                    val toInsert = parsed.filter { !existing.containsKey(it.date) }
                    if (toInsert.isNotEmpty()) {
                        database.leaveHolidayDao().insertAll(toInsert)
                        leavesAdded = toInsert.size
                    }
                }
            }

            Result.success(
                SyncSummary(
                    logsAdded = logsAdded,
                    leavesAdded = leavesAdded,
                    message = "Successfully imported $logsAdded new check-in(s) and $leavesAdded exclusion(s)!"
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("Failed to parse backup JSON: ${e.localizedMessage ?: e.message}"))
        }
    }

    private fun parseLogsJson(jsonContent: String): List<CheckInLog> {
        val list = mutableListOf<CheckInLog>()
        val arr = JSONArray(jsonContent)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val date = obj.getString("date")
            val inTime = if (obj.has("inTime")) obj.getString("inTime") else obj.optString("start", "09:00")
            val outTime = if (obj.has("outTime")) obj.getString("outTime") else obj.optString("end", "17:00")

            val durationMinutes = when {
                obj.has("durationMinutes") -> obj.getInt("durationMinutes")
                obj.has("totalHours") -> (obj.getDouble("totalHours") * 60).toInt()
                else -> {
                    // Calculate from inTime and outTime
                    val inParts = inTime.split(":")
                    val outParts = outTime.split(":")
                    val inM = inParts[0].toInt() * 60 + inParts.getOrElse(1) { "0" }.toInt()
                    val outM = outParts[0].toInt() * 60 + outParts.getOrElse(1) { "0" }.toInt()
                    var diff = outM - inM
                    if (diff <= 0) diff += 24 * 60
                    diff
                }
            }
            val notes = if (obj.has("notes") && !obj.isNull("notes")) obj.getString("notes") else null

            list.add(
                CheckInLog(
                    date = date,
                    inTime = inTime,
                    outTime = outTime,
                    durationMinutes = durationMinutes,
                    notes = notes
                )
            )
        }
        return list
    }

    private fun parseLeavesJson(jsonContent: String): List<LeaveHoliday> {
        val list = mutableListOf<LeaveHoliday>()
        val arr = JSONArray(jsonContent)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val id = if (obj.has("id")) obj.getString("id") else UUID.randomUUID().toString()
            val date = obj.getString("date")
            val type = obj.optString("type", "holiday")
            val title = if (obj.has("title")) obj.getString("title") else obj.optString("desc", "Holiday")
            list.add(LeaveHoliday(id = id, type = type, title = title, date = date))
        }
        return list
    }
}
