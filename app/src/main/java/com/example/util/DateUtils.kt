package com.example.util

import com.example.data.local.CheckInLog
import com.example.data.local.LeaveHoliday
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val timeFormat24 = SimpleDateFormat("HH:mm", Locale.US)
    private val timeFormat12 = SimpleDateFormat("hh:mm a", Locale.US)

    fun todayDateStr(): String = dateFormat.format(Date())

    fun parseDate(dateStr: String): Date {
        return try {
            dateFormat.parse(dateStr) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }

    fun formatDisplayDate(dateStr: String): String {
        return try {
            val date = parseDate(dateStr)
            SimpleDateFormat("EEE, MMM d, yyyy", Locale.US).format(date)
        } catch (e: Exception) {
            dateStr
        }
    }

    fun formatShortDate(dateStr: String): String {
        return try {
            val date = parseDate(dateStr)
            SimpleDateFormat("MMM d", Locale.US).format(date)
        } catch (e: Exception) {
            dateStr
        }
    }

    fun formatTime12(time24: String): String {
        return try {
            val date = timeFormat24.parse(time24) ?: return time24
            timeFormat12.format(date)
        } catch (e: Exception) {
            time24
        }
    }

    fun formatHoursCompact(decimalHours: Double): String {
        val totalMinutes = (decimalHours * 60).toInt()
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        return when {
            h == 0 -> "${m}m"
            m == 0 -> "${h}h"
            else -> "${h}h ${m}m"
        }
    }

    fun getValidDaysThisWeek(logs: List<CheckInLog>, minHours: Double): Int {
        val now = Calendar.getInstance()
        val startOfWeek = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endOfWeek = (startOfWeek.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 6)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }

        return logs.count { log ->
            val logDate = parseDate(log.date)
            logDate >= startOfWeek.time && logDate <= endOfWeek.time && log.isValidWFODay(minHours)
        }
    }

    fun calculateWeeklyStreak(logs: List<CheckInLog>, targetDaysPerWeek: Int, minHours: Double): Int {
        if (logs.isEmpty()) return 0
        var streak = 0
        val cal = Calendar.getInstance()

        for (weekOffset in 0..52) {
            val checkWeek = (cal.clone() as Calendar).apply {
                add(Calendar.WEEK_OF_YEAR, -weekOffset)
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val checkWeekEnd = (checkWeek.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, 6)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }

            val validDaysCount = logs.count { log ->
                val d = parseDate(log.date)
                d >= checkWeek.time && d <= checkWeekEnd.time && log.isValidWFODay(minHours)
            }

            if (weekOffset == 0) {
                // Current week in progress
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                val daysElapsed = if (dayOfWeek == Calendar.SUNDAY) 7 else dayOfWeek - 1
                val daysRemainingInWeek = 7 - daysElapsed
                if (validDaysCount >= targetDaysPerWeek) {
                    streak++
                } else if (validDaysCount + daysRemainingInWeek < targetDaysPerWeek) {
                    return 0
                }
            } else {
                if (validDaysCount >= targetDaysPerWeek) {
                    streak++
                } else {
                    break
                }
            }
        }
        return streak
    }

    data class ArrivalBin(
        val label: String,
        val minHour: Double,
        val maxHour: Double,
        val count: Int
    )

    fun calculateArrivalHistogram(logs: List<CheckInLog>, shiftStart: String): List<ArrivalBin> {
        val parts = shiftStart.split(":")
        val shiftH = parts.getOrNull(0)?.toIntOrNull() ?: 12
        val shiftM = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val shiftDecimal = shiftH + (shiftM / 60.0)

        fun fmtTime(decimal: Double): String {
            val totalM = (decimal * 60).toInt().coerceIn(0, 1439)
            val h = totalM / 60
            val m = totalM % 60
            val ampm = if (h >= 12) "PM" else "AM"
            val dispH = if (h % 12 == 0) 12 else h % 12
            return String.format(Locale.US, "%d:%02d %s", dispH, m, ampm)
        }

        val rawBins = listOf(
            Pair(0.0, shiftDecimal - 0.5),
            Pair(shiftDecimal - 0.5, shiftDecimal),
            Pair(shiftDecimal, shiftDecimal + 0.5),
            Pair(shiftDecimal + 0.5, shiftDecimal + 1.0),
            Pair(shiftDecimal + 1.0, shiftDecimal + 1.5),
            Pair(shiftDecimal + 1.5, 24.0)
        )

        val bins = rawBins.mapIndexed { index, (min, max) ->
            val label = when (index) {
                0 -> "< ${fmtTime(max)}"
                rawBins.lastIndex -> "> ${fmtTime(min)}"
                else -> "${fmtTime(min)}-${fmtTime(max)}"
            }

            val count = logs.count { log ->
                val inParts = log.inTime.split(":")
                val h = inParts.getOrNull(0)?.toIntOrNull() ?: 0
                val m = inParts.getOrNull(1)?.toIntOrNull() ?: 0
                val dec = h + (m / 60.0)
                dec >= min && dec < max
            }
            ArrivalBin(label = label, minHour = min, maxHour = max, count = count)
        }

        return bins
    }

    data class TrendPoint(
        val label: String,
        val actualHours: Double,
        val targetHours: Double,
        val daysCount: Int
    )

    fun calculateWeeklyTrends(logs: List<CheckInLog>, targetDaysPerWeek: Int, minHours: Double): List<TrendPoint> {
        val points = mutableListOf<TrendPoint>()
        val targetGoal = targetDaysPerWeek * minHours

        for (i in 5 downTo 0) {
            val cal = Calendar.getInstance().apply {
                add(Calendar.WEEK_OF_YEAR, -i)
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            val calEnd = (cal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, 6)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }

            val weekLogs = logs.filter {
                val d = parseDate(it.date)
                d >= cal.time && d <= calEnd.time
            }
            val totalH = weekLogs.sumOf { it.totalHours }
            val label = SimpleDateFormat("MMM d", Locale.US).format(cal.time)

            points.add(
                TrendPoint(
                    label = label,
                    actualHours = totalH,
                    targetHours = targetGoal,
                    daysCount = weekLogs.count { it.isValidWFODay(minHours) }
                )
            )
        }
        return points
    }
}
