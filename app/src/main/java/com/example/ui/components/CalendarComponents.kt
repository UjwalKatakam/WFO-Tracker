package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.CheckInLog
import com.example.data.local.LeaveHoliday
import com.example.ui.theme.AppTheme
import com.example.ui.theme.CalendarCellShape
import com.example.ui.theme.SurfaceGroupShape
import com.example.util.DateUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ============================================================================
// 1. UNIFIED MONTH / YEAR NAVIGATION HEADER
// ============================================================================

/**
 * Single month-navigation control:
 * Left arrow, clickable Month & Year title that opens a quick picker dialog,
 * right arrow, and jump-to-today action button.
 */
@Composable
fun MonthNavigationHeader(
    year: Int,
    month: Int,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectYearMonth: (year: Int, month: Int) -> Unit,
    onTodayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cal = remember(year, month) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }
    val monthTitle = remember(cal) {
        SimpleDateFormat("MMMM yyyy", Locale.US).format(cal.time)
    }

    var showPickerModal by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPrevMonth,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Previous month",
                tint = AppTheme.colors.ink
            )
        }

        Surface(
            shape = CircleShape,
            color = AppTheme.colors.card,
            border = wfoCardBorder(),
            modifier = Modifier
                .then(wfoCardShadow(elevation = 2.dp, shape = CircleShape))
                .clickable { showPickerModal = true }
                .semantics { role = Role.Button }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = monthTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = AppTheme.colors.ink,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select month and year",
                    tint = AppTheme.colors.muted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onTodayClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Today,
                    contentDescription = "Jump to today",
                    tint = AppTheme.colors.presentDeep
                )
            }

            IconButton(
                onClick = onNextMonth,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next month",
                    tint = AppTheme.colors.ink
                )
            }
        }
    }

    if (showPickerModal) {
        MonthYearPickerDialog(
            initialYear = year,
            initialMonth = month,
            onDismiss = { showPickerModal = false },
            onConfirm = { selectedYear, selectedMonth ->
                showPickerModal = false
                onSelectYearMonth(selectedYear, selectedMonth)
            }
        )
    }
}

/**
 * Clean Month and Year picker dialog.
 */
@Composable
fun MonthYearPickerDialog(
    initialYear: Int,
    initialMonth: Int,
    onDismiss: () -> Unit,
    onConfirm: (year: Int, month: Int) -> Unit
) {
    var pickerYear by remember { mutableIntStateOf(initialYear) }
    var pickerMonth by remember { mutableIntStateOf(initialMonth) }

    val monthNames = remember {
        listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = SurfaceGroupShape,
        containerColor = AppTheme.colors.sheet,
        titleContentColor = AppTheme.colors.ink,
        textContentColor = AppTheme.colors.ink,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select month and year",
                    style = MaterialTheme.typography.titleMedium,
                    color = AppTheme.colors.ink
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = AppTheme.colors.muted
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Year navigation row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { pickerYear -= 1 },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous year",
                            tint = AppTheme.colors.ink
                        )
                    }

                    Text(
                        text = "$pickerYear",
                        style = MaterialTheme.typography.titleLarge,
                        color = AppTheme.colors.ink,
                        fontWeight = FontWeight.SemiBold
                    )

                    IconButton(
                        onClick = { pickerYear += 1 },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next year",
                            tint = AppTheme.colors.ink
                        )
                    }
                }

                HorizontalDivider(color = AppTheme.colors.rule, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // 12 months grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(monthNames.indices.toList()) { index ->
                        val isSelected = index == pickerMonth
                        Surface(
                            selected = isSelected,
                            onClick = { pickerMonth = index },
                            shape = CircleShape,
                            color = if (isSelected) {
                                AppTheme.colors.presentContainer
                            } else {
                                AppTheme.colors.ground
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) AppTheme.colors.presentDeep else AppTheme.colors.rule
                            ),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = monthNames[index].take(3),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isSelected) AppTheme.colors.presentDeep else AppTheme.colors.ink,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            WfoPrimaryButton(
                text = "Apply",
                onClick = { onConfirm(pickerYear, pickerMonth) }
            )
        },
        dismissButton = {
            WfoTextButton(
                text = "Cancel",
                onClick = onDismiss
            )
        }
    )
}

// ============================================================================
// 2. MAIN MONTH CALENDAR CARD (WITH 8TH COLUMN WEEKLY QUOTA MARKER)
// ============================================================================

