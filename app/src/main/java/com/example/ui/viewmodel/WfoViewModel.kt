package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.CheckInLog
import com.example.data.local.LeaveHoliday
import com.example.data.local.LeaveRangeResult
import com.example.data.preferences.WfoSettings
import com.example.data.repository.WfoRepository
import com.example.service.GeofencePeriodicWorker
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

class WfoViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: WfoRepository = WfoRepository(application)
) : AndroidViewModel(application) {

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
        val next = !_uiState.value.settings.timerCountdownMode
        _uiState.update { it.copy(settings = it.settings.copy(timerCountdownMode = next)) }
        viewModelScope.launch {
            repository.setTimerCountdownMode(next)
        }
    }

    fun setTrendMode(mode: TrendMode) {
        _uiState.update { it.copy(trendMode = mode) }
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

    fun markMonthCelebrated(monthKey: String) {
        viewModelScope.launch {
            repository.setLastCelebratedMonthKey(monthKey)
        }
    }

    fun setThemeMode(mode: com.example.data.preferences.ThemeMode) {
        viewModelScope.launch {
            repository.setThemeMode(mode)
        }
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
                            onSuccess = { "Backup complete" },
                            onFailure = { err -> "Sync notice: ${err.message}" }
                        )
                    )
                }
            } else {
                delay(600)
                _uiState.update {
                    it.copy(
                        isPullRefreshing = false,
                        snackbarMessage = "Dashboard refreshed"
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
                        "Punched in, ${offsetMinutes} min earlier"
                    } else {
                        "Punched in"
                    }
                )
            }
        }
    }

    // Punch In / Out Actions
    fun requestPunchIn(
        context: Context,
        startMillis: Long = System.currentTimeMillis(),
        initialNote: String? = null,
        bypassGeofence: Boolean = false
    ) {
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

            HapticHelper.performClick(context)
            repository.punchIn(startMillis, initialNote)
            try {
                PunchTimerService.start(context, startMillis, settings.minHours)
                com.example.widget.WfoAppWidgetProvider.updateAllWidgets(context)
            } catch (_: Exception) {
            }
            _uiState.update {
                it.copy(
                    snackbarMessage = if (!initialNote.isNullOrBlank()) "Punched in with note" else "Punched in",
                    geofenceAlertMessage = null
                )
            }
        }
    }

    fun requestPunchOut(context: Context, addTransitMinutes: Int = 0) {
        viewModelScope.launch {
            HapticHelper.performClick(context)
            val log = repository.punchOut(addTransitMinutes)
            PunchTimerService.stop(context)
            com.example.widget.WfoAppWidgetProvider.updateAllWidgets(context)
            if (log != null) {
                val current = _uiState.value
                val monthPrefix = log.date.take(7)
                val updatedLogs = current.logs.filter { it.date != log.date } + log
                val validThisMonth = updatedLogs.count {
                    it.date.startsWith(monthPrefix) && it.isValidWFODay(current.settings.minHours)
                }
                val leavesThisMonth = current.leaves.count { leave ->
                    if (!leave.date.startsWith(monthPrefix)) return@count false
                    val c = Calendar.getInstance().apply { time = DateUtils.parseDate(leave.date) }
                    val dow = c.get(Calendar.DAY_OF_WEEK)
                    dow != Calendar.SATURDAY && dow != Calendar.SUNDAY
                }
                val requiredThisMonth = maxOf(0, current.settings.targetDaysPerMonth - leavesThisMonth)
                _uiState.update {
                    it.copy(
                        undoAvailable = true,
                        undoMessage = "Session logged (${log.formattedDuration} ending at ${log.formattedOutTime}). Mistake?",
                        snackbarMessage = "${log.formattedDuration} logged. $validThisMonth of $requiredThisMonth this month."
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
                        snackbarMessage = "Punch out undone. Timer restored."
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
            _uiState.update { it.copy(snackbarMessage = "Attendance log saved for $date") }
        }
    }

    fun deleteLog(date: String) {
        viewModelScope.launch {
            repository.deleteLog(date)
            _uiState.update { it.copy(snackbarMessage = "Log deleted for $date") }
        }
    }

    // Leave & Holiday CRUD
    fun addLeaveRange(
        from: String,
        to: String,
        type: String,
        title: String,
        onResult: ((Result<LeaveRangeResult>) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val result = repository.insertLeaveRange(from, to, type, title)
            result.onSuccess { stats ->
                _uiState.update {
                    it.copy(snackbarMessage = formatLeaveRangeMessage(stats))
                }
            }
            onResult?.invoke(result)
        }
    }

    fun importHolidaysBatch(
        holidays: List<LeaveHoliday>,
        onInserted: ((List<LeaveHoliday>) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val result = repository.insertLeavesBatch(holidays)
            result.onSuccess { inserted ->
                onInserted?.invoke(inserted)
            }
        }
    }

    fun deleteLeavesBatch(ids: List<String>) {
        viewModelScope.launch {
            repository.deleteLeavesByIds(ids)
        }
    }

    private fun formatLeaveRangeMessage(stats: LeaveRangeResult): String {
        fun dayWord(n: Int) = if (n == 1) "day" else "days"
        fun weekendWord(n: Int) = if (n == 1) "weekend day" else "weekend days"
        fun dupWord(n: Int) = if (n == 1) "duplicate" else "duplicates"

        val parts = mutableListOf<String>()
        parts.add(if (stats.added > 0) "Added ${stats.added} ${dayWord(stats.added)}." else "No days added.")
        val skippedParts = mutableListOf<String>()
        if (stats.skippedWeekends > 0) {
            skippedParts.add("${stats.skippedWeekends} ${weekendWord(stats.skippedWeekends)}")
        }
        if (stats.skippedDuplicates > 0) {
            skippedParts.add("${stats.skippedDuplicates} ${dupWord(stats.skippedDuplicates)}")
        }
        if (skippedParts.isNotEmpty()) {
            parts.add("Skipped ${skippedParts.joinToString(" and ")}.")
        }
        return parts.joinToString(" ")
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
        autoMuteHolidays: Boolean = true,
        showConfirmation: Boolean = false
    ) {
        viewModelScope.launch {
            val prev = _uiState.value.settings
            val geofenceParamsChanged = prev.lat != lat ||
                prev.lon != lon ||
                prev.radiusKm != radiusKm ||
                prev.passiveGeofenceArrivalEnabled != passiveGeofenceArrivalEnabled

            val savedSecurely = repository.updateSettings(
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

            if (geofenceParamsChanged) {
                try {
                    val appContext = getApplication<Application>()
                    if (passiveGeofenceArrivalEnabled) {
                        GeofencePeriodicWorker.reschedule(appContext)
                    } else {
                        GeofencePeriodicWorker.cancel(appContext)
                    }
                } catch (_: Exception) {
                }
            }

            val nextMsg = when {
                !savedSecurely -> "Token could not be saved securely"
                showConfirmation -> "Settings updated"
                else -> null
            }
            if (nextMsg != null) {
                _uiState.update { it.copy(snackbarMessage = nextMsg) }
            }
        }
    }

    fun updateSettings(
        settings: WfoSettings,
        showConfirmation: Boolean = false
    ) {
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
            autoMuteHolidays = settings.autoMuteHolidays,
            showConfirmation = showConfirmation
        )
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            _uiState.update { it.copy(snackbarMessage = "All logs and data cleared") }
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
