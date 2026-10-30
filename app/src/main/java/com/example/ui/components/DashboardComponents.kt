package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppTheme
import com.example.ui.theme.CalendarCellShape
import com.example.ui.theme.SurfaceGroupShape
import com.example.ui.viewmodel.DayStreakDetail
import com.example.util.DateUtils
import java.util.Locale

// ============================================================================
// 1. HERO QUOTA COMPONENT (Sitting directly on Ground without Surface container)
// ============================================================================

/**
 * Month quota hero:
 * - Sits on Ground without a Surface container
 * - Month name heading (replaces "Monthly quota")
 * - Quota as "8 of 10 days" in 28 sp
 * - WfoQuotaBar with underMinimum = 0
 * - Plain sentence for pace / deficit
 * - Text line for under-minimum days only when count > 0 ("1 day under 6 hours")
 */
@Composable
fun DashboardHeroQuota(
    monthName: String,
    validDaysThisMonth: Int,
    requiredDaysThisMonth: Int,
    underMinimumDays: Int,
    minHours: Double,
    paceSentence: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = monthName,
            style = MaterialTheme.typography.titleMedium,
            color = AppTheme.colors.muted
        )

        Text(
            text = "$validDaysThisMonth of $requiredDaysThisMonth days",
            style = MaterialTheme.typography.headlineLarge,
            color = AppTheme.colors.ink
        )

        WfoQuotaBar(
            totalRequired = requiredDaysThisMonth,
            completed = validDaysThisMonth,
            underMinimum = 0, // under-minimum days do not occupy quota segments
            contentDescription = "$validDaysThisMonth of $requiredDaysThisMonth days completed",
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = paceSentence,
            style = MaterialTheme.typography.bodyMedium,
            color = AppTheme.colors.muted
        )

        if (underMinimumDays > 0) {
            val hoursLabel = if (minHours % 1.0 == 0.0) "${minHours.toInt()}" else String.format(Locale.US, "%.1f", minHours)
            val dayWord = if (underMinimumDays == 1) "day" else "days"
            Text(
                text = "$underMinimumDays $dayWord under $hoursLabel hours",
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.colors.short
            )
        }
    }
}

// ============================================================================
// 2. PUNCH CONTROL (Sitting directly on Ground without Surface container)
// ============================================================================

/**
 * Punch control:
 * - Sits directly on Ground
 * - Idle: full-width "Punch in" button and "Arrived earlier?" text button.
 * - Active: 56 sp timer that shrinks to fit at 1.3x scale on one line, remaining time, and "Punch out" button.
 */
