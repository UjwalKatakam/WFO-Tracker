package com.example.ui.components

import com.example.data.local.CheckInLog
import java.util.Calendar

data class DashboardHeroMessage(
    val text: String,
    val isPaceWarning: Boolean
)

enum class GreetingTimeOfDay {
    MORNING,
    AFTERNOON,
    EVENING
}

object DashboardCalculations {

    fun timeOfDay(hourOfDay: Int): GreetingTimeOfDay {
        return when (hourOfDay) {
            in 5..11 -> GreetingTimeOfDay.MORNING
            in 12..16 -> GreetingTimeOfDay.AFTERNOON
            else -> GreetingTimeOfDay.EVENING
        }
    }

    fun greetingParts(employeeName: String, hourOfDay: Int): Pair<String, String?> {
        val salutation = when (timeOfDay(hourOfDay)) {
            GreetingTimeOfDay.MORNING -> "Good morning"
            GreetingTimeOfDay.AFTERNOON -> "Good afternoon"
            GreetingTimeOfDay.EVENING -> "Good evening"
        }
        val trimmed = employeeName.trim()
        val firstName = if (trimmed.isEmpty() || trimmed.equals("Employee", ignoreCase = true)) {
            null
        } else {
            trimmed.split(Regex("\\s+")).firstOrNull()?.takeIf { it.isNotEmpty() }
        }
        return if (firstName == null) {
            salutation to null
        } else {
            "$salutation," to firstName
        }
    }

    fun computeHeroMessage(
        monthName: String,
        validDays: Int,
        requiredDays: Int,
        targetDaysPerWeek: Int,
        navYear: Int,
        navMonth: Int,
        logs: List<CheckInLog>,
        minHours: Double,
        nowCalendar: Calendar = Calendar.getInstance()
    ): DashboardHeroMessage {
        if (requiredDays <= 0 || validDays >= requiredDays) {
            return DashboardHeroMessage(
                text = "Quota met for $monthName.",
                isPaceWarning = false
            )
        }

        val remainingDays = requiredDays - validDays
        val remPhrase = if (remainingDays == 1) "1 day" else "$remainingDays days"

        val todayYear = nowCalendar.get(Calendar.YEAR)
        val todayMonth = nowCalendar.get(Calendar.MONTH)
        val todayDom = nowCalendar.get(Calendar.DAY_OF_MONTH)
        val todayStr = String.format(
            java.util.Locale.US,
            "%04d-%02d-%02d",
            todayYear,
            todayMonth + 1,
            todayDom
        )
        val todayLoggedValid = logs.any { it.date == todayStr && it.isValidWFODay(minHours) }

        val remainingWeekdays = countRemainingWeekdays(
            navYear = navYear,
            navMonth = navMonth,
            todayYear = todayYear,
            todayMonth = todayMonth,
            todayDom = todayDom,
            excludeToday = todayLoggedValid
        )

        if (remainingDays > remainingWeekdays) {
            val workingDayWord = if (remainingWeekdays == 1) "working day" else "working days"
            return DashboardHeroMessage(
                text = "$remainingWeekdays $workingDayWord left this month.",
                isPaceWarning = false
            )
        }

        val remainingWeeks = if (remainingWeekdays > 0) (remainingWeekdays + 4) / 5 else 0
        val policyCapacity = remainingWeeks * maxOf(1, targetDaysPerWeek)
        val underPaceButPossible = remainingWeeks == 0 || remainingDays > policyCapacity

        if (underPaceButPossible) {
            val text = if (remainingWeeks > 0) {
                val weekPhrase = if (remainingWeeks == 1) "1 week" else "$remainingWeeks weeks"
                "$remPhrase in $weekPhrase. Every office day counts."
            } else {
                "$remPhrase left this month. Every office day counts."
            }
            return DashboardHeroMessage(
                text = text,
                isPaceWarning = true
            )
        }

        val pct = (validDays * 100) / requiredDays
        val text = when {
            validDays == 0 -> "Fresh month. About $targetDaysPerWeek a week gets you there."
            pct < 25 -> "Good start. $remPhrase to go, about $targetDaysPerWeek a week."
            pct < 50 -> "Building up. $remPhrase to go."
            pct < 75 -> "Halfway there. $remPhrase to go."
            else -> "Almost there. $remPhrase to go."
        }
        return DashboardHeroMessage(
            text = text,
            isPaceWarning = false
        )
    }

    private fun countRemainingWeekdays(
        navYear: Int,
        navMonth: Int,
        todayYear: Int,
        todayMonth: Int,
        todayDom: Int,
        excludeToday: Boolean
    ): Int {
        val navYm = navYear * 12 + navMonth
        val todayYm = todayYear * 12 + todayMonth
        if (navYm < todayYm) return 0

        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, navYear)
            set(Calendar.MONTH, navMonth)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val startDay = when {
            navYm > todayYm -> 1
            excludeToday -> todayDom + 1
            else -> todayDom
        }
        if (startDay > maxDay) return 0

        var count = 0
        for (day in startDay..maxDay) {
            cal.set(Calendar.DAY_OF_MONTH, day)
            val dow = cal.get(Calendar.DAY_OF_WEEK)
            if (dow != Calendar.SATURDAY && dow != Calendar.SUNDAY) {
                count++
            }
        }
        return count
    }
}