/**
 * Interactive Month Calendar Card.
 * 8-column grid: Monday to Sunday + Weekly Quota marker ("2/3").
 * Day cells:
 * - Present: Present tint with duration (>=12 sp).
 * - Under minimum: Short tint with duration (>=12 sp).
 * - Leave/Holiday: Outlined cell with small vector icon and letter (H, S, V, F, W), NO color.
 * - Today: 2 dp ring.
 * - Unlogged weekdays: Plain.
 * - Weekends: Dimmed.
 * - Long-press: Contextual day management dialog.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MonthCalendarCard(
    year: Int,
    month: Int,
    logs: List<CheckInLog>,
    leaves: List<LeaveHoliday>,
    minHours: Double,
    targetDaysPerWeek: Int,
    selectedDateKey: String,
    onDateSelected: (String) -> Unit,
    onPrevMonth: (() -> Unit)? = null,
    onNextMonth: (() -> Unit)? = null,
    onSelectMonth: ((year: Int, month: Int) -> Unit)? = null,
    onTodayClick: (() -> Unit)? = null,
    onDateLongClick: ((String) -> Unit)? = null,
    onEditDate: ((String) -> Unit)? = null,
    onDeleteDate: ((String) -> Unit)? = null,
    showHint: Boolean = false,
    modifier: Modifier = Modifier
) {
    val cal = remember(year, month) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }

    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // Sunday = 1, Monday = 2
    val offsetDays = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - 2
    val totalDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val todayDateStr = remember { DateUtils.todayDateStr() }

    WfoCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Optional local month nav if passed
            if (onPrevMonth != null && onNextMonth != null && onSelectMonth != null && onTodayClick != null) {
                MonthNavigationHeader(
                    year = year,
                    month = month,
                    onPrevMonth = onPrevMonth,
                    onNextMonth = onNextMonth,
                    onSelectYearMonth = onSelectMonth,
                    onTodayClick = onTodayClick
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 8-column header row: Mon-Sun + Quota
            val dayHeaders = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun", "Goal")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                dayHeaders.forEachIndexed { index, header ->
                    val isQuotaCol = index == 7
                    Text(
                        text = header,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isQuotaCol) AppTheme.colors.presentDeep else AppTheme.colors.muted,
                        fontWeight = if (isQuotaCol) FontWeight.SemiBold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            HorizontalDivider(color = AppTheme.colors.rule, thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Calendar weeks calculation
            val totalCells = offsetDays + totalDaysInMonth
            val totalRows = (totalCells + 6) / 7

            for (weekRow in 0 until totalRows) {
                var weekWfoCount = 0

                // Count valid WFO days in this specific week row
                for (col in 0..6) {
                    val cellIndex = weekRow * 7 + col
                    val dayNumber = cellIndex - offsetDays + 1
                    if (dayNumber in 1..totalDaysInMonth) {
                        val dateKey = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayNumber)
                        val log = logs.find { it.date == dateKey }
                        if (log?.isValidWFODay(minHours) == true) {
                            weekWfoCount++
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (col in 0..6) {
                        val cellIndex = weekRow * 7 + col
                        val dayNumber = cellIndex - offsetDays + 1

                        if (dayNumber in 1..totalDaysInMonth) {
                            val dateKey = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayNumber)
                            val isSelected = dateKey == selectedDateKey
                            val isToday = dateKey == todayDateStr
                            val isWeekend = col == 5 || col == 6

                            val log = logs.find { it.date == dateKey }
                            val leave = leaves.find { it.date == dateKey }

                            val isValidWfo = log?.isValidWFODay(minHours) == true
                            val isShortWfo = log != null && !isValidWfo

                            // Colorless outline for leave/holiday
                            val isLeaveOrHoliday = leave != null

                            val cellBg = when {
                                isSelected -> AppTheme.colors.progress
                                isLeaveOrHoliday -> AppTheme.colors.rule.copy(alpha = 0.45f)
                                isValidWfo -> AppTheme.colors.presentContainer
                                else -> Color.Transparent
                            }

                            val borderModifier = when {
                                isToday -> Modifier.border(2.dp, AppTheme.colors.presentDeep, CalendarCellShape)
                                isSelected -> Modifier
                                isShortWfo -> Modifier.border(1.dp, AppTheme.colors.short, CalendarCellShape)
                                isLeaveOrHoliday -> Modifier.border(1.dp, AppTheme.colors.rule, CalendarCellShape)
                                else -> Modifier
                            }

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
                                if (isToday) append("today, ")
                                if (leave != null) append("leave ${leave.type}, ")
                                else if (isValidWfo) append("office day ${log?.formattedDuration}, ")
                                else if (isShortWfo) append("under minimum ${log?.formattedDuration}, ")
                                else if (isWeekend) append("weekend, ")
                                else append("unlogged, ")
                                if (isSelected) append("selected")
                            }

                            val hasEntry = log != null || leave != null

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 44.dp)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(CalendarCellShape)
                                    .background(cellBg)
                                    .then(borderModifier)
                                    .then(clickModifier)
                                    .semantics {
                                        contentDescription = cellDesc
                                        if (hasEntry) {
                                            customActions = listOf(
                                                CustomAccessibilityAction("Edit") {
                                                    if (onEditDate != null) {
                                                        onEditDate(dateKey)
                                                    } else {
                                                        onDateLongClick?.invoke(dateKey)
                                                    }
                                                    true
                                                },
                                                CustomAccessibilityAction("Delete") {
                                                    if (onDeleteDate != null) {
                                                        onDeleteDate(dateKey)
                                                    } else {
                                                        onDateLongClick?.invoke(dateKey)
                                                    }
                                                    true
                                                }
                                            )
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(1.dp)
                                ) {
                                    Text(
                                        text = "$dayNumber",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isValidWfo || isSelected || isToday) FontWeight.SemiBold else FontWeight.Normal,
                                        color = when {
                                            isSelected -> AppTheme.colors.onAction
                                            isValidWfo -> AppTheme.colors.presentDeep
                                            isShortWfo -> AppTheme.colors.short
                                            isLeaveOrHoliday -> AppTheme.colors.ink
                                            isWeekend -> AppTheme.colors.muted
                                            else -> AppTheme.colors.ink
                                        }
                                    )

                                    if (leave != null) {
                                        val (_, letter) = getLeaveIconAndLetter(leave.type)
                                        Text(
                                            text = letter,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) AppTheme.colors.onAction else AppTheme.colors.muted,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    } else if (isValidWfo) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if (isSelected) AppTheme.colors.onAction else AppTheme.colors.presentDeep,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    } else if (isShortWfo && log != null) {
                                        Text(
                                            text = formatCompactDuration(log.totalHours),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) AppTheme.colors.onAction else AppTheme.colors.short,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        } else {
                            // Blank cell padding
                            Spacer(
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 44.dp)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                            )
                        }
                    }

                    // 8th column: Weekly quota marker
                    val isQuotaMet = weekWfoCount >= targetDaysPerWeek
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 44.dp)
                            .aspectRatio(1f)
                            .padding(2.dp)
                            .clip(CalendarCellShape)
                            .background(
                                if (isQuotaMet) {
                                    AppTheme.colors.presentContainer
                                } else {
                                    Color.Transparent
                                }
                            )
                            .border(
                                1.dp,
                                if (isQuotaMet) AppTheme.colors.presentDeep.copy(alpha = 0.4f) else AppTheme.colors.rule.copy(alpha = 0.6f),
                                CalendarCellShape
                            )
                            .semantics {
                                contentDescription = "Week quota: $weekWfoCount of $targetDaysPerWeek days met"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$weekWfoCount/$targetDaysPerWeek",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isQuotaMet) AppTheme.colors.presentDeep else AppTheme.colors.muted,
                            fontWeight = if (isQuotaMet) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = AppTheme.colors.rule, thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // One-line legend row under the grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CalendarLegendItem(
                    label = "Valid",
                    fillColor = AppTheme.colors.presentContainer,
                    borderColor = null
                )
                CalendarLegendItem(
                    label = "Under min",
                    fillColor = Color.Transparent,
                    borderColor = AppTheme.colors.short
                )
                CalendarLegendItem(
                    label = "Leave",
                    fillColor = AppTheme.colors.rule.copy(alpha = 0.45f),
                    borderColor = AppTheme.colors.rule
                )
            }
        }
    }
}

@Composable
private fun CalendarLegendItem(
    label: String,
    fillColor: Color,
    borderColor: Color?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(fillColor)
                .then(
                    if (borderColor != null) {
                        Modifier.border(1.dp, borderColor, RoundedCornerShape(4.dp))
                    } else {
                        Modifier
                    }
                )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AppTheme.colors.muted
        )
    }
}

/**
 * Maps leave type to a clean vector icon and standard letter:
 * Holiday -> H
 * Sick -> S
 * Vacation / Leave -> V
 * Floating -> F
 * WFH / Other -> W
 */
