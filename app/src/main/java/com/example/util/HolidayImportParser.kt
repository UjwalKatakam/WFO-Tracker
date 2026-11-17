package com.example.util

import com.example.data.local.LeaveDefaults
import com.example.data.local.LeaveHoliday
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class HolidayImportCandidate(
    val date: String,
    val title: String,
    val isWeekend: Boolean,
    val isDuplicate: Boolean,
    val isOutsideCurrentYear: Boolean,
    val selectedByDefault: Boolean
)

data class HolidayImportParseResult(
    val candidates: List<HolidayImportCandidate>,
    val skippedRecurringCount: Int = 0,
    val unparsedLinesCount: Int = 0
)

object HolidayImportParser {

    fun parseInput(
        rawContent: String,
        existingLeaves: List<LeaveHoliday>,
        currentYear: Int = Calendar.getInstance().get(Calendar.YEAR)
    ): HolidayImportParseResult {
        if (rawContent.isBlank()) {
            return HolidayImportParseResult(emptyList())
        }

        val existingDates = existingLeaves.map { it.date }.toSet()
        val looksLikeIcs = rawContent.contains("BEGIN:VCALENDAR", ignoreCase = true) ||
            rawContent.contains("BEGIN:VEVENT", ignoreCase = true)

        val rawPairs: List<Pair<String, String>>
        var skippedRecurring = 0
        var unparsedLines = 0

        if (looksLikeIcs) {
            val icsResult = parseIcs(rawContent)
            rawPairs = icsResult.dates
            skippedRecurring = icsResult.skippedRecurring
        } else {
            val textResult = parsePlainLines(rawContent)
            rawPairs = textResult.dates
            unparsedLines = textResult.unparsedLines
        }

        val dedupedByDate = LinkedHashMap<String, String>()
        for ((dateStr, title) in rawPairs) {
            val cleanTitle = title.trim().ifBlank { LeaveDefaults.defaultTitleFor(LeaveDefaults.TYPE_HOLIDAY) }
            val existingTitle = dedupedByDate[dateStr]
            if (existingTitle == null || cleanTitle.length > existingTitle.length) {
                dedupedByDate[dateStr] = cleanTitle
            }
        }

        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
        val candidates = dedupedByDate.entries
            .sortedBy { it.key }
            .map { (dateStr, title) ->
                val cal = Calendar.getInstance().apply {
                    time = dateFmt.parse(dateStr) ?: Date()
                }
                val dow = cal.get(Calendar.DAY_OF_WEEK)
                val isWeekend = dow == Calendar.SATURDAY || dow == Calendar.SUNDAY
                val year = cal.get(Calendar.YEAR)
                val isDuplicate = dateStr in existingDates
                val isOutsideYear = year != currentYear
                HolidayImportCandidate(
                    date = dateStr,
                    title = title,
                    isWeekend = isWeekend,
                    isDuplicate = isDuplicate,
                    isOutsideCurrentYear = isOutsideYear,
                    selectedByDefault = !isWeekend && !isDuplicate
                )
            }

        return HolidayImportParseResult(
            candidates = candidates,
            skippedRecurringCount = skippedRecurring,
            unparsedLinesCount = unparsedLines
        )
    }

    private data class InternalParseOutput(
        val dates: List<Pair<String, String>>,
        val skippedRecurring: Int = 0,
        val unparsedLines: Int = 0
    )

    private fun parseIcs(rawContent: String): InternalParseOutput {
        // Unfold RFC 5545 folded lines (CRLF + space/tab)
        val unfolded = rawContent
            .replace("\r\n ", "")
            .replace("\r\n\t", "")
            .replace("\n ", "")
            .replace("\n\t", "")

        val lines = unfolded.lines()
        val outDates = mutableListOf<Pair<String, String>>()
        var skippedRecurring = 0

        var inEvent = false
        var dtStartRaw: String? = null
        var dtEndRaw: String? = null
        var summaryRaw: String? = null
        var hasRrule = false

        val outFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }

