package com.example.ui.screens

import android.content.Context
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.R
import com.example.data.local.CheckInLog
import com.example.data.local.LeaveHoliday
import com.example.ui.components.ArrivedEarlierBottomSheet
import com.example.ui.components.DashboardArrivalHistogram
import com.example.ui.components.DashboardCalculations
import com.example.ui.components.DashboardGreeting
import com.example.ui.components.DashboardHeroQuota
import com.example.ui.components.DashboardPunchControl
import com.example.ui.components.DashboardThisWeek
import com.example.ui.components.DashboardWeeklyHoursTrend
import com.example.ui.components.DateActionDialog
import com.example.ui.components.GreetingTimeOfDay
import com.example.ui.components.LeavesManagementDialog
import com.example.ui.components.ManualLogDialog
import com.example.ui.components.ManualLogTab
import com.example.ui.components.WeeklyStreakBreakdownDialog
import com.example.ui.components.WfoChip
import com.example.ui.components.WfoListRow
import com.example.ui.components.WfoPrimaryButton
import com.example.ui.components.WfoSectionHeader
import com.example.ui.components.WfoTextButton
import com.example.ui.components.WfoUndoBanner
import com.example.ui.theme.AppTheme
import com.example.ui.theme.NightSceneInk
import com.example.ui.theme.SurfaceGroupShape
import com.example.ui.viewmodel.WfoUiState
import com.example.ui.viewmodel.WfoViewModel
import com.example.util.DateUtils
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: WfoUiState,
    viewModel: WfoViewModel,
    scrollState: ScrollState = rememberScrollState(),
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    var showArrivedEarlierSheet by remember { mutableStateOf(false) }
    var showLeavesDialog by remember { mutableStateOf(false) }

    // Dialog state for day actions or manual logging
    var selectedActionDateKey by remember { mutableStateOf<String?>(null) }
    var showManualDialog by remember { mutableStateOf(false) }
    var manualDialogInitialTab by remember { mutableStateOf(ManualLogTab.ATTENDANCE) }
    var manualDialogInitialDate by remember { mutableStateOf<String?>(null) }
    var manualDialogInitialLog by remember { mutableStateOf<CheckInLog?>(null) }
    var manualDialogInitialLeave by remember { mutableStateOf<LeaveHoliday?>(null) }

    // Passive Geofence Arrival Prompt Dialog
    if (uiState.showArrivalPrompt) {
        var selectedArrivalOffset by remember { mutableIntStateOf(0) }

        AlertDialog(
            onDismissRequest = { viewModel.dismissArrivalPrompt() },
            icon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = AppTheme.colors.presentDeep) },
            title = {
                Text(
                    text = "Arrived at office",
                    style = MaterialTheme.typography.titleMedium,
                    color = AppTheme.colors.ink
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "You arrived within office coordinates. Punch in now?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.colors.ink
                    )
                    Text(
                        text = "Transit offset:",
                        style = MaterialTheme.typography.labelLarge,
                        color = AppTheme.colors.muted
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0 to "Now", 10 to "10m", 15 to "15m", 30 to "30m").forEach { (mins, label) ->
                            val isSelected = selectedArrivalOffset == mins
                            Box(modifier = Modifier.weight(1f)) {
                                WfoChip(
                                    text = label,
                                    selected = isSelected,
                                    onClick = { selectedArrivalOffset = mins },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                WfoPrimaryButton(
                    text = if (selectedArrivalOffset > 0) "Punch in (${selectedArrivalOffset}m)" else "Punch in",
                    onClick = {
                        viewModel.confirmArrivalPunchIn(context, selectedArrivalOffset)
                    }
                )
            },
            dismissButton = {
                WfoTextButton(
                    text = "Dismiss",
                    onClick = { viewModel.dismissArrivalPrompt() }
                )
            },
            shape = SurfaceGroupShape,
            containerColor = AppTheme.colors.sheet,
            textContentColor = AppTheme.colors.ink,
            titleContentColor = AppTheme.colors.ink
        )
    }

    // Departure Reminder Prompt Dialog
    if (uiState.showDeparturePrompt) {
        var selectedBackdateMinutes by remember { mutableIntStateOf(0) }

        AlertDialog(
            onDismissRequest = { viewModel.dismissDeparturePrompt() },
            icon = { Icon(Icons.Default.Notifications, contentDescription = null, tint = AppTheme.colors.short) },
            title = {
                Text(
                    text = "Departure reminder",
                    style = MaterialTheme.typography.titleMedium,
                    color = AppTheme.colors.ink
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Your workday session exceeded the target threshold. Did you leave earlier?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.colors.ink
                    )
                    Text(
                        text = "Backdate punch out time:",
                        style = MaterialTheme.typography.labelLarge,
                        color = AppTheme.colors.muted
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0 to "Now", 15 to "-15m", 30 to "-30m", 45 to "-45m").forEach { (mins, label) ->
                            val isSelected = selectedBackdateMinutes == mins
                            Box(modifier = Modifier.weight(1f)) {
                                WfoChip(
                                    text = label,
                                    selected = isSelected,
                                    onClick = { selectedBackdateMinutes = mins },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                WfoPrimaryButton(
                    text = "Punch out",
                    onClick = {
                        viewModel.requestPunchOut(context, -selectedBackdateMinutes)
                        viewModel.dismissDeparturePrompt()
                    }
                )
            },
            dismissButton = {
                WfoTextButton(
                    text = "Still working",
                    onClick = { viewModel.dismissDeparturePrompt() }
                )
            },
            shape = SurfaceGroupShape,
            containerColor = AppTheme.colors.sheet,
            textContentColor = AppTheme.colors.ink,
            titleContentColor = AppTheme.colors.ink
        )
    }

    // OEM Battery Optimization Nudge Dialog
    if (uiState.showBatteryOptNudge) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissBatteryOptimizationNudge() },
            icon = { Icon(Icons.Default.BatteryAlert, contentDescription = null, tint = AppTheme.colors.short) },
            title = {
                Text(
                    text = "Battery optimization",
                    style = MaterialTheme.typography.titleMedium,
                    color = AppTheme.colors.ink
                )
            },
            text = {
                Text(
                    text = "To ensure office arrival detection and live timers run reliably in the background, allow WFO Tracker to run unrestricted.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTheme.colors.ink
                )
            },
            confirmButton = {
                WfoPrimaryButton(
                    text = "Disable optimization",
                    onClick = {
                        viewModel.dismissBatteryOptimizationNudge()
                        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && powerManager != null && !powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
                            try {
                                val intent = android.content.Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                    }
                )
            },
            dismissButton = {
                WfoTextButton(
                    text = "Later",
                    onClick = { viewModel.dismissBatteryOptimizationNudge() }
                )
            },
            shape = SurfaceGroupShape,
            containerColor = AppTheme.colors.sheet,
            textContentColor = AppTheme.colors.ink,
            titleContentColor = AppTheme.colors.ink
        )
    }

    // Leaves Management Dialog
    if (showLeavesDialog) {
        LeavesManagementDialog(
            leaves = uiState.leaves,
            onAddLeaveRange = { from, to, type, title, onResult ->
                viewModel.addLeaveRange(from, to, type, title, onResult)
            },
            onDeleteLeave = { id ->
                viewModel.deleteLeave(id)
            },
            onImportHolidays = { holidays, onInserted ->
                viewModel.importHolidaysBatch(holidays, onInserted)
            },
            onDeleteLeavesBatch = { ids ->
                viewModel.deleteLeavesBatch(ids)
            },
            onDismiss = { showLeavesDialog = false }
        )
    }

    // Weekly Streak Breakdown Dialog
    if (uiState.showStreakBreakdownDialog) {
        WeeklyStreakBreakdownDialog(
            uiState = uiState,
            onDismiss = { viewModel.openStreakBreakdown(false) }
        )
    }

    // Date Action Dialog (when triggered from other flows)
    val actionDateKey = selectedActionDateKey
    if (actionDateKey != null) {
        val logForDate = uiState.logs.find { it.date == actionDateKey }
        val leaveForDate = uiState.leaves.find { it.date == actionDateKey }

        DateActionDialog(
            dateKey = actionDateKey,
            log = logForDate,
            leave = leaveForDate,
            minHours = uiState.settings.minHours,
            onDismiss = { selectedActionDateKey = null },
            onLogAttendanceClick = {
                manualDialogInitialTab = ManualLogTab.ATTENDANCE
                manualDialogInitialDate = actionDateKey
                manualDialogInitialLog = logForDate
                manualDialogInitialLeave = null
                selectedActionDateKey = null
                showManualDialog = true
            },
            onLogLeaveClick = {
                manualDialogInitialTab = ManualLogTab.LEAVE_HOLIDAY
                manualDialogInitialDate = actionDateKey
                manualDialogInitialLog = null
                manualDialogInitialLeave = leaveForDate
                selectedActionDateKey = null
                showManualDialog = true
            },
            onDeleteLog = {
                viewModel.deleteLog(actionDateKey)
                selectedActionDateKey = null
            },
            onDeleteLeave = if (leaveForDate != null) {
                {
                    viewModel.deleteLeave(leaveForDate.id)
                    selectedActionDateKey = null
                }
            } else null
        )
    }

    // Manual Log Dialog
    if (showManualDialog) {
        val existingLogDates = remember(uiState.logs) { uiState.logs.map { it.date }.toSet() }
        ManualLogDialog(
            initialDate = manualDialogInitialDate ?: uiState.selectedCalendarDateKey,
            initialLog = manualDialogInitialLog,
            initialLeave = manualDialogInitialLeave,
            initialTab = manualDialogInitialTab,
            existingLogDates = existingLogDates,
            onSaveLog = { d, inTime, outTime, notes ->
                viewModel.saveManualLog(d, inTime, outTime, notes)
                showManualDialog = false
            },
            onSaveLeave = { from, to, type, title ->
                viewModel.addLeaveRange(from, to, type, title)
                showManualDialog = false
            },
            onDismiss = { showManualDialog = false }
        )
    }

    // Arrived Earlier Bottom Sheet
    if (showArrivedEarlierSheet) {
        ArrivedEarlierBottomSheet(
            onConfirmPunchIn = { startMillis ->
                showArrivedEarlierSheet = false
                viewModel.requestPunchIn(context, startMillis = startMillis)
            },
            onDismissRequest = {
                showArrivedEarlierSheet = false
            }
        )
    }

    // Pull-to-refresh container
    val pullRefreshState = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = uiState.isPullRefreshing,
        onRefresh = { viewModel.pullToRefresh(context) },
        state = pullRefreshState,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullRefreshState,
                isRefreshing = uiState.isPullRefreshing,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.statusBars)
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.ground)
    ) {
        val nowCal = remember(uiState.selectedCalendarDateKey, uiState.logs.size) {
            Calendar.getInstance()
        }
        val currentHour = nowCal.get(Calendar.HOUR_OF_DAY)
        val greetingTimeOfDay = remember(currentHour) {
            DashboardCalculations.timeOfDay(currentHour)
        }
        val (greetingLine, firstName) = remember(uiState.settings.employeeName, currentHour) {
            DashboardCalculations.greetingParts(
                employeeName = uiState.settings.employeeName,
                hourOfDay = currentHour
            )
        }

        val isDark = AppTheme.colors.isDark
        val sceneDrawableRes = when {
            isDark -> R.drawable.wfo_header_landscape_night_1800x1000
            greetingTimeOfDay == GreetingTimeOfDay.MORNING -> R.drawable.wfo_header_landscape_1800x1000
            greetingTimeOfDay == GreetingTimeOfDay.AFTERNOON -> R.drawable.wfo_header_landscape_afternoon_1800x1000
            else -> R.drawable.wfo_header_landscape_evening_1800x1000
        }
        val isEveningLightScene = !isDark && greetingTimeOfDay == GreetingTimeOfDay.EVENING
        val greetingTextColor = when {
            isDark -> NightSceneInk
            isEveningLightScene -> Color.White
            else -> AppTheme.colors.ink
        }
        val greetingLinkColor = if (isEveningLightScene) Color.White else AppTheme.colors.presentDeep
        val heroOverlapPx = with(LocalDensity.current) { 32.dp.roundToPx() }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // 0. FULL-WIDTH HEADER SCENE WITH GREETING
            Box(modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(id = sceneDrawableRes),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.8f)
                )

                if (isEveningLightScene) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.horizontalGradient(
                                    colorStops = arrayOf(
                                        0.0f to Color.Black.copy(alpha = 0.48f),
                                        0.45f to Color.Black.copy(alpha = 0.42f),
                                        0.78f to Color.Transparent
                                    )
                                )
                            )
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(top = 64.dp, start = 16.dp, end = 16.dp, bottom = 44.dp)
                ) {
                    DashboardGreeting(
                        greetingLine = greetingLine,
                        firstName = firstName,
                        textColor = greetingTextColor,
                        linkColor = greetingLinkColor,
                        onAddNameClick = onNavigateToSettings
                    )
                }
            }

            // MAIN DASHBOARD CONTENT
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. HERO QUOTA RING COMPONENT (overlaps bottom of scene by ~32dp)
                val monthNames = DateFormatSymbols.getInstance(Locale.US).months
                val monthName = monthNames.getOrElse(uiState.dashboardNavMonth) { "This month" }
                val monthKey = String.format(
                    Locale.US,
                    "%04d-%02d",
                    uiState.dashboardNavYear,
                    uiState.dashboardNavMonth + 1
                )

                val paceMessage = remember(
                    monthName,
                    uiState.validDaysThisMonth,
                    uiState.requiredDaysThisMonth,
                    uiState.settings.targetDaysPerWeek,
                    uiState.dashboardNavYear,
                    uiState.dashboardNavMonth,
                    uiState.logs,
                    uiState.settings.minHours
                ) {
                    DashboardCalculations.computeHeroMessage(
                        monthName = monthName,
                        validDays = uiState.validDaysThisMonth,
                        requiredDays = uiState.requiredDaysThisMonth,
                        targetDaysPerWeek = uiState.settings.targetDaysPerWeek,
                        navYear = uiState.dashboardNavYear,
                        navMonth = uiState.dashboardNavMonth,
                        logs = uiState.logs,
                        minHours = uiState.settings.minHours
                    )
                }

                val isCurrentCalendarMonth = uiState.dashboardNavYear == nowCal.get(Calendar.YEAR) &&
                    uiState.dashboardNavMonth == nowCal.get(Calendar.MONTH)

                DashboardHeroQuota(
                    monthName = monthName,
                    monthKey = monthKey,
                    isCurrentCalendarMonth = isCurrentCalendarMonth,
                    lastCelebratedMonthKey = uiState.settings.lastCelebratedMonthKey,
                    validDaysThisMonth = uiState.validDaysThisMonth,
                    requiredDaysThisMonth = uiState.requiredDaysThisMonth,
                    paceMessage = paceMessage,
                    weeklyStreak = uiState.weeklyStreak,
                    onStreakClick = { viewModel.openStreakBreakdown(true) },
                    onMonthCelebrated = { celebratedKey -> viewModel.markMonthCelebrated(celebratedKey) },
                    modifier = Modifier.layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val reportedHeight = (placeable.height - heroOverlapPx).coerceAtLeast(0)
                        layout(placeable.width, reportedHeight) {
                            placeable.placeRelative(0, -heroOverlapPx)
                        }
                    }
                )

                // Persistent Undo Punch Out Banner
                AnimatedVisibility(
                    visible = uiState.undoAvailable && uiState.undoMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    WfoUndoBanner(
                        message = uiState.undoMessage ?: "Session logged.",
                        onUndoClick = { viewModel.executeUndoPunchOut(context) },
                        onDismissClick = { viewModel.dismissUndo() }
                    )
                }

                HorizontalDivider(color = AppTheme.colors.rule, thickness = 1.dp)

                // 2. PUNCH CONTROL (Sitting directly on Ground without Surface container)
                DashboardPunchControl(
                    isPunchedIn = uiState.isPunchedIn,
                    elapsedSeconds = uiState.elapsedSeconds,
                    minHours = uiState.settings.minHours,
                    targetReached = uiState.targetReached,
                    targetTimeFormatted = uiState.targetTimeFormatted,
                    countdownMode = uiState.settings.timerCountdownMode,
                    onToggleCountdownMode = { viewModel.toggleCountdownMode() },
                    onPunchInClick = {
                        viewModel.requestPunchIn(context)
                    },
                    onPunchOutClick = { viewModel.requestPunchOut(context) },
                    onArrivedEarlierClick = { showArrivedEarlierSheet = true }
                )

                HorizontalDivider(color = AppTheme.colors.rule, thickness = 1.dp)

                // 3. THIS WEEK (Sitting directly on Ground without Surface container)
                DashboardThisWeek(
                    weekBreakdown = uiState.currentWeekBreakdown,
                    validDaysThisWeek = uiState.validDaysThisWeek,
                    targetDaysPerWeek = uiState.settings.targetDaysPerWeek,
                    todayDateKey = DateUtils.todayDateStr()
                )

                HorizontalDivider(color = AppTheme.colors.rule, thickness = 1.dp)

                // 4. EXCLUSIONS ROW
                WfoListRow(
                    title = "Exclusions",
                    subtitle = "${uiState.currentMonthLeaves.size} days",
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = AppTheme.colors.presentDeep,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = AppTheme.colors.muted
                        )
                    },
                    onClick = { showLeavesDialog = true },
                    showDivider = false,
                    horizontalPadding = 0.dp
                )

                // 5. INSIGHTS
                WfoSectionHeader(
                    title = "Insights",
                    horizontalPadding = 0.dp
                )

                DashboardArrivalHistogram(
                    arrivalBins = uiState.arrivalBins
                )

                DashboardWeeklyHoursTrend(
                    weeklyTrends = uiState.weeklyTrends
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