fun getLeaveIconAndLetter(type: String): Pair<ImageVector, String> {
    val lower = type.lowercase(Locale.ROOT)
    return when {
        lower.contains("holiday") -> Pair(Icons.Default.EventBusy, "H")
        lower.contains("sick") -> Pair(Icons.Default.LocalHospital, "S")
        lower.contains("vacation") || lower.contains("leave") -> Pair(Icons.Default.Flight, "V")
        lower.contains("float") -> Pair(Icons.Default.DateRange, "F")
        lower.contains("wfh") || lower.contains("home") -> Pair(Icons.Default.HomeWork, "W")
        else -> Pair(Icons.Default.DateRange, lower.take(1).uppercase(Locale.ROOT))
    }
}

/**
 * Compact duration formatter for calendar day cells.
 * Always at least 12 sp in the UI.
 */
private fun formatCompactDuration(hours: Double): String {
    val h = hours.toInt()
    val m = ((hours - h) * 60).toInt()
    return if (h > 0) "${h}h" else "${m}m"
}

// ============================================================================
// 3. 12-MONTH YEAR MATRIX MONTH CARD
// ============================================================================

/**
 * Dedicated month card for the 12-Month Year Overview.
 * Displays month name, office day count, and a compact calendar grid with Focus CTA.
 * No broken arrows!
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun YearMatrixMonthCard(
    year: Int,
    month: Int,
    logs: List<CheckInLog>,
    leaves: List<LeaveHoliday>,
    minHours: Double,
    targetDaysPerMonth: Int = 12,
    selectedDateKey: String,
    onDateSelected: (String) -> Unit,
    onOpenMonthClick: (year: Int, month: Int) -> Unit,
    onDateLongClick: ((String) -> Unit)? = null,
    onEditDate: ((String) -> Unit)? = null,
    onDeleteDate: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val cal = remember(year, month) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }
    val todayCal = remember { Calendar.getInstance() }
    val isCurrentCalendarMonth = year == todayCal.get(Calendar.YEAR) && month == todayCal.get(Calendar.MONTH)
    val todayDateStr = remember { DateUtils.todayDateStr() }

    val monthName = remember(cal) {
        SimpleDateFormat("MMMM", Locale.US).format(cal.time)
    }

    val monthPrefix = String.format(Locale.US, "%04d-%02d", year, month + 1)
    val validDaysCount = logs.count { it.date.startsWith(monthPrefix) && it.isValidWFODay(minHours) }
    val weekdayLeavesCount = leaves.count {
        if (!it.date.startsWith(monthPrefix)) return@count false
        val d = DateUtils.parseDate(it.date)
        val c = Calendar.getInstance().apply { time = d }
        val dow = c.get(Calendar.DAY_OF_WEEK)
        dow != Calendar.SATURDAY && dow != Calendar.SUNDAY
    }
    val monthRequiredDays = maxOf(0, targetDaysPerMonth - weekdayLeavesCount)
    val monthProgress = if (monthRequiredDays > 0) {
        (validDaysCount.toFloat() / monthRequiredDays.toFloat()).coerceIn(0f, 1f)
    } else {
        1f
    }
    val leaveDaysCount = leaves.count { it.date.startsWith(monthPrefix) }

    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    val offsetDays = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - 2
    val totalDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    WfoCard(
        elevation = 2.dp,
        border = if (isCurrentCalendarMonth) BorderStroke(2.dp, AppTheme.colors.presentDeep) else null,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Month Name & quota stats & Focus CTA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = monthName,
                        style = MaterialTheme.typography.titleMedium,
                        color = AppTheme.colors.ink,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "$validDaysCount of $monthRequiredDays days",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppTheme.colors.presentDeep,
                            fontWeight = FontWeight.Medium
                        )
                        if (leaveDaysCount > 0) {
                            Text(
                                text = "$leaveDaysCount leaves",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppTheme.colors.muted
                            )
                        }
                    }
                }

                WfoTonalButton(
                    text = "Focus",
                    onClick = { onOpenMonthClick(year, month) },
                    icon = Icons.Default.NorthEast
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Horizontal progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(AppTheme.colors.trackEmpty)
            ) {
                if (monthProgress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(monthProgress)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(AppTheme.colors.progress)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Day of week headers
            val dayHeaders = listOf("M", "T", "W", "T", "F", "S", "S")
            Row(modifier = Modifier.fillMaxWidth()) {
                dayHeaders.forEach { header ->
                    Text(
                        text = header,
                        style = MaterialTheme.typography.labelSmall,
                        color = AppTheme.colors.muted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Mini Calendar Grid
            val totalCells = offsetDays + totalDaysInMonth
            val totalRows = (totalCells + 6) / 7

            for (weekRow in 0 until totalRows) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 1.dp)
                ) {
                    for (col in 0..6) {
                        val cellIndex = weekRow * 7 + col
                        val dayNumber = cellIndex - offsetDays + 1

                        if (dayNumber in 1..totalDaysInMonth) {
                            val dateKey = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayNumber)
                            val isSelected = dateKey == selectedDateKey
                            val isToday = dateKey == todayDateStr
                            val isWeekend = col == 5 || col == 6

                            val log = logs.find { it.date == dateKey }
                            val leave = leaves.find { it.date == dateKey }

                            val isValidWfo = log?.isValidWFODay(minHours) == true
                            val isShortWfo = log != null && !isValidWfo

                            val cellBg = when {
                                isSelected -> AppTheme.colors.progress
                                leave != null -> AppTheme.colors.rule.copy(alpha = 0.45f)
                                isValidWfo -> AppTheme.colors.presentContainer
                                else -> Color.Transparent
                            }

                            val borderModifier = when {
                                isToday -> Modifier.border(2.dp, AppTheme.colors.presentDeep, CalendarCellShape)
                                isSelected -> Modifier
                                isShortWfo -> Modifier.border(1.dp, AppTheme.colors.short, CalendarCellShape)
                                leave != null -> Modifier.border(1.dp, AppTheme.colors.rule, CalendarCellShape)
                                else -> Modifier
                            }

                            val clickModifier = if (onDateLongClick != null) {
                                Modifier.combinedClickable(
                                    onClick = { onDateSelected(dateKey) },
                                    onLongClick = { onDateLongClick(dateKey) }
                                )
                            } else {
                                Modifier.clickable { onDateSelected(dateKey) }
                            }

                            val hasEntry = log != null || leave != null
                            val miniCellDesc = buildString {
                                append("Day $dayNumber, ")
                                if (leave != null) append("leave ${leave.type}")
                                else if (isValidWfo) append("office day")
                                else if (isShortWfo) append("under minimum")
                                else if (isWeekend) append("weekend")
                                else append("unlogged")
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(1.dp)
                                    .clip(CalendarCellShape)
                                    .background(cellBg)
                                    .then(borderModifier)
                                    .then(clickModifier)
                                    .semantics {
                                        contentDescription = miniCellDesc
                                        if (hasEntry) {
                                            customActions = listOf(
                                                CustomAccessibilityAction("Edit") {
                                                    if (onEditDate != null) {
                                                        onEditDate(dateKey)
                                                    } else {
                                                        onDateLongClick?.invoke(dateKey)
                                                    }
                                                    true
                                                },
                                                CustomAccessibilityAction("Delete") {
                                                    if (onDeleteDate != null) {
                                                        onDeleteDate(dateKey)
                                                    } else {
                                                        onDateLongClick?.invoke(dateKey)
                                                    }
                                                    true
                                                }
                                            )
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (leave != null) {
                                    val (_, letter) = getLeaveIconAndLetter(leave.type)
                                    Text(
                                        text = letter,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) AppTheme.colors.onAction else AppTheme.colors.muted,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                } else {
                                    Text(
                                        text = "$dayNumber",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = when {
                                            isSelected -> AppTheme.colors.onAction
                                            isValidWfo -> AppTheme.colors.presentDeep
                                            isShortWfo -> AppTheme.colors.short
                                            isWeekend -> AppTheme.colors.muted
                                            else -> AppTheme.colors.ink
                                        }
                                    )
                                }
                            }
                        } else {
                            Spacer(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 4. SELECTED DAY DETAIL CARD (WITH EMPTY STATE HELPFUL HINT)
// ============================================================================

/**
 * Selected Day Detail Card.
 * Displays friendly date, log or leave timeline, or a helpful hint when nothing is logged.
 * Replaces old emoji banners with quiet utility layout.
 */