        fun flushEvent() {
            if (hasRrule) {
                skippedRecurring++
                return
            }
            val startParsed = dtStartRaw?.let(::parseIcsDateValue) ?: return
            val summary = unescapeIcsText(summaryRaw.orEmpty())
                .ifBlank { LeaveDefaults.defaultTitleFor(LeaveDefaults.TYPE_HOLIDAY) }

            val endParsed = dtEndRaw?.let(::parseIcsDateValue)
            val startCal = Calendar.getInstance().apply {
                time = startParsed.first
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val endCalInclusive = if (endParsed != null) {
                val c = Calendar.getInstance().apply {
                    time = endParsed.first
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                // Per RFC 5545, DTEND for all-day VALUE=DATE events is exclusive
                val isExclusiveMidnight = endParsed.second || endParsed.third
                if (isExclusiveMidnight && c.after(startCal)) {
                    c.add(Calendar.DAY_OF_MONTH, -1)
                }
                if (c.before(startCal)) startCal.clone() as Calendar else c
            } else {
                startCal.clone() as Calendar
            }

            // Guard against corrupt multi-year events
            var steps = 0
            while (!startCal.after(endCalInclusive) && steps < 60) {
                outDates.add(outFmt.format(startCal.time) to summary)
                startCal.add(Calendar.DAY_OF_MONTH, 1)
                steps++
            }
        }

        for (rawLine in lines) {
            val line = rawLine.trim()
            when {
                line.equals("BEGIN:VEVENT", ignoreCase = true) -> {
                    inEvent = true
                    dtStartRaw = null
                    dtEndRaw = null
                    summaryRaw = null
                    hasRrule = false
                }
                line.equals("END:VEVENT", ignoreCase = true) -> {
                    if (inEvent) {
                        flushEvent()
                        inEvent = false
                    }
                }
                inEvent -> {
                    val colonIdx = line.indexOf(':')
                    if (colonIdx > 0) {
                        val prop = line.substring(0, colonIdx).uppercase(Locale.US)
                        val value = line.substring(colonIdx + 1)
                        val propName = prop.substringBefore(';')
                        when (propName) {
                            "RRULE" -> hasRrule = true
                            "DTSTART" -> dtStartRaw = value
                            "DTEND" -> dtEndRaw = value
                            "SUMMARY" -> summaryRaw = value
                        }
                    }
                }
            }
        }

        return InternalParseOutput(dates = outDates, skippedRecurring = skippedRecurring)
    }

    /**
     * Returns Triple(Date, isAllDayDate, isMidnightTimestamp)
     */
    private fun parseIcsDateValue(value: String): Triple<Date, Boolean, Boolean>? {
        val cleaned = value.trim()
        val dateOnlyMatch = Regex("^(\\d{8})$").matchEntire(cleaned)
        if (dateOnlyMatch != null) {
            val fmt = SimpleDateFormat("yyyyMMdd", Locale.US).apply { isLenient = false }
            val pos = ParsePosition(0)
            val d = fmt.parse(cleaned, pos) ?: return null
            if (pos.index != cleaned.length) return null
            return Triple(d, true, true)
        }
        val dateTimeMatch = Regex("^(\\d{8})T(\\d{6})Z?$", RegexOption.IGNORE_CASE).matchEntire(cleaned)
        if (dateTimeMatch != null) {
            val datePart = dateTimeMatch.groupValues[1]
            val timePart = dateTimeMatch.groupValues[2]
            val fmt = SimpleDateFormat("yyyyMMdd", Locale.US).apply { isLenient = false }
            val pos = ParsePosition(0)
            val d = fmt.parse(datePart, pos) ?: return null
            if (pos.index != datePart.length) return null
            return Triple(d, false, timePart == "000000")
        }
        return null
    }

    private fun unescapeIcsText(text: String): String {
        return text
            .replace("\\n", " ")
            .replace("\\,", ",")
            .replace("\\;", ";")
            .replace("\\\\", "\\")
            .trim()
    }

    private fun parsePlainLines(rawContent: String): InternalParseOutput {
        val outDates = mutableListOf<Pair<String, String>>()
        var unparsed = 0
        val outFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }

        val isoFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
        val dmyFmt = SimpleDateFormat("dd-MM-yyyy", Locale.US).apply { isLenient = false }
        val dMonYFmt = SimpleDateFormat("d MMM yyyy", Locale.US).apply { isLenient = false }

        val isoRegex = Regex("^\\d{4}-\\d{2}-\\d{2}$")
        val dmyRegex = Regex("^\\d{2}-\\d{2}-\\d{4}$")

        fun parseStrict(text: String, fmt: SimpleDateFormat): Date? {
            val pos = ParsePosition(0)
            val parsed = fmt.parse(text, pos)
            if (parsed == null || pos.index != text.length || pos.errorIndex != -1) {
                return null
            }
            return parsed
        }

        for (rawLine in rawContent.lines()) {
            val line = rawLine.trim()
            if (line.isBlank()) continue

            val commaIdx = line.indexOf(',')
            val datePart = if (commaIdx >= 0) line.substring(0, commaIdx).trim() else line
            val titlePart = if (commaIdx >= 0) {
                line.substring(commaIdx + 1).trim().ifBlank { LeaveDefaults.defaultTitleFor(LeaveDefaults.TYPE_HOLIDAY) }
            } else {
                LeaveDefaults.defaultTitleFor(LeaveDefaults.TYPE_HOLIDAY)
            }

            val parsedDate = when {
                isoRegex.matches(datePart) -> parseStrict(datePart, isoFmt)
                dmyRegex.matches(datePart) -> parseStrict(datePart, dmyFmt)
                else -> parseStrict(datePart, dMonYFmt)
            }

            if (parsedDate != null) {
                outDates.add(outFmt.format(parsedDate) to titlePart)
            } else {
                unparsed++
            }
        }

        return InternalParseOutput(dates = outDates, unparsedLines = unparsed)
    }
}
