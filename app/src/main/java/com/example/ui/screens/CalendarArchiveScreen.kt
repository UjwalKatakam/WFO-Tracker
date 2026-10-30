package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarViewMonth
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.ui.components.YearMatrixMonthCard
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.PurpleExclusion
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
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val yearsList = listOf(currentYear - 1, currentYear, currentYear + 1, currentYear + 2)
    val monthShortNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    var displayMode by remember { mutableStateOf(CalendarDisplayMode.MONTH_VIEW) }

    // Dialog state for long-press or direct edit
    var selectedActionDateKey by remember { mutableStateOf<String?>(null) }
    var showManualDialog by remember { mutableStateOf(false) }
    var manualDialogInitialTab by remember { mutableStateOf(ManualLogTab.ATTENDANCE) }
    var manualDialogInitialDate by remember { mutableStateOf(DateUtils.todayDateStr()) }
    var manualDialogInitialLog by remember { mutableStateOf<CheckInLog?>(null) }
    var manualDialogInitialLeave by remember { mutableStateOf<LeaveHoliday?>(null) }

    var showYearOverYearDialog by remember { mutableStateOf(false) }

    if (showYearOverYearDialog) {
        com.example.ui.components.YearOverYearComparisonDialog(
            uiState = uiState,
            onDismiss = { showYearOverYearDialog = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "📅 CALENDAR & WFO MATRIX",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Track office attendance, leaves & holidays",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // View Mode Switcher: Month Focus vs 12-Month Matrix
        TabRow(
            selectedTabIndex = if (displayMode == CalendarDisplayMode.MONTH_VIEW) 0 else 1,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = displayMode == CalendarDisplayMode.MONTH_VIEW,
                onClick = { displayMode = CalendarDisplayMode.MONTH_VIEW },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                        Text("Month Focus", fontWeight = if (displayMode == CalendarDisplayMode.MONTH_VIEW) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            )
            Tab(
                selected = displayMode == CalendarDisplayMode.YEAR_MATRIX,
                onClick = { displayMode = CalendarDisplayMode.YEAR_MATRIX },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GridOn, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                        Text("12-Month Matrix", fontWeight = if (displayMode == CalendarDisplayMode.YEAR_MATRIX) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Year Selector Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Year:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 4.dp)
            )
            yearsList.forEach { y ->
                FilterChip(
                    selected = uiState.selectedArchiveYear == y,
                    onClick = { viewModel.setArchiveYear(y) },
                    label = { Text("$y", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
            }

            androidx.compose.material3.OutlinedButton(
                onClick = { showYearOverYearDialog = true },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(32.dp)
            ) {
                androidx.compose.material3.Icon(
                    androidx.compose.material.icons.Icons.Default.CalendarViewMonth,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("YoY Compare", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (displayMode == CalendarDisplayMode.MONTH_VIEW) {
            // Month Quick Picker Ribbon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                monthShortNames.forEachIndexed { index, name ->
                    FilterChip(
                        selected = uiState.selectedArchiveMonth == index,
                        onClick = { viewModel.setArchiveYearMonth(uiState.selectedArchiveYear, index) },
                        label = { Text(name, fontSize = 11.5.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BluePrimary.copy(alpha = 0.15f),
                            selectedLabelColor = BluePrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Month View: Scrollable Column with Working Navigation & Details
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Interactive Month Calendar Card with working Prev/Next arrows
                item {
                    MonthCalendarCard(
                        year = uiState.selectedArchiveYear,
                        month = uiState.selectedArchiveMonth,
                        logs = uiState.logs,
                        leaves = uiState.leaves,
                        minHours = uiState.settings.minHours,
                        selectedDateKey = uiState.selectedCalendarDateKey,
                        onDateSelected = { viewModel.selectCalendarDate(it) },
                        onPrevMonth = { viewModel.setArchiveMonth(-1) },
                        onNextMonth = { viewModel.setArchiveMonth(1) },
                        onSelectMonth = { y, m -> viewModel.setArchiveYearMonth(y, m) },
                        onTodayClick = { viewModel.resetArchiveToToday() },
                        onDateLongClick = { dateKey ->
                            selectedActionDateKey = dateKey
                        }
                    )
                }

                // Selected Day Timeline Detail Card with "+ Log / Edit Day" Button
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
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            }
        } else {
            // 12-Month Year Matrix Overview
            val yearPrefix = "${uiState.selectedArchiveYear}"
            val totalYearWfo = uiState.logs.count { it.date.startsWith(yearPrefix) && it.isValidWFODay(uiState.settings.minHours) }
            val totalYearLeaves = uiState.leaves.count { it.date.startsWith(yearPrefix) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total WFO in ${uiState.selectedArchiveYear}: $totalYearWfo days",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = GreenSuccess
                )
                if (totalYearLeaves > 0) {
                    Text(
                        text = "🌴 $totalYearLeaves Leaves/Holidays",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PurpleExclusion
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 12 Month Cards Grid
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
            }
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

    // Manual Log / Leave Unified Dialog
    if (showManualDialog) {
        ManualLogDialog(
            initialDate = manualDialogInitialDate,
            initialLog = manualDialogInitialLog,
            initialLeave = manualDialogInitialLeave,
            initialTab = manualDialogInitialTab,
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
