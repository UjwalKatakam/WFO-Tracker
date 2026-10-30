package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CheckInLog
import com.example.data.local.LeaveHoliday
import com.example.ui.components.DailyStatusTimelineCard
import com.example.ui.components.DateActionDialog
import com.example.ui.components.ManualLogDialog
import com.example.ui.components.ManualLogTab
import com.example.ui.components.MonthCalendarCard
import com.example.ui.components.MonthNavigationHeader
import com.example.ui.components.YearMatrixMonthCard
import com.example.ui.components.YearOverYearComparisonDialog
import com.example.ui.theme.AppTheme
import com.example.ui.viewmodel.WfoUiState
import com.example.ui.viewmodel.WfoViewModel
import com.example.util.DateUtils
import java.util.Calendar

enum class CalendarDisplayMode {
    MONTH_VIEW,
    YEAR_MATRIX
}

@Composable
fun CalendarArchiveScreen(
    uiState: WfoUiState,
    viewModel: WfoViewModel,
    modifier: Modifier = Modifier
) {
    var displayMode by remember { mutableStateOf(CalendarDisplayMode.MONTH_VIEW) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    // Dialog state for long-press or direct edit
    var selectedActionDateKey by remember { mutableStateOf<String?>(null) }
    var showManualDialog by remember { mutableStateOf(false) }
    var manualDialogInitialTab by remember { mutableStateOf(ManualLogTab.ATTENDANCE) }
    var manualDialogInitialDate by remember { mutableStateOf(DateUtils.todayDateStr()) }
    var manualDialogInitialLog by remember { mutableStateOf<CheckInLog?>(null) }
    var manualDialogInitialLeave by remember { mutableStateOf<LeaveHoliday?>(null) }

    var showYearOverYearDialog by remember { mutableStateOf(false) }

    if (showYearOverYearDialog) {
        YearOverYearComparisonDialog(
            uiState = uiState,
            onDismiss = { showYearOverYearDialog = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.ground)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Top Toolbar: View switcher (Month vs 12-month) and overflow menu
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Segmented switch tabs
            Surface(
                shape = CircleShape,
                color = AppTheme.colors.raised,
                border = BorderStroke(1.dp, AppTheme.colors.rule)
            ) {
                Row(
                    modifier = Modifier.padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isMonth = displayMode == CalendarDisplayMode.MONTH_VIEW
                    Surface(
                        selected = isMonth,
                        onClick = { displayMode = CalendarDisplayMode.MONTH_VIEW },
                        shape = CircleShape,
                        color = if (isMonth) AppTheme.colors.present.copy(alpha = if (AppTheme.colors.isDark) 0.2f else 0.14f) else AppTheme.colors.raised,
                        border = if (isMonth) BorderStroke(1.dp, AppTheme.colors.present) else null,
                        modifier = Modifier.semantics { role = Role.Tab }
                    ) {
                        Text(
                            text = "Month",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isMonth) AppTheme.colors.present else AppTheme.colors.muted,
                            fontWeight = if (isMonth) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    val isMatrix = displayMode == CalendarDisplayMode.YEAR_MATRIX
                    Surface(
                        selected = isMatrix,
                        onClick = { displayMode = CalendarDisplayMode.YEAR_MATRIX },
                        shape = CircleShape,
                        color = if (isMatrix) AppTheme.colors.present.copy(alpha = if (AppTheme.colors.isDark) 0.2f else 0.14f) else AppTheme.colors.raised,
                        border = if (isMatrix) BorderStroke(1.dp, AppTheme.colors.present) else null,
                        modifier = Modifier.semantics { role = Role.Tab }
                    ) {
                        Text(
                            text = "12-month matrix",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isMatrix) AppTheme.colors.present else AppTheme.colors.muted,
                            fontWeight = if (isMatrix) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Overflow menu for Year-over-Year audit
            Box {
                IconButton(
                    onClick = { showOverflowMenu = true },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = AppTheme.colors.ink
                    )
                }

                DropdownMenu(
                    expanded = showOverflowMenu,
                    onDismissRequest = { showOverflowMenu = false },
                    modifier = Modifier.background(AppTheme.colors.raised)
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Year-over-year audit",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppTheme.colors.ink
                            )
                        },
                        onClick = {
                            showOverflowMenu = false
                            showYearOverYearDialog = true
                        }
                    )
                }
            }
        }

        if (displayMode == CalendarDisplayMode.MONTH_VIEW) {
            // Month View: Single navigation header + Month grid + Selected day details
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Unified month navigation control
                item {
                    MonthNavigationHeader(
                        year = uiState.selectedArchiveYear,
                        month = uiState.selectedArchiveMonth,
                        onPrevMonth = { viewModel.setArchiveMonth(-1) },
                        onNextMonth = { viewModel.setArchiveMonth(1) },
                        onSelectYearMonth = { y, m -> viewModel.setArchiveYearMonth(y, m) },
                        onTodayClick = { viewModel.resetArchiveToToday() }
                    )
                }

                // Month Calendar Grid with 8th column weekly quota marker
                item {
                    MonthCalendarCard(
                        year = uiState.selectedArchiveYear,
                        month = uiState.selectedArchiveMonth,
                        logs = uiState.logs,
                        leaves = uiState.leaves,
                        minHours = uiState.settings.minHours,
                        targetDaysPerWeek = uiState.settings.targetDaysPerWeek,
                        selectedDateKey = uiState.selectedCalendarDateKey,
                        onDateSelected = { viewModel.selectCalendarDate(it) },
                        onDateLongClick = { dateKey -> selectedActionDateKey = dateKey }
                    )
                }

                // Selected day detail card with empty state helpful hint
                item {
                    DailyStatusTimelineCard(
                        dateKey = uiState.selectedCalendarDateKey,
                        log = uiState.selectedDateLog,
                        leave = uiState.selectedDateLeave,
                        isTodayPunchedIn = uiState.isPunchedIn,
                        activeStartMillis = uiState.activeStartMillis,
                        minHours = uiState.settings.minHours,
                        onLogActionClick = {
                            selectedActionDateKey = uiState.selectedCalendarDateKey
                        },
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                }
            }
        } else {
            // 12-Month Year Matrix View
            val yearPrefix = "${uiState.selectedArchiveYear}"
            val totalYearWfo = uiState.logs.count { it.date.startsWith(yearPrefix) && it.isValidWFODay(uiState.settings.minHours) }
            val totalYearLeaves = uiState.leaves.count { it.date.startsWith(yearPrefix) }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Year navigation row
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.setArchiveYear(uiState.selectedArchiveYear - 1) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous year",
                                tint = AppTheme.colors.ink
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${uiState.selectedArchiveYear}",
                                style = MaterialTheme.typography.titleLarge,
                                color = AppTheme.colors.ink,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = buildString {
                                    append("$totalYearWfo office days in ${uiState.selectedArchiveYear}")
                                    if (totalYearLeaves > 0) {
                                        append(" · $totalYearLeaves leaves")
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = AppTheme.colors.muted
                            )
                        }

                        IconButton(
                            onClick = { viewModel.setArchiveYear(uiState.selectedArchiveYear + 1) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next year",
                                tint = AppTheme.colors.ink
                            )
                        }
                    }
                }

                // 12 mini month cards
                itemsIndexed((0..11).toList()) { monthIndex, _ ->
                    YearMatrixMonthCard(
                        year = uiState.selectedArchiveYear,
                        month = monthIndex,
                        logs = uiState.logs,
                        leaves = uiState.leaves,
                        minHours = uiState.settings.minHours,
                        selectedDateKey = uiState.selectedCalendarDateKey,
                        onDateSelected = { dateKey ->
                            viewModel.selectCalendarDate(dateKey)
                            viewModel.setArchiveYearMonth(uiState.selectedArchiveYear, monthIndex)
                        },
                        onOpenMonthClick = { y, m ->
                            viewModel.setArchiveYearMonth(y, m)
                            displayMode = CalendarDisplayMode.MONTH_VIEW
                        },
                        onDateLongClick = { dateKey ->
                            selectedActionDateKey = dateKey
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // Popup triggered when user long-presses any date or taps "Edit day" / "Log day"
    selectedActionDateKey?.let { actionDateKey ->
        val logForDate = uiState.logs.find { it.date == actionDateKey }
        val leaveForDate = uiState.leaves.find { it.date == actionDateKey }

        DateActionDialog(
            dateKey = actionDateKey,
            log = logForDate,
            leave = leaveForDate,
            minHours = uiState.settings.minHours,
            onLogAttendanceClick = {
                manualDialogInitialDate = actionDateKey
                manualDialogInitialLog = logForDate
                manualDialogInitialLeave = null
                manualDialogInitialTab = ManualLogTab.ATTENDANCE
                showManualDialog = true
            },
            onLogLeaveClick = {
                manualDialogInitialDate = actionDateKey
                manualDialogInitialLog = null
                manualDialogInitialLeave = leaveForDate
                manualDialogInitialTab = ManualLogTab.LEAVE_HOLIDAY
                showManualDialog = true
            },
            onDeleteLog = if (logForDate != null) {
                { viewModel.deleteLog(actionDateKey) }
            } else null,
            onDeleteLeave = if (leaveForDate != null) {
                { viewModel.deleteLeave(leaveForDate.id) }
            } else null,
            onDismiss = { selectedActionDateKey = null }
        )
    }

    // Manual Log / Leave unified modal
    if (showManualDialog) {
        val existingLogDates = remember(uiState.logs) { uiState.logs.map { it.date }.toSet() }
        ManualLogDialog(
            initialDate = manualDialogInitialDate,
            initialLog = manualDialogInitialLog,
            initialLeave = manualDialogInitialLeave,
            initialTab = manualDialogInitialTab,
            existingLogDates = existingLogDates,
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
