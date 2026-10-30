package com.example.ui.screens

import android.content.Context
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
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
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.local.CheckInLog
import com.example.data.local.LeaveHoliday
import com.example.ui.components.ArrivedEarlierBottomSheet
import com.example.ui.components.DashboardArrivalHistogram
import com.example.ui.components.DashboardHeroQuota
import com.example.ui.components.DashboardPunchControl
import com.example.ui.components.DashboardThisWeek
import com.example.ui.components.DashboardWeeklyHoursTrend
import com.example.ui.components.DateActionDialog
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
import com.example.ui.viewmodel.WfoUiState
import com.example.ui.viewmodel.WfoViewModel
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: WfoUiState,
    viewModel: WfoViewModel
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
            icon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = AppTheme.colors.present) },
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
            containerColor = AppTheme.colors.raised,
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
            containerColor = AppTheme.colors.raised,
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
            containerColor = AppTheme.colors.raised,
            textContentColor = AppTheme.colors.ink,
            titleContentColor = AppTheme.colors.ink
        )
    }

    // Leaves Management Dialog
    if (showLeavesDialog) {
        LeavesManagementDialog(
            leaves = uiState.leaves,
            onAddLeaveRange = { from, to, type, title ->
                viewModel.addLeaveRange(from, to, type, title)
            },
            onDeleteLeave = { id ->
                viewModel.deleteLeave(id)
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
            selectedOffsetMinutes = uiState.transitOffsetMinutes,
            customTimeInput = uiState.customArrivedTimeInput,
            onOffsetSelected = { mins ->
                viewModel.setTransitOffsetMinutes(mins)
            },
            onPickTimeClick = {
                val defaultTime = if (uiState.customArrivedTimeInput.isNotBlank()) {
                    uiState.customArrivedTimeInput
                } else {
                    val c = Calendar.getInstance()
                    String.format(Locale.US, "%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
                }
                showNativeTimePicker(context, defaultTime) { picked ->
                    viewModel.setCustomArrivedTimeInput(picked)
                }
            },
            onClearCustomTime = {
                viewModel.setCustomArrivedTimeInput("")
            },
            onConfirmPunchIn = {
                showArrivedEarlierSheet = false
                viewModel.requestPunchIn(context)
            },
            onDismissRequest = {
                // Correctness 1: reset transit offset to 0 and clear custom time when dismissed
                showArrivedEarlierSheet = false
                viewModel.setTransitOffsetMinutes(0)
                viewModel.setCustomArrivedTimeInput("")
            }
        )
    }

    // Pull-to-refresh container
    val pullRefreshState = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = uiState.isPullRefreshing,
        onRefresh = { viewModel.pullToRefresh(context) },
        state = pullRefreshState,
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.ground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
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

            // 1. HERO QUOTA COMPONENT (Sitting directly on Ground without Surface container)
            val monthNames = DateFormatSymbols.getInstance(Locale.US).months
            val monthName = monthNames.getOrElse(uiState.dashboardNavMonth) { "This month" }

            val remainingDays = uiState.requiredDaysThisMonth - uiState.validDaysThisMonth
            val paceSentence = when {
                remainingDays <= 0 -> "Quota met"
                else -> "$remainingDays more days needed, about 3 a week"
            }

            DashboardHeroQuota(
                monthName = monthName,
                validDaysThisMonth = uiState.validDaysThisMonth,
                requiredDaysThisMonth = uiState.requiredDaysThisMonth,
                underMinimumDays = 0, // Passed as 0 per requirement 2; underMinimum not in WfoUiState
                minHours = uiState.settings.minHours,
                paceSentence = paceSentence
            )

            HorizontalDivider(color = AppTheme.colors.rule, thickness = 1.dp)

            // 2. PUNCH CONTROL (Sitting directly on Ground without Surface container)
            DashboardPunchControl(
                isPunchedIn = uiState.isPunchedIn,
                elapsedSeconds = uiState.elapsedSeconds,
                minHours = uiState.settings.minHours,
                targetReached = uiState.targetReached,
                targetTimeFormatted = uiState.targetTimeFormatted,
                onPunchInClick = { viewModel.requestPunchIn(context) },
                onPunchOutClick = { viewModel.requestPunchOut(context) },
                onArrivedEarlierClick = { showArrivedEarlierSheet = true }
            )

            HorizontalDivider(color = AppTheme.colors.rule, thickness = 1.dp)

            // 3. THIS WEEK (Sitting directly on Ground without Surface container)
            DashboardThisWeek(
                weekBreakdown = uiState.currentWeekBreakdown,
                validDaysThisWeek = uiState.validDaysThisWeek,
                targetDaysPerWeek = uiState.settings.targetDaysPerWeek,
                todayDateKey = uiState.selectedCalendarDateKey
            )

            HorizontalDivider(color = AppTheme.colors.rule, thickness = 1.dp)

            // 4. STREAK AND EXCLUSIONS (Plain list rows on Ground, separated by Rule lines, no border box)
            Column(modifier = Modifier.fillMaxWidth()) {
                WfoListRow(
                    title = "Weekly streak",
                    subtitle = "${uiState.weeklyStreak} weeks",
                    trailingContent = {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = AppTheme.colors.muted
                        )
                    },
                    onClick = { viewModel.openStreakBreakdown(true) },
                    showDivider = true
                )

                WfoListRow(
                    title = "Exclusions",
                    subtitle = "${uiState.currentMonthLeaves.size} days",
                    trailingContent = {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = AppTheme.colors.muted
                        )
                    },
                    onClick = { showLeavesDialog = true },
                    showDivider = false
                )
            }

            // 5. INSIGHTS (Insight charts maintain single container)
            WfoSectionHeader(title = "Insights")

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

/**
 * Native Android TimePickerDialog helper
 */
private fun showNativeTimePicker(
    context: Context,
    initialTime: String,
    onTimeSelected: (String) -> Unit
) {
    val parts = initialTime.split(":")
    val cal = Calendar.getInstance()
    val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: cal.get(Calendar.HOUR_OF_DAY)
    val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: cal.get(Calendar.MINUTE)

    val timePicker = android.app.TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            val formatted = String.format(Locale.US, "%02d:%02d", hourOfDay, minute)
            onTimeSelected(formatted)
        },
        initialHour,
        initialMinute,
        false
    )
    timePicker.show()
}
