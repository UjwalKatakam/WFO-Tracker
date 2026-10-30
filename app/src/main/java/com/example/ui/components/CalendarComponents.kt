package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CheckInLog
import com.example.data.local.LeaveHoliday
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.PurpleExclusion
import com.example.util.DateUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MonthCalendarCard(
    year: Int,
    month: Int,
    logs: List<CheckInLog>,
    leaves: List<LeaveHoliday>,
    minHours: Double,
    selectedDateKey: String,
    onDateSelected: (String) -> Unit,
    onPrevMonth: (() -> Unit)? = null,
    onNextMonth: (() -> Unit)? = null,
    onSelectMonth: ((year: Int, month: Int) -> Unit)? = null,
    onTodayClick: (() -> Unit)? = null,
    onDateLongClick: ((String) -> Unit)? = null,
    showHint: Boolean = true,
    modifier: Modifier = Modifier
) {
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val monthTitle = SimpleDateFormat("MMMM yyyy", Locale.US).format(cal.time)
    var showMonthDropdown by remember { mutableStateOf(false) }

    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    // Calculate start day offset (Monday = 0)
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // Sunday = 1, Monday = 2
    val offsetDays = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - 2
    val totalDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Bar with Prev/Next and intuitive Month Quick Picker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onPrevMonth != null) {
                    IconButton(onClick = onPrevMonth, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
                    }
                } else {
                    Spacer(modifier = Modifier.size(36.dp))
                }

                Box {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.clickable {
                            if (onSelectMonth != null) showMonthDropdown = true
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = monthTitle,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (onSelectMonth != null) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = "Select Month",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    if (onSelectMonth != null) {
                        DropdownMenu(
                            expanded = showMonthDropdown,
                            onDismissRequest = { showMonthDropdown = false }
                        ) {
                            monthNames.forEachIndexed { idx, name ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "$name $year",
                                            fontWeight = if (idx == month) FontWeight.Bold else FontWeight.Normal,
                                            color = if (idx == month) BluePrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        showMonthDropdown = false
                                        onSelectMonth(year, idx)
                                    }
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onTodayClick != null) {
                        IconButton(
                            onClick = onTodayClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Today,
                                contentDescription = "Jump to Today",
                                tint = BluePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    if (onNextMonth != null) {
                        IconButton(onClick = onNextMonth, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
                        }
                    } else {
                        Spacer(modifier = Modifier.size(36.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Day of Week Header
            val dayHeaders = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            Row(modifier = Modifier.fillMaxWidth()) {
                dayHeaders.forEach { header ->
                    Text(
                        text = header,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 7-column Calendar Grid
            val totalCells = offsetDays + totalDaysInMonth
            val totalRows = (totalCells + 6) / 7

            for (row in 0 until totalRows) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    for (col in 0..6) {
                        val cellIndex = row * 7 + col
                        val dayNumber = cellIndex - offsetDays + 1

                        if (dayNumber in 1..totalDaysInMonth) {
                            val dateKey = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayNumber)
                            val isSelected = dateKey == selectedDateKey
                            val isWeekend = col == 5 || col == 6

                            val log = logs.find { it.date == dateKey }
                            val leave = leaves.find { it.date == dateKey }

                            val isValidWfo = log?.isValidWFODay(minHours) == true
                            val isShortWfo = log != null && !isValidWfo

                            val cellBg = when {
                                leave != null -> PurpleExclusion.copy(alpha = 0.25f)
                                isValidWfo -> GreenSuccess.copy(alpha = 0.25f)
                                isShortWfo -> AmberWarning.copy(alpha = 0.25f)
                                isWeekend -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            }

                            val borderModifier = if (isSelected) {
                                Modifier.border(2.dp, BluePrimary, RoundedCornerShape(6.dp))
                            } else if (isValidWfo) {
                                Modifier.border(1.dp, GreenSuccess.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            } else if (isShortWfo) {
                                Modifier.border(1.dp, AmberWarning.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            } else Modifier

                            val clickModifier = if (onDateLongClick != null) {
                                Modifier.combinedClickable(
                                    onClick = { onDateSelected(dateKey) },
                                    onLongClick = { onDateLongClick(dateKey) }
                                )
                            } else {
                                Modifier.clickable { onDateSelected(dateKey) }
                            }

                            val cellDesc = buildString {
                                append("Day $dayNumber, ")
                                if (leave != null) append("Excluded ${leave.type}, ")
                                else if (isValidWfo) append("Qualified WFO ${log?.formattedDuration}, ")
                                else if (isShortWfo) append("Under target ${log?.formattedDuration}, ")
                                else if (isWeekend) append("Weekend, ")
                                else append("No log, ")
                                if (isSelected) append("selected")
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(cellBg)
                                    .then(borderModifier)
                                    .then(clickModifier)
                                    .semantics { contentDescription = cellDesc },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$dayNumber",
                                        fontSize = 11.sp,
                                        fontWeight = if (isValidWfo || isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = when {
                                            leave != null -> PurpleExclusion
                                            isValidWfo -> GreenSuccess
                                            isShortWfo -> AmberWarning
                                            isWeekend -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )

                                    if (leave != null) {
                                        Text(
                                            text = when (leave.type) {
                                                "holiday" -> "🌴"
                                                "sick" -> "🤒"
                                                else -> "✈️"
                                            },
                                            fontSize = 7.sp
                                        )
                                    } else if (log != null) {
                                        Text(
                                            text = log.formattedDuration,
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isValidWfo) GreenSuccess else AmberWarning
                                        )
                                    }
                                }
                            }
                        } else {
                            // Empty placeholder cell
                            Spacer(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                            )
                        }
                    }
                }
            }

            if (showHint && onDateLongClick != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💡 Tap to view timeline • Long-press to log attendance or leave",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

/**
 * Dedicated Month Card for the 12-Month Year Overview.
 * Displays clean month title, compliance count, and "Open Month" action — NO broken/dead arrows!
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun YearMatrixMonthCard(
    year: Int,
    month: Int,
    logs: List<CheckInLog>,
    leaves: List<LeaveHoliday>,
    minHours: Double,
    selectedDateKey: String,
    onDateSelected: (String) -> Unit,
    onOpenMonthClick: (year: Int, month: Int) -> Unit,
    onDateLongClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val monthName = SimpleDateFormat("MMMM yyyy", Locale.US).format(cal.time)

    // Calculate month logs & leaves
    val monthPrefix = String.format(Locale.US, "%04d-%02d", year, month + 1)
    val validDaysCount = logs.count { it.date.startsWith(monthPrefix) && it.isValidWFODay(minHours) }
    val leaveDaysCount = leaves.count { it.date.startsWith(monthPrefix) }

    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    val offsetDays = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - 2
    val totalDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row: Month Name & stats & Open Month CTA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = monthName,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "✓ $validDaysCount WFO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GreenSuccess
                        )
                        if (leaveDaysCount > 0) {
                            Text(
                                text = "🌴 $leaveDaysCount Leaves",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PurpleExclusion
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = { onOpenMonthClick(year, month) },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("Focus", fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(Icons.Default.NorthEast, contentDescription = null, modifier = Modifier.size(11.dp))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Day of Week Header
            val dayHeaders = listOf("M", "T", "W", "T", "F", "S", "S")
            Row(modifier = Modifier.fillMaxWidth()) {
                dayHeaders.forEach { header ->
                    Text(
                        text = header,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Mini Calendar Grid
            val totalCells = offsetDays + totalDaysInMonth
            val totalRows = (totalCells + 6) / 7

            for (row in 0 until totalRows) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 1.dp)
                ) {
                    for (col in 0..6) {
                        val cellIndex = row * 7 + col
                        val dayNumber = cellIndex - offsetDays + 1

                        if (dayNumber in 1..totalDaysInMonth) {
                            val dateKey = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayNumber)
                            val isSelected = dateKey == selectedDateKey
                            val isWeekend = col == 5 || col == 6

                            val log = logs.find { it.date == dateKey }
                            val leave = leaves.find { it.date == dateKey }

                            val isValidWfo = log?.isValidWFODay(minHours) == true
                            val isShortWfo = log != null && !isValidWfo

                            val cellBg = when {
                                leave != null -> PurpleExclusion.copy(alpha = 0.25f)
                                isValidWfo -> GreenSuccess.copy(alpha = 0.25f)
                                isShortWfo -> AmberWarning.copy(alpha = 0.25f)
                                isWeekend -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            }

                            val borderModifier = if (isSelected) {
                                Modifier.border(1.5.dp, BluePrimary, RoundedCornerShape(4.dp))
                            } else Modifier

                            val clickModifier = if (onDateLongClick != null) {
                                Modifier.combinedClickable(
                                    onClick = { onDateSelected(dateKey) },
                                    onLongClick = { onDateLongClick(dateKey) }
                                )
                            } else {
                                Modifier.clickable { onDateSelected(dateKey) }
                            }

                            val cellDesc = "Day $dayNumber, ${monthName}" + if (isValidWfo) ", Qualified WFO" else if (leave != null) ", Excluded" else ""

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(1.5.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(cellBg)
                                    .then(borderModifier)
                                    .then(clickModifier)
                                    .semantics { contentDescription = cellDesc },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$dayNumber",
                                    fontSize = 9.5.sp,
                                    fontWeight = if (isValidWfo || isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = when {
                                        leave != null -> PurpleExclusion
                                        isValidWfo -> GreenSuccess
                                        isShortWfo -> AmberWarning
                                        isWeekend -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        } else {
                            Spacer(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(1.5.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DailyStatusTimelineCard(
    dateKey: String,
    log: CheckInLog?,
    leave: LeaveHoliday?,
    isTodayPunchedIn: Boolean,
    activeStartMillis: Long?,
    minHours: Double,
    onLogActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val parsedDate = DateUtils.parseDate(dateKey)
    val friendlyDate = SimpleDateFormat("EEE, MMM d, yyyy", Locale.US).format(parsedDate)
    val isToday = dateKey == DateUtils.todayDateStr()

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "📋 DAILY STATUS TIMELINE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = friendlyDate,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                if (onLogActionClick != null) {
                    OutlinedButton(
                        onClick = onLogActionClick,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            imageVector = if (log != null || leave != null) Icons.Default.Edit else Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (log != null || leave != null) "Edit Day" else "+ Log Day",
                            fontSize = 11.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            when {
                leave != null -> {
                    // Holiday / Leave status
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = PurpleExclusion.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = when (leave.type) {
                                        "holiday" -> "🌴"
                                        "sick" -> "🤒"
                                        else -> "✈️"
                                    },
                                    fontSize = 16.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = leave.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = PurpleExclusion
                            )
                            Text(
                                text = "Excluded working day from quota requirements (${leave.type.uppercase()})",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                isToday && isTodayPunchedIn && activeStartMillis != null -> {
                    // Active Ongoing Session
                    val startTimeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(activeStartMillis))
                    val targetMillis = activeStartMillis + (minHours * 3600 * 1000L).toLong()
                    val targetTimeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(targetMillis))

                    TimelineItem(
                        title = "Office Check-in: Arrived",
                        subtitle = "Punched in at $startTimeStr",
                        isCompleted = true
                    )
                    TimelineItem(
                        title = "Quota Limit: Daily Goal",
                        subtitle = "Will reach ${String.format(Locale.US, "%.1f", minHours)}h target at $targetTimeStr",
                        isCompleted = false
                    )
                }

                log != null -> {
                    // Completed Day
                    val isValid = log.isValidWFODay(minHours)
                    TimelineItem(
                        title = "Office Location: Arrived",
                        subtitle = "Office check-in registered at ${DateUtils.formatTime12(log.inTime)}",
                        isCompleted = true,
                        isSuccess = isValid
                    )
                    TimelineItem(
                        title = "Office Exit: Logged Off",
                        subtitle = "Office exit registered at ${DateUtils.formatTime12(log.outTime)}",
                        isCompleted = true,
                        isSuccess = isValid
                    )
                    TimelineItem(
                        title = "Summary Milestone",
                        subtitle = "Logged ${log.formattedDuration} total (${if (isValid) "✓ Quota Cleared" else "⚠️ Left Early"})",
                        isCompleted = true,
                        isSuccess = isValid,
                        isLast = true
                    )
                }

                else -> {
                    Text(
                        text = "No workspace attendance logs registered for this date.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineItem(
    title: String,
    subtitle: String,
    isCompleted: Boolean,
    isSuccess: Boolean = true,
    isLast: Boolean = false
) {
    val nodeColor = when {
        !isCompleted -> MaterialTheme.colorScheme.outline
        isSuccess -> GreenSuccess
        else -> AmberWarning
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(nodeColor.copy(alpha = 0.2f))
                    .border(2.dp, nodeColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = nodeColor,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(36.dp)
                        .background(nodeColor.copy(alpha = 0.4f))
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.padding(bottom = if (isLast) 0.dp else 12.dp)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
