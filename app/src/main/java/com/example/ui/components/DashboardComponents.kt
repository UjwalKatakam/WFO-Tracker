package com.example.ui.components

import android.animation.ValueAnimator
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppTheme
import com.example.ui.theme.CalendarCellShape
import com.example.ui.theme.ChipShape
import com.example.ui.theme.SurfaceGroupShape
import com.example.ui.viewmodel.DayStreakDetail
import com.example.util.DateUtils
import com.example.util.HapticHelper
import java.util.Calendar
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

private val WeekTileShape = RoundedCornerShape(12.dp)

// ============================================================================
// 0. GREETING LINE
// ============================================================================

@Composable
fun DashboardGreeting(
    greetingPrefix: String,
    firstName: String?,
    modifier: Modifier = Modifier
) {
    val mutedColor = AppTheme.colors.muted
    val inkColor = AppTheme.colors.ink

    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = mutedColor)) {
                append(greetingPrefix)
            }
            if (!firstName.isNullOrBlank()) {
                withStyle(SpanStyle(color = inkColor)) {
                    append(firstName)
                }
            }
        },
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier.fillMaxWidth()
    )
}

// ============================================================================
// 1. HERO QUOTA RING COMPONENT
// ============================================================================

@Composable
fun DashboardHeroQuota(
    monthName: String,
    monthKey: String,
    lastCelebratedMonthKey: String,
    validDaysThisMonth: Int,
    requiredDaysThisMonth: Int,
    paceMessage: DashboardHeroMessage,
    weeklyStreak: Int,
    onStreakClick: () -> Unit,
    onMonthCelebrated: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val reduceMotion = remember(context) {
        try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
            scale == 0f || !ValueAnimator.areAnimatorsEnabled()
        } catch (_: Exception) {
            !ValueAnimator.areAnimatorsEnabled()
        }
    }

    val targetProgress = if (requiredDaysThisMonth > 0) {
        (validDaysThisMonth.toFloat() / requiredDaysThisMonth.toFloat()).coerceIn(0f, 1f)
    } else {
        1f
    }

    val celebrationSweep = remember { Animatable(targetProgress) }
    val celebrationScale = remember { Animatable(1f) }
    var isCelebrating by remember { mutableStateOf(false) }

    LaunchedEffect(monthKey, validDaysThisMonth, requiredDaysThisMonth, lastCelebratedMonthKey) {
        val shouldCelebrate = requiredDaysThisMonth > 0 &&
            validDaysThisMonth >= requiredDaysThisMonth &&
            monthKey != lastCelebratedMonthKey

        if (shouldCelebrate) {
            onMonthCelebrated(monthKey)
            if (!reduceMotion) {
                isCelebrating = true
                val startFrac = ((validDaysThisMonth - 1).coerceAtLeast(0).toFloat() /
                    requiredDaysThisMonth.toFloat()).coerceIn(0f, 1f)
                celebrationSweep.snapTo(startFrac)
                celebrationSweep.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
                )
                HapticHelper.performMilestoneCelebration(context)
                celebrationScale.animateTo(1.04f, animationSpec = tween(durationMillis = 150))
                celebrationScale.animateTo(1f, animationSpec = tween(durationMillis = 220))
                isCelebrating = false
            } else {
                celebrationSweep.snapTo(1f)
                celebrationScale.snapTo(1f)
            }
        } else {
            celebrationSweep.snapTo(targetProgress)
            celebrationScale.snapTo(1f)
        }
    }

    val displayedProgress = if (isCelebrating) celebrationSweep.value else targetProgress

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (requiredDaysThisMonth > 0) {
            val presentColor = AppTheme.colors.present
            val presentDeepColor = AppTheme.colors.presentDeep
            val ruleColor = AppTheme.colors.rule
            val inkColor = AppTheme.colors.ink
            val mutedColor = AppTheme.colors.muted

            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val ringDiameter = minOf(maxWidth - 28.dp, 240.dp).coerceAtLeast(156.dp)
                val canvasSize = ringDiameter + 24.dp
                val density = LocalDensity.current
                val fontScale = density.fontScale

                Box(
                    modifier = Modifier
                        .size(canvasSize)
                        .graphicsLayer {
                            scaleX = celebrationScale.value
                            scaleY = celebrationScale.value
                        }
                        .clearAndSetSemantics {
                            contentDescription = "$validDaysThisMonth of $requiredDaysThisMonth days completed in $monthName"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokePx = 14.dp.toPx()
                        val ringSidePx = ringDiameter.toPx()
                        val ringRadiusPx = ringSidePx / 2f
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val arcTopLeft = Offset(
                            x = center.x - ringRadiusPx + strokePx / 2f,
                            y = center.y - ringRadiusPx + strokePx / 2f
                        )
                        val arcSize = Size(ringSidePx - strokePx, ringSidePx - strokePx)

                        if (requiredDaysThisMonth <= 24) {
                            val n = requiredDaysThisMonth
                            val gapDegrees = 4f
                            val stepDegrees = 360f / n
                            val sweepDegrees = (stepDegrees - gapDegrees).coerceAtLeast(2f)

                            for (index in 0 until n) {
                                val startAngle = -90f + (index * stepDegrees) + (gapDegrees / 2f)
                                val segmentThreshold = (index + 1).toFloat() / n.toFloat()
                                val isFilled = if (isCelebrating) {
                                    displayedProgress >= segmentThreshold - 0.0001f
                                } else {
                                    index < validDaysThisMonth
                                }

                                drawArc(
                                    color = if (isFilled) presentColor else ruleColor,
                                    startAngle = startAngle,
                                    sweepAngle = sweepDegrees,
                                    useCenter = false,
                                    topLeft = arcTopLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokePx, cap = StrokeCap.Butt)
                                )
                            }
                        } else {
                            drawArc(
                                color = ruleColor,
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = arcTopLeft,
                                size = arcSize,
                                style = Stroke(width = strokePx, cap = StrokeCap.Butt)
                            )
                            val filledSweep = 360f * displayedProgress
                            if (filledSweep > 0f) {
                                drawArc(
                                    color = presentColor,
                                    startAngle = -90f,
                                    sweepAngle = filledSweep,
                                    useCenter = false,
                                    topLeft = arcTopLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokePx, cap = StrokeCap.Butt)
                                )
                            }
                        }

                        // Milestone ticks outside the ring at 25%, 50%, 75%, and 100%
                        val milestones = listOf(0.25f, 0.50f, 0.75f, 1.00f)
                        val tickInnerRadius = ringRadiusPx + 4.dp.toPx()
                        val tickOuterRadius = ringRadiusPx + 9.dp.toPx()
                        val tickStroke = 2.5.dp.toPx()

                        milestones.forEach { milestone ->
                            val reached = displayedProgress >= milestone - 0.0001f
                            val angleRad = Math.toRadians(-90.0 + (360.0 * milestone))
                            val cosA = cos(angleRad).toFloat()
                            val sinA = sin(angleRad).toFloat()
                            drawLine(
                                color = if (reached) presentDeepColor else ruleColor,
                                start = Offset(
                                    center.x + tickInnerRadius * cosA,
                                    center.y + tickInnerRadius * sinA
                                ),
                                end = Offset(
                                    center.x + tickOuterRadius * cosA,
                                    center.y + tickOuterRadius * sinA
                                ),
                                strokeWidth = tickStroke,
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    // Center label inside the ring
                    val innerTextWidth = (ringDiameter - 44.dp).coerceAtLeast(100.dp)
                    val headlineText = "$validDaysThisMonth of $requiredDaysThisMonth"
                    var fontSizeSp by remember(headlineText, innerTextWidth, fontScale) {
                        val initial = if (fontScale > 1.05f) (48f / fontScale).coerceAtLeast(28f) else 56f
                        mutableStateOf(initial.sp)
                    }

                    Column(
                        modifier = Modifier.width(innerTextWidth),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = headlineText,
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = fontSizeSp,
                                lineHeight = (fontSizeSp.value * 1.12f).sp
                            ),
                            color = inkColor,
                            maxLines = 1,
                            softWrap = false,
                            textAlign = TextAlign.Center,
                            onTextLayout = { result ->
                                if ((result.hasVisualOverflow || result.didOverflowWidth) && fontSizeSp.value > 20f) {
                                    fontSizeSp = (fontSizeSp.value * 0.88f).sp
                                }
                            }
                        )
                        Text(
                            text = "days in $monthName",
                            style = MaterialTheme.typography.bodyMedium,
                            color = mutedColor,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Text(
            text = paceMessage.text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (paceMessage.isPaceWarning) AppTheme.colors.short else AppTheme.colors.muted,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite }
        )

        // Streak pill chip on the hero
        val streakText = "Streak: $weeklyStreak ${if (weeklyStreak == 1) "week" else "weeks"}"
        val streakActive = weeklyStreak > 0
        Surface(
            onClick = onStreakClick,
            shape = ChipShape,
            color = if (streakActive) AppTheme.colors.presentContainer else AppTheme.colors.raised,
            border = BorderStroke(
                width = 1.dp,
                color = if (streakActive) AppTheme.colors.presentDeep else AppTheme.colors.rule
            ),
            modifier = Modifier
                .defaultMinSize(minHeight = 48.dp)
                .semantics { role = Role.Button }
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = streakText,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (streakActive) AppTheme.colors.presentDeep else AppTheme.colors.ink,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ============================================================================
// 2. PUNCH CONTROL (Sitting directly on Ground without Surface container)
// ============================================================================

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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!isPunchedIn) {
            WfoPrimaryButton(
                text = "Punch in",
                onClick = onPunchInClick,
                modifier = Modifier.fillMaxWidth()
            )

            WfoTextButton(
                text = "Arrived earlier?",
                onClick = onArrivedEarlierClick,
                textColorOverride = AppTheme.colors.presentDeep
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
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val fontScale = LocalDensity.current.fontScale
                    val baseSp = if (maxWidth < 360.dp) 44f else 56f
                    var currentFontSize by remember(timerText, maxWidth, fontScale) {
                        val initial = if (fontScale > 1.05f) (baseSp / fontScale).coerceAtLeast(32f).sp else baseSp.sp
                        mutableStateOf(initial)
                    }

                    Text(
                        text = timerText,
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = currentFontSize),
                        color = AppTheme.colors.ink,
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.Center,
                        onTextLayout = { layoutResult ->
                            if (layoutResult.hasVisualOverflow || layoutResult.didOverflowWidth) {
                                if (currentFontSize.value > 22f) {
                                    currentFontSize = (currentFontSize.value * 0.9f).sp
                                }
                            }
                        }
                    )
                }

                val targetSec = (minHours * 3600).toLong()
                val remainingSec = maxOf(0L, targetSec - elapsedSeconds)

                if (targetReached || remainingSec <= 0L) {
                    Text(
                        text = "Daily target met",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.colors.presentDeep,
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
// 3. ARRIVED EARLIER BOTTOM SHEET
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ArrivedEarlierBottomSheet(
    onConfirmPunchIn: (startMillis: Long) -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    var selectedOffsetMinutes by remember { mutableIntStateOf(0) }
    var customTimeInput by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    fun parseTodayClockMillis(hhMm: String): Long? {
        val parts = hhMm.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val m = parts.getOrNull(1)?.toIntOrNull() ?: return null
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

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

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0 to "Now", 5 to "5 min", 10 to "10 min", 15 to "15 min").forEach { (mins, label) ->
                    val isSelected = selectedOffsetMinutes == mins && customTimeInput.isBlank()
                    WfoChip(
                        text = label,
                        selected = isSelected,
                        onClick = {
                            selectedOffsetMinutes = mins
                            customTimeInput = ""
                            errorMsg = null
                        }
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
                border = BorderStroke(1.dp, if (errorMsg != null) AppTheme.colors.danger else AppTheme.colors.rule),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        showNativeTimePicker(context, customTimeInput) { picked ->
                            customTimeInput = picked
                            val pickedMillis = parseTodayClockMillis(picked)
                            errorMsg = if (pickedMillis != null && pickedMillis > System.currentTimeMillis()) {
                                "Arrival time cannot be in the future."
                            } else {
                                null
                            }
                        }
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
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
                            onClick = {
                                customTimeInput = ""
                                errorMsg = null
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear custom time",
                                tint = AppTheme.colors.muted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            if (errorMsg != null) {
                Text(
                    text = errorMsg ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.colors.danger
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            WfoPrimaryButton(
                text = "Punch in",
                onClick = {
                    val now = System.currentTimeMillis()
                    if (customTimeInput.isNotBlank()) {
                        val pickedMillis = parseTodayClockMillis(customTimeInput)
                        if (pickedMillis == null || pickedMillis > now) {
                            errorMsg = "Arrival time cannot be in the future."
                            return@WfoPrimaryButton
                        }
                        onConfirmPunchIn(pickedMillis)
                    } else {
                        val startMillis = now - (selectedOffsetMinutes * 60 * 1000L)
                        onConfirmPunchIn(startMillis)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ============================================================================
// 4. THIS WEEK STRIP (Sitting directly on Ground)
// ============================================================================

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
        verticalArrangement = Arrangement.spacedBy(12.dp)
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

                val fullDayName = fullDayNames.getOrElse(index) { day.dayName }
                val dayNumber = day.dateKey.substringAfterLast("-").toIntOrNull()?.toString() ?: ""

                val statusDescription = when {
                    isPresent -> "present"
                    isShort -> "under minimum"
                    isLeave -> "leave"
                    else -> "no entry"
                }
                val markerDescription = "$fullDayName, $statusDescription"

                val tileBackground = when {
                    isPresent -> AppTheme.colors.presentContainer
                    isShort || isLeave -> Color.Transparent
                    else -> AppTheme.colors.raised
                }

                val tileBorder = when {
                    isToday -> BorderStroke(2.dp, AppTheme.colors.presentDeep)
                    isShort -> BorderStroke(1.dp, AppTheme.colors.short)
                    isLeave -> BorderStroke(1.dp, AppTheme.colors.rule)
                    else -> null
                }

                val dayNumberColor = when {
                    isPresent -> AppTheme.colors.presentDeep
                    isShort -> AppTheme.colors.short
                    else -> AppTheme.colors.ink
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = day.dayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isToday) AppTheme.colors.presentDeep else AppTheme.colors.muted,
                        fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal
                    )

                    Surface(
                        shape = WeekTileShape,
                        color = tileBackground,
                        border = tileBorder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .semantics {
                                this.contentDescription = markerDescription
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = dayNumber,
                                style = MaterialTheme.typography.labelLarge,
                                color = dayNumberColor,
                                fontWeight = if (isToday || isPresent) FontWeight.SemiBold else FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            when {
                                isPresent -> Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = AppTheme.colors.presentDeep,
                                    modifier = Modifier.size(14.dp)
                                )
                                isShort -> Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = AppTheme.colors.short,
                                    modifier = Modifier.size(14.dp)
                                )
                                isLeave -> Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = null,
                                    tint = AppTheme.colors.muted,
                                    modifier = Modifier.size(14.dp)
                                )
                                else -> Spacer(modifier = Modifier.height(14.dp))
                            }
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

@Composable
fun DashboardArrivalHistogram(
    arrivalBins: List<DateUtils.ArrivalBin>,
    modifier: Modifier = Modifier
) {
    val hasData = arrivalBins.any { it.count > 0 }
    val maxCount = arrivalBins.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    val peakBin = arrivalBins.maxByOrNull { it.count }

    Surface(
        shape = SurfaceGroupShape,
        color = AppTheme.colors.raised,
        border = wfoCardBorder(),
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
                if (hasData && peakBin != null && peakBin.count > 0) {
                    Text(
                        text = "Peak: ${peakBin.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTheme.colors.muted
                    )
                }
            }

            if (!hasData) {
                WfoEmptyState(
                    illustration = WfoIllustrationKind.OFFICE_BUILDING,
                    body = "Arrival times appear after your first punch in."
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    arrivalBins.forEach { bin ->
                        val isPeak = bin.count == peakBin?.count && bin.count > 0
                        val fraction = (bin.count.toFloat() / maxCount.toFloat()).coerceIn(0f, 1f)

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = bin.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppTheme.colors.ink
                                )
                                Text(
                                    text = "${bin.count}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isPeak) AppTheme.colors.presentDeep else AppTheme.colors.muted,
                                    fontWeight = if (isPeak) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(CalendarCellShape)
                                    .background(AppTheme.colors.ground)
                            ) {
                                if (bin.count > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(fraction)
                                            .fillMaxHeight()
                                            .clip(CalendarCellShape)
                                            .background(
                                                if (isPeak) AppTheme.colors.present else AppTheme.colors.muted
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardWeeklyHoursTrend(
    weeklyTrends: List<DateUtils.TrendPoint>,
    modifier: Modifier = Modifier
) {
    val hasData = weeklyTrends.any { it.actualHours > 0.0 }
    val maxHours = weeklyTrends.maxOfOrNull { maxOf(it.actualHours, it.targetHours) }?.coerceAtLeast(1.0) ?: 24.0
    val targetThreshold = weeklyTrends.firstOrNull()?.targetHours ?: 24.0
    val plotHeight = 100.dp

    Surface(
        shape = SurfaceGroupShape,
        color = AppTheme.colors.raised,
        border = wfoCardBorder(),
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
                if (hasData) {
                    Text(
                        text = String.format(Locale.US, "Target %.0fh", targetThreshold),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTheme.colors.muted
                    )
                }
            }

            if (!hasData) {
                WfoEmptyState(
                    illustration = WfoIllustrationKind.BAR_CHART,
                    body = "Weekly hours appear after your first punch out."
                )
            } else {
                // Value labels row: only shown when hours > 0; zero weeks stay empty
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    weeklyTrends.forEach { point ->
                        val hasHours = point.actualHours > 0.0
                        val isMet = point.actualHours >= point.targetHours
                        Text(
                            text = if (hasHours) String.format(Locale.US, "%.1fh", point.actualHours) else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isMet) AppTheme.colors.presentDeep else AppTheme.colors.short,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Plot area in one coordinate space
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(plotHeight)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        weeklyTrends.forEach { point ->
                            val isMet = point.actualHours >= point.targetHours
                            val fraction = (point.actualHours / maxHours).toFloat().coerceIn(0f, 1f)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                if (point.actualHours > 0.0) {
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
                    }

                    val targetLineColor = AppTheme.colors.muted.copy(alpha = 0.7f)
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

                // Bottom row: week labels
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
}