@Composable
fun DailyStatusTimelineCard(
    dateKey: String,
    log: CheckInLog?,
    leave: LeaveHoliday?,
    isTodayPunchedIn: Boolean,
    activeStartMillis: Long?,
    minHours: Double,
    onLogActionClick: (() -> Unit)? = null,
    onEditClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val parsedDate = remember(dateKey) { DateUtils.parseDate(dateKey) }
    val friendlyDate = remember(parsedDate) {
        SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US).format(parsedDate)
    }
    val isToday = dateKey == DateUtils.todayDateStr()

    WfoCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Friendly date and action button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Day detail",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppTheme.colors.muted
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = friendlyDate,
                        style = MaterialTheme.typography.titleSmall,
                        color = AppTheme.colors.ink,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (log == null && leave == null && onLogActionClick != null) {
                    WfoTextButton(
                        text = "Log day",
                        onClick = onLogActionClick,
                        textColorOverride = AppTheme.colors.presentDeep
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = AppTheme.colors.rule, thickness = 1.dp)
            Spacer(modifier = Modifier.height(14.dp))

            when {
                leave != null -> {
                    val (icon, _) = getLeaveIconAndLetter(leave.type)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = AppTheme.colors.raised,
                            border = BorderStroke(1.dp, AppTheme.colors.rule),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = AppTheme.colors.muted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = leave.title,
                                style = MaterialTheme.typography.bodyLarge,
                                color = AppTheme.colors.ink,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Excluded working day from quota (${leave.type})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppTheme.colors.muted
                            )
                        }
                    }
                }

                isToday && isTodayPunchedIn && activeStartMillis != null -> {
                    val startTimeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(activeStartMillis))
                    val targetMillis = activeStartMillis + (minHours * 3600 * 1000L).toLong()
                    val targetTimeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(targetMillis))

                    CalendarTimelineRow(
                        title = "Punched in",
                        subtitle = "Arrived at $startTimeStr",
                        isCompleted = true,
                        isSuccess = true
                    )
                    CalendarTimelineRow(
                        title = "Target completion",
                        subtitle = "Reaches ${String.format(Locale.US, "%.1f", minHours)}h goal at $targetTimeStr",
                        isCompleted = false,
                        isLast = true
                    )
                }

                log != null -> {
                    val isValid = log.isValidWFODay(minHours)
                    CalendarTimelineRow(
                        title = "Arrival",
                        subtitle = "Check-in at ${DateUtils.formatTime12(log.inTime)}",
                        isCompleted = true,
                        isSuccess = isValid
                    )
                    CalendarTimelineRow(
                        title = "Departure",
                        subtitle = "Check-out at ${DateUtils.formatTime12(log.outTime)}",
                        isCompleted = true,
                        isSuccess = isValid
                    )
                    CalendarTimelineRow(
                        title = "Duration",
                        subtitle = "${log.formattedDuration} total — ${if (isValid) "Quota met" else "Under minimum"}",
                        isCompleted = true,
                        isSuccess = isValid,
                        isLast = true
                    )
                }

                else -> {
                    Text(
                        text = "Nothing logged for this day. Press and hold a day to add one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.colors.muted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }
            }

            if (log != null || leave != null) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = AppTheme.colors.rule, thickness = 1.dp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onEditClick != null || onLogActionClick != null) {
                        WfoTextButton(
                            text = "Edit",
                            onClick = { (onEditClick ?: onLogActionClick)?.invoke() },
                            textColorOverride = AppTheme.colors.presentDeep
                        )
                    }
                    if (onDeleteClick != null) {
                        WfoTextButton(
                            text = "Delete",
                            isDanger = true,
                            onClick = onDeleteClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarTimelineRow(
    title: String,
    subtitle: String,
    isCompleted: Boolean,
    isSuccess: Boolean = true,
    isLast: Boolean = false
) {
    val indicatorColor = when {
        !isCompleted -> AppTheme.colors.rule
        isSuccess -> AppTheme.colors.presentDeep
        else -> AppTheme.colors.short
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(indicatorColor.copy(alpha = 0.2f))
                    .border(1.5.dp, indicatorColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = indicatorColor,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(1.5.dp)
                        .height(30.dp)
                        .background(AppTheme.colors.rule)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.padding(bottom = if (isLast) 0.dp else 10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.colors.ink,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.colors.muted
            )
        }
    }
}
