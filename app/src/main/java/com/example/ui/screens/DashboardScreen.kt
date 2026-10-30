package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HistoryToggleOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CheckInLog
import com.example.data.local.LeaveHoliday
import com.example.ui.components.ArrivalHistogramCard
import com.example.ui.components.CircularProgressTimer
import com.example.ui.components.DailyStatusTimelineCard
import com.example.ui.components.DateActionDialog
import com.example.ui.components.DateTimePickerField
import com.example.ui.components.ForgotPunchInDialog
import com.example.ui.components.HoursTrendCard
import com.example.ui.components.LeavesManagementDialog
import com.example.ui.components.ManualLogDialog
import com.example.ui.components.ManualLogTab
import com.example.ui.components.MonthCalendarCard
import com.example.ui.components.PunchInWithNoteDialog
import com.example.ui.components.TimerQuickActionMenuDialog
import com.example.ui.components.WeeklyStreakBreakdownDialog
import com.example.ui.components.showNativeTimePicker
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.PurpleExclusion
import com.example.ui.viewmodel.WfoUiState
import com.example.ui.viewmodel.WfoViewModel
import com.example.util.BatteryOptimizationHelper
import com.example.util.DateUtils
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: WfoUiState,
    viewModel: WfoViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var showLeavesDialog by remember { mutableStateOf(false) }
    var showTransitInfo by remember { mutableStateOf(false) }
    var selectedActionDateKey by remember { mutableStateOf<String?>(null) }
    var showManualDialog by remember { mutableStateOf(false) }
    var manualDialogInitialTab by remember { mutableStateOf(ManualLogTab.ATTENDANCE) }
    var manualDialogInitialDate by remember { mutableStateOf(DateUtils.todayDateStr()) }
    var manualDialogInitialLog by remember { mutableStateOf<CheckInLog?>(null) }
    var manualDialogInitialLeave by remember { mutableStateOf<LeaveHoliday?>(null) }

    // Geofence alert dialog
    if (uiState.geofenceAlertMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissGeofenceAlert() },
            icon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = AmberWarning) },
            title = { Text("GPS Geofence Notice") },
            text = {
                Text(
                    uiState.geofenceAlertMessage ?: "You are currently outside your configured office radius.",
                    fontSize = 13.5.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearGeofenceAlert()
                        viewModel.requestPunchIn(context, bypassGeofence = true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    Text("Punch In Anyway (Override)")
                }
            },
            dismissButton = {
                Button(
                    onClick = { viewModel.dismissGeofenceAlert() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        )
    }

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

    // Passive Geofence Arrival Prompt Dialog
    if (uiState.showArrivalPrompt) {
        var selectedArrivalOffset by remember { mutableStateOf(0) }

        AlertDialog(
            onDismissRequest = { viewModel.dismissArrivalPrompt() },
            icon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = GreenSuccess) },
            title = { Text("Welcome to the Office!") },
            text = {
                Column {
                    Text(
                        "You've arrived at the office coordinates. Would you like to punch in now?",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Would you like to backdate start time with a transit offset?",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0 to "Now (0m)", 10 to "+10m", 15 to "+15m", 30 to "+30m").forEach { (mins, label) ->
                            val isSelected = selectedArrivalOffset == mins
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) BluePrimary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedArrivalOffset = mins }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.confirmArrivalPunchIn(context, selectedArrivalOffset)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    Text(if (selectedArrivalOffset > 0) "Punch In (+${selectedArrivalOffset}m)" else "Punch In (Now)")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.dismissArrivalPrompt() }
                ) {
                    Text("Not Yet")
                }
            }
        )
    }

    // Daily Departure Reminder Prompt Dialog
    if (uiState.showDeparturePrompt) {
        var selectedBackdateMinutes by remember { mutableStateOf(0) }

        AlertDialog(
            onDismissRequest = { viewModel.dismissDeparturePrompt() },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = AmberWarning) },
            title = { Text("Still at the Office?") },
            text = {
                Column {
                    Text(
                        "Your session has exceeded your departure reminder threshold. Did you leave earlier?",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Did you leave earlier? Backdate punch out time:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0 to "Left Just Now", 15 to "15m Ago", 30 to "30m Ago", 60 to "1h Ago").forEach { (mins, label) ->
                            val isSelected = selectedBackdateMinutes == mins
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) BluePrimary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedBackdateMinutes = mins }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissDeparturePrompt()
                        viewModel.requestPunchOut(context, addTransitMinutes = -selectedBackdateMinutes)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonDanger)
                ) {
                    Text("Punch Out Now")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.dismissDeparturePrompt() }
                ) {
                    Text("Still Working")
                }
            }
        )
    }

    // Modal Dialogs for Contextual Long Presses & Shortcuts
    if (uiState.showStreakBreakdownDialog) {
        WeeklyStreakBreakdownDialog(
            uiState = uiState,
            onDismiss = { viewModel.openStreakBreakdown(false) }
        )
    }

    if (uiState.showTimerQuickActionMenu) {
        TimerQuickActionMenuDialog(
            activeStartMillis = uiState.activeStartMillis,
            onDismiss = { viewModel.openTimerQuickActionMenu(false) },
            onAdjustStartTime = { h, m ->
                viewModel.adjustActiveSessionStart(context, h, m)
            },
            onMarkAsWfh = {
                viewModel.markTodayAsWfh(context)
            },
            onSaveNote = { _ ->
                // note saved
            }
        )
    }

    if (uiState.showPunchInWithNoteDialog) {
        PunchInWithNoteDialog(
            initialOffsetMinutes = uiState.transitOffsetMinutes,
            onDismiss = { viewModel.openPunchInWithNoteDialog(false) },
            onConfirmPunchIn = { note, offset ->
                viewModel.setTransitOffsetMinutes(offset)
                viewModel.requestPunchIn(context, initialNote = note)
            }
        )
    }

    if (uiState.showForgotPunchInDialog) {
        ForgotPunchInDialog(
            shiftStart = uiState.settings.shiftStart,
            onDismiss = { viewModel.openForgotPunchInDialog(false) },
            onConfirmBackfill = { h, m ->
                viewModel.backfillPunchInToday(context, h, m)
            }
        )
    }

    PullToRefreshBox(
        isRefreshing = uiState.isPullRefreshing,
        onRefresh = { viewModel.pullToRefresh(context) },
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // OEM Battery Optimization Nudge Banner
            if (uiState.showBatteryOptNudge) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AmberWarning.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberWarning.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.BatteryAlert, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Prevent Timer Service Being Killed",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(
                                onClick = { viewModel.dismissBatteryOptimizationNudge() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "OEM systems (Xiaomi, Samsung, OnePlus, Oppo) kill background live timers. Whitelist WFO Tracker to guarantee uninterrupted tracking.",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    BatteryOptimizationHelper.requestIgnoreBatteryOptimization(context)
                                    viewModel.dismissBatteryOptimizationNudge()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AmberWarning),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Whitelist App", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                            OutlinedButton(
                                onClick = { viewModel.dismissBatteryOptimizationNudge() },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Dismiss", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Persistent Undo Punch Out Banner with Dismiss Button
            AnimatedVisibility(
                visible = uiState.undoAvailable && uiState.undoMessage != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = BluePrimary.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BluePrimary.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.undoMessage ?: "Session logged.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = { viewModel.executeUndoPunchOut(context) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Undo", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
                            }
                            IconButton(
                                onClick = { viewModel.dismissUndo() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Top Section: Live Punch In/Out Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressTimer(
                        isPunchedIn = uiState.isPunchedIn,
                        elapsedSeconds = uiState.elapsedSeconds,
                        targetHours = uiState.settings.minHours,
                        countdownMode = uiState.countdownMode,
                        targetReached = uiState.targetReached,
                        onToggleCountdown = { viewModel.toggleCountdownMode() },
                        onPunchClick = {
                            if (uiState.isPunchedIn) {
                                viewModel.requestPunchOut(context, addTransitMinutes = 0)
                            } else {
                                viewModel.requestPunchIn(context)
                            }
                        },
                        onLongClick = {
                            if (uiState.isPunchedIn) {
                                viewModel.openTimerQuickActionMenu(true)
                            }
                        },
                        onPunchLongClick = {
                            if (!uiState.isPunchedIn) {
                                viewModel.openPunchInWithNoteDialog(true)
                            }
                        }
                    )

                    // Target or Arrival Information
                    Spacer(modifier = Modifier.height(8.dp))
                    if (uiState.isPunchedIn && uiState.targetTimeFormatted.isNotEmpty()) {
                        Text(
                            text = "🎯 Daily ${String.format(Locale.US, "%.1f", uiState.settings.minHours)}h target: ${uiState.targetTimeFormatted}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BluePrimary
                        )
                    }

                    // "Forgot to punch in?" shortcut link when not punched in
                    if (!uiState.isPunchedIn) {
                        TextButton(
                            onClick = { viewModel.openForgotPunchInDialog(true) },
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HistoryToggleOff,
                                contentDescription = null,
                                tint = BluePrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Forgot to punch in? Backdate arrival",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BluePrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Commute Offset Memory",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    IconButton(
                                        onClick = { showTransitInfo = !showTransitInfo },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = "Offset Info",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                if (showTransitInfo) {
                                    Text(
                                        text = "Choose transit minutes ago or pick an exact arrival time. Offset preference is remembered automatically.",
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(0, 5, 7, 10, 15).forEach { mins ->
                                        FilterChip(
                                            selected = uiState.transitOffsetMinutes == mins && uiState.customArrivedTimeInput.isBlank(),
                                            onClick = { viewModel.setTransitOffsetMinutes(mins) },
                                            label = { Text(if (mins == 0) "Now" else "${mins}m", fontSize = 11.sp) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    DateTimePickerField(
                                        label = "Or Arrived At (Clock Picker)",
                                        valueText = if (uiState.customArrivedTimeInput.isNotBlank()) {
                                            DateUtils.formatTime12(uiState.customArrivedTimeInput)
                                        } else {
                                            "Tap to pick arrival time..."
                                        },
                                        subtitle = if (uiState.customArrivedTimeInput.isNotBlank()) "Arrival punch will start from ${uiState.customArrivedTimeInput}" else "Set custom backdated arrival",
                                        icon = Icons.Default.Schedule,
                                        onClick = {
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
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (uiState.customArrivedTimeInput.isNotBlank()) {
                                        IconButton(
                                            onClick = { viewModel.setCustomArrivedTimeInput("") },
                                            modifier = Modifier.padding(start = 4.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear Custom Time", modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Stats Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📊 QUICK STATS & RUN RATE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    MetricRow(
                        label = "This Month's Attendance:",
                        value = "${uiState.validDaysThisMonth} / ${uiState.requiredDaysThisMonth} Days (${uiState.monthlyCompliancePercentage}%)",
                        valueColor = if (uiState.monthlyCompliancePercentage >= 100) GreenSuccess else BluePrimary
                    )

                    // Run rate calculation
                    val now = Calendar.getInstance()
                    val daysInMonth = now.getActualMaximum(Calendar.DAY_OF_MONTH)
                    val remainingDaysInMonth = maxOf(1, daysInMonth - now.get(Calendar.DAY_OF_MONTH))
                    val remainingWeeks = maxOf(0.5f, remainingDaysInMonth / 7f)
                    val deficitDays = maxOf(0, uiState.requiredDaysThisMonth - uiState.validDaysThisMonth)
                    val runRateText = if (deficitDays == 0) {
                        "Quota Met! 🎉"
                    } else {
                        String.format(Locale.US, "%.1f days / week", deficitDays / remainingWeeks)
                    }

                    MetricRow(
                        label = "WFO Frequency Run Rate:",
                        value = runRateText,
                        valueColor = if (deficitDays == 0) GreenSuccess else AmberWarning
                    )

                    MetricRow(
                        label = "Current Week Progress:",
                        value = "${uiState.validDaysThisWeek} / ${uiState.settings.targetDaysPerWeek} valid days",
                        valueColor = if (uiState.validDaysThisWeek >= uiState.settings.targetDaysPerWeek) GreenSuccess else BluePrimary
                    )

                    // Weekly streak row with click / long-press for breakdown
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { viewModel.openStreakBreakdown(true) },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Weekly Consistency Streak:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🔥 ${uiState.weeklyStreak} Weeks",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberWarning
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "(Details)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BluePrimary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Exclusions (Holidays/Leaves):",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = PurpleExclusion.copy(alpha = 0.18f),
                            modifier = Modifier.clickable { showLeavesDialog = true }
                        ) {
                            Text(
                                text = "${uiState.currentMonthLeaves.size} Days 🌴 (Manage)",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = PurpleExclusion,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Arrival Time Consistency Histogram
            ArrivalHistogramCard(arrivalBins = uiState.arrivalBins)

            Spacer(modifier = Modifier.height(14.dp))

            // Attendance & Hours Trends
            HoursTrendCard(weeklyTrends = uiState.weeklyTrends)

            Spacer(modifier = Modifier.height(14.dp))

            // Month Calendar View
            MonthCalendarCard(
                year = uiState.dashboardNavYear,
                month = uiState.dashboardNavMonth,
                logs = uiState.logs,
                leaves = uiState.leaves,
                minHours = uiState.settings.minHours,
                selectedDateKey = uiState.selectedCalendarDateKey,
                onDateSelected = { viewModel.selectCalendarDate(it) },
                onPrevMonth = { viewModel.setDashboardMonth(-1) },
                onNextMonth = { viewModel.setDashboardMonth(1) },
                onSelectMonth = { y, m -> viewModel.setDashboardYearMonth(y, m) },
                onTodayClick = { viewModel.resetToCurrentMonth() },
                onDateLongClick = { dateKey ->
                    selectedActionDateKey = dateKey
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Daily Status Timeline for selected day
            DailyStatusTimelineCard(
                dateKey = uiState.selectedCalendarDateKey,
                log = uiState.selectedDateLog,
                leave = uiState.selectedDateLeave,
                isTodayPunchedIn = uiState.isPunchedIn,
                activeStartMillis = uiState.activeStartMillis,
                minHours = uiState.settings.minHours,
                onLogActionClick = {
                    selectedActionDateKey = uiState.selectedCalendarDateKey
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Popup triggered when user long-presses any date or taps "+ Log / Edit Day"
    selectedActionDateKey?.let { actionDateKey ->
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
                showManualDialog = true
            },
            onLogLeaveClick = {
                manualDialogInitialTab = ManualLogTab.LEAVE_HOLIDAY
                manualDialogInitialDate = actionDateKey
                manualDialogInitialLog = null
                manualDialogInitialLeave = leaveForDate
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

    if (showManualDialog) {
        ManualLogDialog(
            initialTab = manualDialogInitialTab,
            initialDate = manualDialogInitialDate,
            initialLog = manualDialogInitialLog,
            initialLeave = manualDialogInitialLeave,
            onSaveLog = { d, inTime, outTime, notes ->
                viewModel.saveManualLog(d, inTime, outTime, notes)
                showManualDialog = false
                selectedActionDateKey = null
            },
            onSaveLeave = { from, to, type, title ->
                viewModel.addLeaveRange(from, to, type, title)
                showManualDialog = false
                selectedActionDateKey = null
            },
            onDeleteLog = { d ->
                viewModel.deleteLog(d)
                showManualDialog = false
                selectedActionDateKey = null
            },
            onDeleteLeave = { id ->
                viewModel.deleteLeave(id)
                showManualDialog = false
                selectedActionDateKey = null
            },
            onDismiss = {
                showManualDialog = false
                selectedActionDateKey = null
            }
        )
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}
