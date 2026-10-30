package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.CheckInLog
import com.example.data.local.LeaveHoliday
import com.example.data.preferences.WfoSettings
import com.example.data.repository.WfoRepository
import com.example.service.PunchTimerService
import com.example.util.BatteryOptimizationHelper
import com.example.util.DateUtils
import com.example.util.HapticHelper
import com.example.util.LocationHelper
import com.example.util.PdfReportGenerator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class WfoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WfoRepository(application)
    private val _uiState = MutableStateFlow(WfoUiState())
    val uiState: StateFlow<WfoUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        val now = Calendar.getInstance()
        _uiState.update {
            it.copy(
                dashboardNavYear = now.get(Calendar.YEAR),
                dashboardNavMonth = now.get(Calendar.MONTH),
                selectedArchiveYear = now.get(Calendar.YEAR)
            )
        }

        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                repository.settingsFlow,
                repository.allLogs,
                repository.allLeaves
            ) { settings, logs, leaves ->
                Triple(settings, logs, leaves)
            }.collectLatest { (settings, logs, leaves) ->
                val isPunched = settings.activePunchStartMillis != null
                _uiState.update { current ->
                    current.copy(
                        settings = settings,
                        logs = logs,
                        leaves = leaves,
                        isPunchedIn = isPunched,
                        activeStartMillis = settings.activePunchStartMillis,
                        transitOffsetMinutes = settings.lastUsedTransitOffsetMinutes,
                        undoAvailable = settings.lastDeletedUndoJson != null && settings.persistentUndoSummary != null,
                        undoMessage = settings.persistentUndoSummary
                    )
                }

                if (isPunched) {
                    startTimerTicker(settings.activePunchStartMillis!!, settings.minHours)
                } else {
                    stopTimerTicker()
                }
            }
        }
    }

    fun checkBatteryOptimization(context: Context) {
        val isIgnored = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
        val dismissed = _uiState.value.settings.batteryOptNudgeDismissed
        _uiState.update {
            it.copy(showBatteryOptNudge = !isIgnored && !dismissed)
        }
    }

    fun dismissBatteryOptimizationNudge() {
        viewModelScope.launch {
            repository.setBatteryOptDismissed(true)
            _uiState.update { it.copy(showBatteryOptNudge = false) }
        }
    }

    private fun startTimerTicker(startMillis: Long, minHours: Double) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val elapsed = maxOf(0L, (now - startMillis) / 1000)
                val targetSec = (minHours * 3600).toLong()
                val targetMillis = startMillis + (minHours * 3600 * 1000L).toLong()
                val targetFormatted = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(targetMillis))

                _uiState.update {
                    it.copy(
                        elapsedSeconds = elapsed,
                        targetReached = elapsed >= targetSec,
                        targetTimeFormatted = targetFormatted
                    )
                }
                delay(1000L)
            }
        }
    }

    private fun stopTimerTicker() {
        timerJob?.cancel()
        timerJob = null
        _uiState.update {
            it.copy(
                elapsedSeconds = 0L,
                targetReached = false,
                targetTimeFormatted = ""
            )
        }
    }

    fun toggleCountdownMode() {
        _uiState.update { it.copy(countdownMode = !it.countdownMode) }
    }

    fun setTrendMode(mode: TrendMode) {
        _uiState.update { it.copy(trendMode = mode) }
    }

    fun setTransitOffsetMinutes(mins: Int) {
        viewModelScope.launch {
            repository.setLastUsedTransitOffset(mins)
        }
        _uiState.update { it.copy(transitOffsetMinutes = mins, customArrivedTimeInput = "") }
    }

    fun setCustomArrivedTimeInput(text: String) {
        _uiState.update { it.copy(customArrivedTimeInput = text) }
    }

    fun setDashboardMonth(offset: Int) {
        _uiState.update { current ->
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, current.dashboardNavYear)
                set(Calendar.MONTH, current.dashboardNavMonth)
                add(Calendar.MONTH, offset)
            }
            current.copy(
                dashboardNavYear = cal.get(Calendar.YEAR),
                dashboardNavMonth = cal.get(Calendar.MONTH)
            )
        }
    }

    fun setDashboardYearMonth(year: Int, month: Int) {
        _uiState.update {
            it.copy(dashboardNavYear = year, dashboardNavMonth = month)
        }
    }

    fun resetToCurrentMonth() {
        val now = Calendar.getInstance()
        _uiState.update {
            it.copy(
                dashboardNavYear = now.get(Calendar.YEAR),
                dashboardNavMonth = now.get(Calendar.MONTH)
            )
        }
    }

    fun selectCalendarDate(dateKey: String) {
        _uiState.update { it.copy(selectedCalendarDateKey = dateKey) }
    }

    fun selectArchiveYear(year: Int) {
        _uiState.update { it.copy(selectedArchiveYear = year) }
    }

    fun setArchiveYear(year: Int) {
        selectArchiveYear(year)
    }

    fun selectArchiveMonth(month: Int) {
        _uiState.update { it.copy(selectedArchiveMonth = month) }
    }

    fun setArchiveYearMonth(year: Int, month: Int) {
        _uiState.update { it.copy(selectedArchiveYear = year, selectedArchiveMonth = month) }
    }

    fun setArchiveMonth(delta: Int) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, _uiState.value.selectedArchiveYear)
            set(Calendar.MONTH, _uiState.value.selectedArchiveMonth)
            add(Calendar.MONTH, delta)
        }
        _uiState.update {
            it.copy(
                selectedArchiveYear = cal.get(Calendar.YEAR),
                selectedArchiveMonth = cal.get(Calendar.MONTH)
            )
        }
    }

    fun resetArchiveToToday() {
        val cal = Calendar.getInstance()
        _uiState.update {
            it.copy(
                selectedArchiveYear = cal.get(Calendar.YEAR),
                selectedArchiveMonth = cal.get(Calendar.MONTH),
                selectedCalendarDateKey = DateUtils.todayDateStr()
            )
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun clearGeofenceAlert() {
        _uiState.update { it.copy(geofenceAlertMessage = null) }
    }

    fun dismissGeofenceAlert() = clearGeofenceAlert()

    fun openArrivalPrompt() {
        _uiState.update { it.copy(showArrivalPrompt = true) }
    }

    fun dismissArrivalPrompt() {
        _uiState.update { it.copy(showArrivalPrompt = false) }
    }

    fun openDeparturePrompt() {
        _uiState.update { it.copy(showDeparturePrompt = true) }
    }

    fun dismissDeparturePrompt() {
        _uiState.update { it.copy(showDeparturePrompt = false) }
    }

    fun dismissUndo() {
        viewModelScope.launch {
            repository.dismissUndo()
            _uiState.update { it.copy(undoAvailable = false, undoMessage = null) }
        }
    }

    // Modal dialog triggers
    fun openStreakBreakdown(show: Boolean) {
        _uiState.update { it.copy(showStreakBreakdownDialog = show) }
    }

    fun openTimerQuickActionMenu(show: Boolean) {
        _uiState.update { it.copy(showTimerQuickActionMenu = show) }
    }

    fun openPunchInWithNoteDialog(show: Boolean) {
        _uiState.update { it.copy(showPunchInWithNoteDialog = show) }
    }

    fun openForgotPunchInDialog(show: Boolean) {
        _uiState.update { it.copy(showForgotPunchInDialog = show) }
    }

    fun openYearOverYearDialog(show: Boolean) {
        _uiState.update { it.copy(showYearOverYearDialog = show) }
    }

    // Pull to Refresh
    fun pullToRefresh(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPullRefreshing = true) }
            val settings = _uiState.value.settings
            if (settings.gitToken.isNotEmpty() && settings.gistId.isNotEmpty()) {
                val res = repository.pushToGist()
                _uiState.update {
                    it.copy(
                        isPullRefreshing = false,
                        snackbarMessage = res.fold(
                            onSuccess = { "Cloud Sync complete! Gist updated. ☁️" },
                            onFailure = { err -> "Sync notice: ${err.message}" }
                        )
                    )
                }
            } else {
                delay(600)
                _uiState.update {
                    it.copy(
                        isPullRefreshing = false,
                        snackbarMessage = "Dashboard refreshed! Local logs up to date."
                    )
                }
            }
        }
    }

    fun confirmArrivalPunchIn(context: Context, offsetMinutes: Int) {
        viewModelScope.launch {
            HapticHelper.performClick(context)
            val settings = _uiState.value.settings
            val startMillis = System.currentTimeMillis() - (offsetMinutes * 60 * 1000L)
            repository.punchIn(startMillis)
            PunchTimerService.start(context, startMillis, settings.minHours)
            com.example.widget.WfoAppWidgetProvider.updateAllWidgets(context)
            _uiState.update {
                it.copy(
                    showArrivalPrompt = false,
                    snackbarMessage = if (offsetMinutes > 0) {
                        "Punched in with +${offsetMinutes}m transit offset applied! 🏢"
                    } else {
                        "Punched in now! 🏢"
                    }
                )
            }
        }
    }

    // Punch In / Out Actions
    fun requestPunchIn(context: Context, bypassGeofence: Boolean = false, initialNote: String? = null) {
        viewModelScope.launch {
            val settings = _uiState.value.settings

            // Optional GPS Geofence Check
            if (settings.gpsVerificationEnabled && !bypassGeofence) {
                val geofenceStatus = LocationHelper.checkGeofence(
                    context = context,
                    officeLat = settings.lat,
                    officeLon = settings.lon,
                    radiusKm = settings.radiusKm
                )
                if (geofenceStatus != null && !geofenceStatus.isWithinRadius) {
                    HapticHelper.performWarning(context)
                    _uiState.update {
                        it.copy(geofenceAlertMessage = geofenceStatus.message)
                    }
                    return@launch
                }
            }

            // Calculate start time with offset or custom time
            val customInput = _uiState.value.customArrivedTimeInput.trim()
            val startMillis = if (customInput.isNotEmpty()) {
                parseCustomArrivedTime(customInput) ?: calculateOffsetMillis()
            } else {
                calculateOffsetMillis()
            }

            HapticHelper.performClick(context)
            repository.punchIn(startMillis, initialNote)
            PunchTimerService.start(context, startMillis, settings.minHours)
            com.example.widget.WfoAppWidgetProvider.updateAllWidgets(context)
            _uiState.update {
                it.copy(
                    snackbarMessage = if (!initialNote.isNullOrBlank()) "Punched In! Note: \"$initialNote\" 🏢" else "Punched In Successfully! 🏢",
                    geofenceAlertMessage = null,
                    showPunchInWithNoteDialog = false
                )
            }
        }
    }

    /**
     * "Forgot to punch in" quick recovery: sets today's arrival time to specific hour/minute
     */
    fun backfillPunchInToday(context: Context, hourOfDay: Int, minute: Int) {
        viewModelScope.launch {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hourOfDay)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startMillis = cal.timeInMillis
            val settings = _uiState.value.settings

            HapticHelper.performClick(context)
            repository.punchIn(startMillis)
            PunchTimerService.start(context, startMillis, settings.minHours)
            com.example.widget.WfoAppWidgetProvider.updateAllWidgets(context)

            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            _uiState.update {
                it.copy(
                    showForgotPunchInDialog = false,
                    snackbarMessage = "Punched in backdated to ${timeFormat.format(Date(startMillis))}! ⏱️"
                )
            }
        }
    }

    /**
     * Quick adjust arrival time for currently active live session
     */
    fun adjustActiveSessionStart(context: Context, hourOfDay: Int, minute: Int) {
        viewModelScope.launch {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hourOfDay)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val newStart = cal.timeInMillis
            val settings = _uiState.value.settings

            repository.adjustArrivalStartTime(newStart)
            PunchTimerService.start(context, newStart, settings.minHours)
            com.example.widget.WfoAppWidgetProvider.updateAllWidgets(context)

            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            _uiState.update {
                it.copy(
                    showTimerQuickActionMenu = false,
                    snackbarMessage = "Arrival time adjusted to ${timeFormat.format(Date(newStart))}! ⏱️"
                )
            }
        }
    }

    /**
     * Switch today from active office session to Approved WFH
     */
    fun markTodayAsWfh(context: Context) {
        viewModelScope.launch {
            PunchTimerService.stop(context)
            repository.clearActiveSessionOnly()
            com.example.widget.WfoAppWidgetProvider.updateAllWidgets(context)

            val today = DateUtils.todayDateStr()
            repository.insertLeaveRange(today, today, "wfh", "Approved WFH")
            _uiState.update {
                it.copy(
                    showTimerQuickActionMenu = false,
                    snackbarMessage = "Today registered as Approved WFH! 🏠"
                )
            }
        }
    }

    private fun calculateOffsetMillis(): Long {
        val offsetMins = _uiState.value.transitOffsetMinutes
        return System.currentTimeMillis() - (offsetMins * 60 * 1000L)
    }

    private fun parseCustomArrivedTime(input: String): Long? {
        val clean = input.trim().uppercase()
        val regex = Regex("^(\\d{1,2}):(\\d{2})\\s*(AM|PM)?$")
        val match = regex.find(clean) ?: return null

        var h = match.groupValues[1].toIntOrNull() ?: return null
        val m = match.groupValues[2].toIntOrNull() ?: return null
        val ampm = match.groupValues.getOrNull(3)

        if (ampm == "PM" && h < 12) h += 12
        if (ampm == "AM" && h == 12) h = 0

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun requestPunchOut(context: Context, addTransitMinutes: Int = 0) {
        viewModelScope.launch {
            HapticHelper.performClick(context)
            val log = repository.punchOut(addTransitMinutes)
            PunchTimerService.stop(context)
            com.example.widget.WfoAppWidgetProvider.updateAllWidgets(context)
            if (log != null) {
                _uiState.update {
                    it.copy(
                        undoAvailable = true,
                        undoMessage = "Session logged (${log.formattedDuration} ending at ${log.formattedOutTime}). Mistake?",
                        snackbarMessage = "Punched Out! Logged ${log.formattedDuration} total."
                    )
                }
            }
        }
    }

    fun executeUndoPunchOut(context: Context) {
        viewModelScope.launch {
            HapticHelper.performClick(context)
            val restored = repository.undoPunchOut()
            if (restored) {
                val startMillis = _uiState.value.settings.activePunchStartMillis ?: System.currentTimeMillis()
                PunchTimerService.start(context, startMillis, _uiState.value.settings.minHours)
                com.example.widget.WfoAppWidgetProvider.updateAllWidgets(context)
                _uiState.update {
                    it.copy(
                        undoAvailable = false,
                        undoMessage = null,
                        snackbarMessage = "Punch out undone! Live timer restored. ⏱️"
                    )
                }
            }
        }
    }

    // Manual Log CRUD
    fun saveManualLog(date: String, inTime: String, outTime: String, notes: String? = null) {
        viewModelScope.launch {
            val inParts = inTime.split(":")
            val outParts = outTime.split(":")
            val inM = (inParts.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (inParts.getOrNull(1)?.toIntOrNull() ?: 0)
            val outM = (outParts.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (outParts.getOrNull(1)?.toIntOrNull() ?: 0)
            var diffM = outM - inM
            if (diffM <= 0) diffM += 24 * 60

            val log = CheckInLog(
                date = date,
                inTime = inTime,
                outTime = outTime,
                durationMinutes = diffM,
                notes = notes
            )
            repository.insertLog(log)
            _uiState.update { it.copy(snackbarMessage = "Attendance log saved for $date!") }
        }
    }

    fun deleteLog(date: String) {
        viewModelScope.launch {
            repository.deleteLog(date)
            _uiState.update { it.copy(snackbarMessage = "Log deleted for $date") }
        }
    }

    // Leave & Holiday CRUD
    fun addLeaveRange(from: String, to: String, type: String, title: String) {
        viewModelScope.launch {
            val count = repository.insertLeaveRange(from, to, type, title)
            _uiState.update {
                it.copy(snackbarMessage = "Saved $count day(s) of ${title.ifBlank { type }}!")
            }
        }
    }

    fun deleteLeave(id: String) {
        viewModelScope.launch {
            repository.deleteLeave(id)
            _uiState.update { it.copy(snackbarMessage = "Leave exclusion removed") }
        }
    }

    // Settings Updates
    fun updateSettings(
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
        viewModelScope.launch {
            repository.updateSettings(
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
            _uiState.update { it.copy(snackbarMessage = "Settings updated successfully!") }
        }
    }

    fun updateSettings(settings: com.example.data.preferences.WfoSettings) {
        updateSettings(
            minHours = settings.minHours,
            targetDaysPerWeek = settings.targetDaysPerWeek,
            shiftStart = settings.shiftStart,
            lat = settings.lat,
            lon = settings.lon,
            radiusKm = settings.radiusKm,
            employeeName = settings.employeeName,
            gitToken = settings.gitToken,
            gistId = settings.gistId,
            notifyOnTarget = settings.notifyOnTarget,
            gpsVerificationEnabled = settings.gpsVerificationEnabled,
            passiveGeofenceArrivalEnabled = settings.passiveGeofenceArrivalEnabled,
            departureReminderEnabled = settings.departureReminderEnabled,
            departureReminderHours = settings.departureReminderHours,
            autoMuteHolidays = settings.autoMuteHolidays
        )
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            _uiState.update { it.copy(snackbarMessage = "All logs and data have been wiped.") }
        }
    }

    // GitHub Gist Push & Pull
    fun pushToGist() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncingCloud = true) }
            val result = repository.pushToGist()
            _uiState.update {
                it.copy(
                    isSyncingCloud = false,
                    snackbarMessage = result.fold(
                        onSuccess = { summary -> summary },
                        onFailure = { err -> err.message ?: "Cloud sync failed" }
                    )
                )
            }
        }
    }

    fun pushToCloud() = pushToGist()

    fun pullFromGist() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncingCloud = true) }
            val result = repository.pullFromGist()
            _uiState.update {
                it.copy(
                    isSyncingCloud = false,
                    snackbarMessage = result.fold(
                        onSuccess = { summary -> summary.message },
                        onFailure = { err -> err.message ?: "Cloud restore failed" }
                    )
                )
            }
        }
    }

    fun pullFromCloud() = pullFromGist()

    // JSON SAF Export / Import
    suspend fun exportJsonString(): String {
        return repository.exportBackupJson()
    }

    fun importJsonString(jsonContent: String) {
        viewModelScope.launch {
            val result = repository.importBackupJson(jsonContent)
            _uiState.update {
                it.copy(
                    snackbarMessage = result.fold(
                        onSuccess = { summary -> summary.message },
                        onFailure = { err -> err.message ?: "Import failed" }
                    )
                )
            }
        }
    }

    // PDF Report Generator
    fun generatePdfReport(context: Context, year: Int, month: Int): File {
        val state = _uiState.value
        return PdfReportGenerator.generateMonthlyAuditReport(
            context = context,
            year = year,
            month = month,
            logs = state.logs,
            leaves = state.leaves,
            settings = state.settings
        )
    }
}