@Composable
fun DashboardPunchControl(
    isPunchedIn: Boolean,
    elapsedSeconds: Long,
    minHours: Double,
    targetReached: Boolean,
    targetTimeFormatted: String,
    onPunchInClick: () -> Unit,
    onPunchOutClick: () -> Unit,
    onArrivedEarlierClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (!isPunchedIn) {
            WfoPrimaryButton(
                text = "Punch in",
                onClick = onPunchInClick,
                modifier = Modifier.fillMaxWidth()
            )

            WfoTextButton(
                text = "Arrived earlier?",
                onClick = onArrivedEarlierClick
            )
        } else {
            val hours = elapsedSeconds / 3600
            val minutes = (elapsedSeconds % 3600) / 60
            val seconds = elapsedSeconds % 60
            val timerText = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Timer stays on one line and auto-scales down on compact widths/high font scales
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val fontSize = if (maxWidth < 360.dp) 44.sp else 56.sp
                    Text(
                        text = timerText,
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = fontSize),
                        color = AppTheme.colors.ink,
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.Center
                    )
                }

                val targetSec = (minHours * 3600).toLong()
                val remainingSec = maxOf(0L, targetSec - elapsedSeconds)

                if (targetReached || remainingSec <= 0L) {
                    Text(
                        text = "Daily target met",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.colors.present,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    val remHours = remainingSec / 3600
                    val remMins = (remainingSec % 3600) / 60
                    val remText = if (remHours > 0) "${remHours}h ${remMins}m" else "${remMins}m"
                    Text(
                        text = "Remaining: $remText",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.colors.muted
                    )
                }

                if (targetTimeFormatted.isNotBlank()) {
                    Text(
                        text = "Target departure: $targetTimeFormatted",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTheme.colors.muted
                    )
                }
            }

            WfoPrimaryButton(
                text = "Punch out",
                onClick = onPunchOutClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ============================================================================
// 3. ARRIVED EARLIER BOTTOM SHEET (FlowRow chips with "Now", "5 min", etc.)
// ============================================================================

/**
 * Bottom sheet for backdating arrival time with transit offsets or time picker.
 * Uses FlowRow for chips and explicit reset upon dismissal.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ArrivedEarlierBottomSheet(
    selectedOffsetMinutes: Int,
    customTimeInput: String,
    onOffsetSelected: (Int) -> Unit,
    onPickTimeClick: () -> Unit,
    onClearCustomTime: () -> Unit,
    onConfirmPunchIn: () -> Unit,
    onDismissRequest: () -> Unit
) {
    WfoBottomSheet(
        onDismissRequest = onDismissRequest,
        title = "Arrived earlier?"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "How long ago did you arrive?",
                style = MaterialTheme.typography.labelLarge,
                color = AppTheme.colors.muted
            )

            // FlowRow chip layout with standardized "Now", "5 min", "10 min", "15 min"
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0 to "Now", 5 to "5 min", 7 to "7 min", 10 to "10 min", 15 to "15 min").forEach { (mins, label) ->
                    val isSelected = selectedOffsetMinutes == mins && customTimeInput.isBlank()
                    WfoChip(
                        text = label,
                        selected = isSelected,
                        onClick = { onOffsetSelected(mins) }
                    )
                }
            }

            HorizontalDivider(
                color = AppTheme.colors.rule,
                thickness = 1.dp
            )

            Text(
                text = "Or pick arrival time",
                style = MaterialTheme.typography.labelLarge,
                color = AppTheme.colors.muted
            )

            Surface(
                shape = SurfaceGroupShape,
                color = AppTheme.colors.ground,
                border = BorderStroke(1.dp, AppTheme.colors.rule),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onPickTimeClick)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = AppTheme.colors.muted,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (customTimeInput.isNotBlank()) {
                                DateUtils.formatTime12(customTimeInput)
                            } else {
                                "Pick arrival time"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (customTimeInput.isNotBlank()) AppTheme.colors.ink else AppTheme.colors.muted
                        )
                    }

                    if (customTimeInput.isNotBlank()) {
                        IconButton(
                            onClick = onClearCustomTime,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear custom time",
                                tint = AppTheme.colors.muted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            WfoPrimaryButton(
                text = "Punch in",
                onClick = onConfirmPunchIn,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ============================================================================
// 4. THIS WEEK STRIP (Sitting directly on Ground without Surface container)
// ============================================================================

/**
 * Week strip:
 * - Sits on Ground without a Surface container
 * - Order is strictly Monday through Friday
 * - 2 dp ring on today
 * - Present = filled Present
 * - Leave = outlined with letter
 * - Past empty = outlined cell
 * - Future weekday = filled Rule tone
 * - Explicit TalkBack contentDescription on each marker ("Tuesday, present, 6h 11m")
 */
@Composable
fun DashboardThisWeek(
    weekBreakdown: List<DayStreakDetail>,
    validDaysThisWeek: Int,
    targetDaysPerWeek: Int,
    todayDateKey: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "This week",
                style = MaterialTheme.typography.titleMedium,
                color = AppTheme.colors.muted
            )
            Text(
                text = "$validDaysThisWeek of $targetDaysPerWeek days",
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.colors.ink,
                fontWeight = FontWeight.Medium
            )
        }

        // Five weekdays strictly Mon-Fri (first 5 elements of currentWeekBreakdown)
        val weekdays = weekBreakdown.take(5)
        val fullDayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            weekdays.forEachIndexed { index, day ->
                val isToday = day.dateKey == todayDateKey
                val isPresent = day.isValidWfo
                val isShort = !day.isValidWfo && day.hours > 0.0
                val isLeave = day.isLeave
                val isFuture = day.isFutureOrRest && !isToday

                val fullDayName = fullDayNames.getOrElse(index) { day.dayName }
                val hoursFormatted = String.format(Locale.US, "%dh %02dm", (day.hours).toInt(), ((day.hours % 1.0) * 60).toInt())

                val statusDescription = when {
                    isPresent -> "present, $hoursFormatted"
                    isShort -> "under minimum, $hoursFormatted"
                    isLeave -> "leave"
                    isFuture -> "upcoming"
                    else -> "absent"
                }

                val markerDescription = "$fullDayName, $statusDescription"

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = day.dayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isToday) AppTheme.colors.ink else AppTheme.colors.muted,
                        fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal
                    )

                    val todayRingModifier = if (isToday) {
                        Modifier.border(2.dp, AppTheme.colors.ink, CalendarCellShape)
                    } else {
                        Modifier
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .then(todayRingModifier)
                            .then(
                                when {
                                    isPresent -> Modifier
                                        .clip(CalendarCellShape)
                                        .background(AppTheme.colors.present)
                                    isShort -> Modifier
                                        .border(1.5.dp, AppTheme.colors.short, CalendarCellShape)
                                    isLeave -> Modifier
                                        .border(1.dp, AppTheme.colors.rule, CalendarCellShape)
                                    isFuture -> Modifier
                                        .clip(CalendarCellShape)
                                        .background(AppTheme.colors.rule)
                                    else -> Modifier // Past empty weekday: outlined cell
                                        .border(1.dp, AppTheme.colors.rule, CalendarCellShape)
                                }
                            )
                            .semantics {
                                this.contentDescription = markerDescription
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLeave) {
                            Text(
                                text = "L",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppTheme.colors.muted
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 5. INSIGHTS CHARTS: HORIZONTAL HISTOGRAM & UNIFIED HOURS TREND
// ============================================================================

/**
 * Arrival Times Histogram:
 * - One horizontal row per bin
 * - Full label on the left (no maxLines truncation)
 * - Horizontal bar in the middle (Peak in Present, others in Muted at 35% alpha)
 * - Count on the right
 */
@Composable
fun DashboardArrivalHistogram(
    arrivalBins: List<DateUtils.ArrivalBin>,
    modifier: Modifier = Modifier
) {
    val maxCount = arrivalBins.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    val peakBin = arrivalBins.maxByOrNull { it.count }

    Surface(
        shape = SurfaceGroupShape,
        color = AppTheme.colors.raised,
        border = BorderStroke(1.dp, AppTheme.colors.rule),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Arrival times",
                    style = MaterialTheme.typography.titleSmall,
                    color = AppTheme.colors.ink
                )
                if (peakBin != null && peakBin.count > 0) {
                    Text(
                        text = "Peak: ${peakBin.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTheme.colors.muted
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                arrivalBins.forEach { bin ->
                    val isPeak = bin.count == peakBin?.count && bin.count > 0
                    val fraction = if (maxCount > 0) (bin.count.toFloat() / maxCount.toFloat()) else 0f

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Full label on the left (no maxLines)
                        Text(
                            text = bin.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.colors.ink,
                            modifier = Modifier.width(88.dp)
                        )

                        // Bar in the middle
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(12.dp)
                                .clip(CalendarCellShape)
                                .background(AppTheme.colors.rule)
                        ) {
                            if (bin.count > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction)
                                        .fillMaxHeight()
                                        .clip(CalendarCellShape)
                                        .background(
                                            if (isPeak) AppTheme.colors.present else AppTheme.colors.muted.copy(alpha = 0.35f)
                                        )
                                )
                            }
                        }

                        // Count on the right
                        Text(
                            text = "${bin.count}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isPeak) AppTheme.colors.present else AppTheme.colors.muted,
                            modifier = Modifier
                                .width(32.dp)
                                .padding(start = 10.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}

/**
 * Weekly Hours Trend:
 * - Bars and target line drawn in one coordinate space: bar height = hours / maxHours * plotHeight
 * - No coerceIn clamps on bar height
 * - Value and week labels outside the plot area (top and bottom rows)
 * - Target line drawn above the bars: 1.5 dp, AppTheme.colors.ink at 60% alpha
 */
@Composable
fun DashboardWeeklyHoursTrend(
    weeklyTrends: List<DateUtils.TrendPoint>,
    modifier: Modifier = Modifier
) {
    val maxHours = weeklyTrends.maxOfOrNull { maxOf(it.actualHours, it.targetHours) }?.coerceAtLeast(1.0) ?: 24.0
    val targetThreshold = weeklyTrends.firstOrNull()?.targetHours ?: 24.0
    val plotHeight = 100.dp

    Surface(
        shape = SurfaceGroupShape,
        color = AppTheme.colors.raised,
        border = BorderStroke(1.dp, AppTheme.colors.rule),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Weekly hours",
                    style = MaterialTheme.typography.titleSmall,
                    color = AppTheme.colors.ink
                )
                Text(
                    text = String.format(Locale.US, "Target: %.0fh", targetThreshold),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.colors.muted
                )
            }

            // Top row: Value labels outside the plot area
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weeklyTrends.forEach { point ->
                    val isMet = point.actualHours >= point.targetHours
                    Text(
                        text = String.format(Locale.US, "%.1fh", point.actualHours),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isMet) AppTheme.colors.present else AppTheme.colors.short,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Middle: Plot area in one coordinate space
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(plotHeight)
            ) {
                // 1. Bars drawn in one coordinate space (bar height = hours / maxHours * plotHeight)
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    weeklyTrends.forEach { point ->
                        val isMet = point.actualHours >= point.targetHours
                        val fraction = (point.actualHours / maxHours).toFloat()

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.5f)
                                    .fillMaxHeight(fraction)
                                    .clip(CalendarCellShape)
                                    .background(if (isMet) AppTheme.colors.present else AppTheme.colors.short)
                            )
                        }
                    }
                }

                // 2. Target line drawn above the bars: 1.5 dp, AppTheme.colors.ink at 60% alpha
                val targetLineColor = AppTheme.colors.ink.copy(alpha = 0.60f)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val yPos = size.height * (1f - (targetThreshold.toFloat() / maxHours.toFloat()))
                    drawLine(
                        color = targetLineColor,
                        start = Offset(0f, yPos),
                        end = Offset(size.width, yPos),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                    )
                }
            }

            // Bottom row: Week labels outside the plot area
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weeklyTrends.forEach { point ->
                    Text(
                        text = point.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTheme.colors.muted,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
