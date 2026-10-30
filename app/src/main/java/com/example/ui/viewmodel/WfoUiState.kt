package com.example.ui.viewmodel

import com.example.data.local.CheckInLog
import com.example.data.local.LeaveHoliday
import com.example.data.preferences.WfoSettings
import com.example.util.DateUtils
import java.util.Calendar

data class DayStreakDetail(
    val dayName: String,
    val dateKey: String,
    val hours: Double,
    val isValidWfo: Boolean,
    val isLeave: Boolean,
    val leaveTitle: String?,
    val isFutureOrRest: Boolean
)

data class YearComparisonStats(
    val year: Int,
    val totalWfoDays: Int,
    val totalHours: Double,
    val avgDailyHours: Double,
    val totalLeaves: Int,
    val compliancePct: Int
)

data class WfoUiState(
    val settings: WfoSettings = WfoSettings(),
    val logs: List<CheckInLog> = emptyList(),
    val leaves: List<LeaveHoliday> = emptyList(),
    val isPunchedIn: Boolean = false,
    val activeStartMillis: Long? = null,
    val elapsedSeconds: Long = 0L,
    val countdownMode: Boolean = false,
    val targetReached: Boolean = false,
    val targetTimeFormatted: String = "",
    val transitOffsetMinutes: Int = 15,
    val customArrivedTimeInput: String = "",
    val selectedCalendarDateKey: String = DateUtils.todayDateStr(),
    val selectedArchiveYear: Int = 2026,
    val selectedArchiveMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    val dashboardNavYear: Int = 2026,
    val dashboardNavMonth: Int = 8, // 0-indexed
    val trendMode: TrendMode = TrendMode.WEEKLY,
    val isSyncingCloud: Boolean = false,
    val isPullRefreshing: Boolean = false,
    val undoAvailable: Boolean = false,
    val undoMessage: String? = null,
    val snackbarMessage: String? = null,
    val geofenceAlertMessage: String? = null,
    val showArrivalPrompt: Boolean = false,
    val showDeparturePrompt: Boolean = false,
    val showBatteryOptNudge: Boolean = false,
    val showStreakBreakdownDialog: Boolean = false,
    val showTimerQuickActionMenu: Boolean = false,
    val showPunchInWithNoteDialog: Boolean = false,
    val showForgotPunchInDialog: Boolean = false,
    val showYearOverYearDialog: Boolean = false
) {
    val currentMonthLogs: List<CheckInLog>
        get() = logs.filter {
            val d = DateUtils.parseDate(it.date)
            val c = Calendar.getInstance().apply { time = d }
            c.get(Calendar.YEAR) == dashboardNavYear &&
                    c.get(Calendar.MONTH) == dashboardNavMonth
        }

    val currentMonthLeaves: List<LeaveHoliday>
        get() = leaves.filter {
            val d = DateUtils.parseDate(it.date)
            val c = Calendar.getInstance().apply { time = d }
            val dow = c.get(Calendar.DAY_OF_WEEK)
            c.get(Calendar.YEAR) == dashboardNavYear &&
                    c.get(Calendar.MONTH) == dashboardNavMonth &&
                    dow != Calendar.SATURDAY && dow != Calendar.SUNDAY
        }

    val validDaysThisMonth: Int
        get() = currentMonthLogs.count { it.isValidWFODay(settings.minHours) }

    val totalHoursThisMonth: Double
        get() = currentMonthLogs.sumOf { it.totalHours }

    val requiredDaysThisMonth: Int
        get() = maxOf(0, settings.targetDaysPerMonth - currentMonthLeaves.size)

    val monthlyCompliancePercentage: Int
        get() {
            if (requiredDaysThisMonth <= 0) return 100
            return ((validDaysThisMonth.toDouble() / requiredDaysThisMonth) * 100).toInt().coerceIn(0, 100)
        }

    val validDaysThisWeek: Int
        get() = DateUtils.getValidDaysThisWeek(logs, settings.minHours)

    val weeklyStreak: Int
        get() = DateUtils.calculateWeeklyStreak(logs, settings.targetDaysPerWeek, settings.minHours)

    val selectedDateLog: CheckInLog?
        get() = logs.find { it.date == selectedCalendarDateKey }

    val selectedDateLeave: LeaveHoliday?
        get() = leaves.find { it.date == selectedCalendarDateKey }

    val arrivalBins: List<DateUtils.ArrivalBin>
        get() = DateUtils.calculateArrivalHistogram(logs, settings.shiftStart)

    val weeklyTrends: List<DateUtils.TrendPoint>
        get() = DateUtils.calculateWeeklyTrends(logs, settings.targetDaysPerWeek, settings.minHours)

    val progressToDailyQuota: Float
        get() {
            val targetSec = (settings.minHours * 3600).toFloat()
            if (targetSec <= 0f) return 0f
            return (elapsedSeconds.toFloat() / targetSec).coerceIn(0f, 1f)
        }

    /**
     * Detailed breakdown for each day of the current week (Monday through Sunday)
     */
    val currentWeekBreakdown: List<DayStreakDetail>
        get() {
            val details = mutableListOf<DayStreakDetail>()
            val now = Calendar.getInstance()
            val cal = Calendar.getInstance().apply {
                firstDayOfWeek = Calendar.MONDAY
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            }
            val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

            for (i in 0 until 7) {
                val dateStr = String.format(
                    java.util.Locale.US,
                    "%04d-%02d-%02d",
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH) + 1,
                    cal.get(Calendar.DAY_OF_MONTH)
                )
                val log = logs.find { it.date == dateStr }
                val leave = leaves.find { it.date == dateStr }
                val isFuture = cal.after(now) && !dateStr.equals(DateUtils.todayDateStr())
                val isWeekend = (i >= 5)

                details.add(
                    DayStreakDetail(
                        dayName = dayNames[i],
                        dateKey = dateStr,
                        hours = log?.totalHours ?: 0.0,
                        isValidWfo = log?.isValidWFODay(settings.minHours) == true,
                        isLeave = leave != null,
                        leaveTitle = leave?.title,
                        isFutureOrRest = isFuture || (isWeekend && log == null)
                    )
                )
                cal.add(Calendar.DAY_OF_MONTH, 1)
            }
            return details
        }

    /**
     * Year-over-Year comparison stats for selected year vs prior year
     */
    fun calculateYearStats(year: Int): YearComparisonStats {
        val yearPrefix = "$year"
        val yearLogs = logs.filter { it.date.startsWith(yearPrefix) }
        val yearLeaves = leaves.filter { it.date.startsWith(yearPrefix) }
        val wfoDays = yearLogs.count { it.isValidWFODay(settings.minHours) }
        val totalH = yearLogs.sumOf { it.totalHours }
        val avgH = if (yearLogs.isNotEmpty()) totalH / yearLogs.size else 0.0
        val targetAnnual = settings.targetDaysPerWeek * 48 // approx 48 working weeks
        val compliance = if (targetAnnual > 0) ((wfoDays.toDouble() / targetAnnual) * 100).toInt().coerceIn(0, 100) else 0

        return YearComparisonStats(
            year = year,
            totalWfoDays = wfoDays,
            totalHours = totalH,
            avgDailyHours = avgH,
            totalLeaves = yearLeaves.size,
            compliancePct = compliance
        )
    }
}

enum class TrendMode {
    WEEKLY, MONTHLY
}
