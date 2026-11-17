package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.CheckInLog
import com.example.ui.components.ManualLogDialog
import com.example.ui.components.ReportMonthPickerSheet
import com.example.ui.components.WfoBottomSheet
import com.example.ui.components.WfoCard
import com.example.ui.components.WfoChip
import com.example.ui.components.WfoEmptyState
import com.example.ui.components.WfoIllustrationKind
import com.example.ui.components.WfoPrimaryButton
import com.example.ui.components.WfoTextButton
import com.example.ui.components.sharePdfReport
import com.example.ui.components.showNativeDatePicker
import com.example.ui.theme.AppTheme
import com.example.ui.theme.ChipShape
import com.example.ui.theme.SurfaceGroupShape
import com.example.ui.viewmodel.WfoUiState
import com.example.ui.viewmodel.WfoViewModel
import com.example.util.DateUtils
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Locale

private enum class StatusFilter {
    ALL,
    QUALIFIED,
    UNDER_TARGET
}

private data class MonthLogGroup(
    val key: String,
    val monthTitle: String,
    val daysCount: Int,
    val summary: String,
    val logs: List<CheckInLog>
)

private fun pluralize(count: Int, singular: String, plural: String = "${singular}s"): String {
    return if (count == 1) "$count $singular" else "$count $plural"
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun LogHistoryScreen(
    uiState: WfoUiState,
    viewModel: WfoViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Dialog & sheet state
    var showManualDialog by remember { mutableStateOf(false) }
    var editingLog by remember { mutableStateOf<CheckInLog?>(null) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showReportMonthPicker by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }

    // Search and filter parameters
    var searchQuery by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf(StatusFilter.ALL) }
    var filterFromDate by remember { mutableStateOf("") }
    var filterToDate by remember { mutableStateOf("") }

    // Locally pending deleted dates (for immediate row dismissal and snackbar undo window)
    var pendingDeleted by remember { mutableStateOf(emptySet<String>()) }

    // Prune pendingDeleted dates once uiState.logs no longer contains them so rows do not flash back
    LaunchedEffect(uiState.logs) {
        val currentDates = uiState.logs.map { it.date }.toSet()
        pendingDeleted = pendingDeleted.filter { it in currentDates }.toSet()
    }

    // Commit any pending deletes when screen is disposed
    DisposableEffect(Unit) {
        onDispose {
            pendingDeleted.forEach { date ->
                viewModel.deleteLog(date)
            }
        }
    }

    // Common deletion handler used by swipe-to-dismiss and accessibility action
    val markForDeletion: (CheckInLog) -> Unit = { log ->
        val logDate = log.date
        pendingDeleted = pendingDeleted + logDate
        coroutineScope.launch {
            val parsed = DateUtils.parseDate(logDate)
            val dateShort = SimpleDateFormat("d MMM", Locale.US).format(parsed)
            val result = snackbarHostState.showSnackbar(
                message = "Deleted log for $dateShort",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                pendingDeleted = pendingDeleted - logDate
            } else {
                viewModel.deleteLog(logDate)
            }
        }
    }

    // SAF Document Launchers for JSON File Backup
    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val jsonStr = viewModel.exportJsonString()
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(jsonStr.toByteArray())
                    }
                } catch (_: Exception) {}
            }
        }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val stream = context.contentResolver.openInputStream(uri)
                    val reader = BufferedReader(InputStreamReader(stream))
                    val content = reader.readText()
                    reader.close()
                    viewModel.importJsonString(content)
                } catch (_: Exception) {}
            }
        }
    }

    // Active filter badge count (excluding inline search query)
    val activeFilterCount = remember(filterStatus, filterFromDate, filterToDate) {
        var count = 0
        if (filterStatus != StatusFilter.ALL) count++
        if (filterFromDate.isNotBlank()) count++
        if (filterToDate.isNotBlank()) count++
        count
    }

    // Filtered list: immediate response to pendingDeleted, search query, and filter criteria
    val filteredLogs = remember(
        uiState.logs,
        searchQuery,
        filterStatus,
        filterFromDate,
        filterToDate,
        pendingDeleted,
        uiState.settings.minHours
    ) {
        uiState.logs
            .filter { it.date !in pendingDeleted }
            .filter { log ->
                val isValid = log.isValidWFODay(uiState.settings.minHours)
                val matchesStatus = when (filterStatus) {
                    StatusFilter.ALL -> true
                    StatusFilter.QUALIFIED -> isValid
                    StatusFilter.UNDER_TARGET -> !isValid
                }
                val matchesFrom = filterFromDate.isBlank() || log.date >= filterFromDate
                val matchesTo = filterToDate.isBlank() || log.date <= filterToDate
                val q = searchQuery.trim().lowercase(Locale.US)
                val matchesQuery = if (q.isEmpty()) {
                    true
                } else {
                    val parsedDate = DateUtils.parseDate(log.date)
                    val displayDate = SimpleDateFormat("EEE d MMM", Locale.US).format(parsedDate).lowercase(Locale.US)
                    val monthName = SimpleDateFormat("MMMM yyyy", Locale.US).format(parsedDate).lowercase(Locale.US)
                    log.date.lowercase(Locale.US).contains(q) ||
                        displayDate.contains(q) ||
                        monthName.contains(q) ||
                        (log.notes?.lowercase(Locale.US)?.contains(q) == true)
                }

                matchesStatus && matchesFrom && matchesTo && matchesQuery
            }
            .sortedByDescending { it.date }
    }

    // Summary line computed from visible filtered list (excluding pending deletes)
    val summaryText = remember(filteredLogs, uiState.settings.minHours) {
        val total = filteredLogs.size
        val qualified = filteredLogs.count { it.isValidWFODay(uiState.settings.minHours) }
        val totalMins = filteredLogs.sumOf { it.durationMinutes }
        val totalH = totalMins / 60
        val totalM = totalMins % 60

        "${pluralize(total, "entry", "entries")} · ${totalH}h ${totalM}m · ${pluralize(qualified, "valid day")}"
    }

    // Month grouping for LazyColumn stickyHeader
    val monthGroups = remember(filteredLogs) {
        filteredLogs
            .groupBy { log -> log.date.take(7) } // "YYYY-MM"
            .map { (yearMonth, logsInMonth) ->
                val date = DateUtils.parseDate("$yearMonth-01")
                val monthName = SimpleDateFormat("MMMM yyyy", Locale.US).format(date)
                val daysCount = logsInMonth.size
                val totalMins = logsInMonth.sumOf { it.durationMinutes }
                val h = totalMins / 60
                val m = totalMins % 60
                MonthLogGroup(
                    key = yearMonth,
                    monthTitle = monthName,
                    daysCount = daysCount,
                    summary = "${pluralize(daysCount, "day")}, ${h}h ${m}m",
                    logs = logsInMonth
                )
            }
    }

    // Manual Log Dialog
    if (showManualDialog) {
        val existingLogDates = remember(uiState.logs) { uiState.logs.map { it.date }.toSet() }
        ManualLogDialog(
            initialLog = editingLog,
            existingLogDates = existingLogDates,
            onSaveLog = { date, inTime, outTime, notes ->
                viewModel.saveManualLog(date, inTime, outTime, notes)
                showManualDialog = false
                editingLog = null
            },
            onSaveLeave = { from, to, type, title ->
                viewModel.addLeaveRange(from, to, type, title)
                showManualDialog = false
                editingLog = null
            },
            onDeleteLog = { date ->
                viewModel.deleteLog(date)
                showManualDialog = false
                editingLog = null
            },
            onDismiss = {
                showManualDialog = false
                editingLog = null
            }
        )
    }

    // Monthly Audit Report Picker Bottom Sheet
    if (showReportMonthPicker) {
        ReportMonthPickerSheet(
            initialYear = uiState.dashboardNavYear,
            initialMonth = uiState.dashboardNavMonth,
            onDismissRequest = { showReportMonthPicker = false },
            onConfirm = { year, month ->
                val pdfFile = viewModel.generatePdfReport(context, year, month)
                sharePdfReport(context, pdfFile)
            }
        )
    }

    // Filter Bottom Sheet
    if (showFilterSheet) {
        val liveCount = filteredLogs.size

        WfoBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            title = "Filters"
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Status Filter Chips using FlowRow with wrap-content chips (no weight)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Status",
                        style = MaterialTheme.typography.labelLarge,
                        color = AppTheme.colors.muted,
                        fontWeight = FontWeight.Medium
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WfoChip(
                            text = "All",
                            selected = filterStatus == StatusFilter.ALL,
                            onClick = { filterStatus = StatusFilter.ALL }
                        )
                        WfoChip(
                            text = "Qualified",
                            selected = filterStatus == StatusFilter.QUALIFIED,
                            onClick = { filterStatus = StatusFilter.QUALIFIED }
                        )
                        WfoChip(
                            text = "Under target",
                            selected = filterStatus == StatusFilter.UNDER_TARGET,
                            onClick = { filterStatus = StatusFilter.UNDER_TARGET }
                        )
                    }
                }

                // Date Range Filter
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Date range",
                        style = MaterialTheme.typography.labelLarge,
                        color = AppTheme.colors.muted,
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterDateField(
                            label = "From",
                            dateValue = filterFromDate,
                            onPickDate = {
                                showNativeDatePicker(context, filterFromDate.ifBlank { DateUtils.todayDateStr() }) { picked ->
                                    filterFromDate = picked
                                }
                            },
                            onClearDate = { filterFromDate = "" },
                            modifier = Modifier.weight(1f)
                        )

                        FilterDateField(
                            label = "To",
                            dateValue = filterToDate,
                            onPickDate = {
                                showNativeDatePicker(context, filterToDate.ifBlank { DateUtils.todayDateStr() }) { picked ->
                                    filterToDate = picked
                                }
                            },
                            onClearDate = { filterToDate = "" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom actions: Clear all filters & Show X logs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WfoTextButton(
                        text = "Clear all filters",
                        onClick = {
                            filterStatus = StatusFilter.ALL
                            filterFromDate = ""
                            filterToDate = ""
                        }
                    )

                    WfoPrimaryButton(
                        text = "Show ${pluralize(liveCount, "log")}",
                        onClick = { showFilterSheet = false }
                    )
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = AppTheme.colors.ground,
        contentWindowInsets = WindowInsets(0.dp),
        modifier = modifier.fillMaxSize()
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            // Header: Right-aligned action row (search, filter, add log, overflow menu) with 48 dp targets
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // 1. Search toggle
                    IconButton(
                        onClick = {
                            isSearchActive = !isSearchActive
                            if (!isSearchActive) {
                                searchQuery = ""
                                focusManager.clearFocus()
                            }
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search logs",
                            tint = if (isSearchActive) AppTheme.colors.presentDeep else AppTheme.colors.ink
                        )
                    }

                    // 2. Filter sheet trigger with badge
                    BadgedBox(
                        badge = {
                            if (activeFilterCount > 0) {
                                Badge(
                                    containerColor = AppTheme.colors.progress,
                                    contentColor = AppTheme.colors.onAction
                                ) {
                                    Text(
                                        text = "$activeFilterCount",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    ) {
                        IconButton(
                            onClick = { showFilterSheet = true },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filter logs",
                                tint = if (activeFilterCount > 0) AppTheme.colors.presentDeep else AppTheme.colors.ink
                            )
                        }
                    }

                    // 3. Add manual log
                    IconButton(
                        onClick = {
                            editingLog = null
                            showManualDialog = true
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add log",
                            tint = AppTheme.colors.ink
                        )
                    }

                    // 4. Overflow menu
                    Box {
                        IconButton(
                            onClick = { showOverflowMenu = true },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More actions",
                                tint = AppTheme.colors.ink
                            )
                        }

                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false },
                            shape = SurfaceGroupShape,
                            containerColor = AppTheme.colors.sheet
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Export CSV",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = AppTheme.colors.ink
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    shareCsv(context, filteredLogs, uiState.settings.minHours)
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Export PDF summary",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = AppTheme.colors.ink
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    showReportMonthPicker = true
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Export JSON backup",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = AppTheme.colors.ink
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    exportJsonLauncher.launch("WFO_Backup_${DateUtils.todayDateStr()}.json")
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Import JSON backup",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = AppTheme.colors.ink
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    importJsonLauncher.launch(arrayOf("application/json", "text/*"))
                                }
                            )
                        }
                    }
                }
            }

            // Inline search field (toggled via header action)
            if (isSearchActive) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search by date or note",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppTheme.colors.muted
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = AppTheme.colors.muted,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = AppTheme.colors.muted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AppTheme.colors.sheet,
                        unfocusedContainerColor = AppTheme.colors.sheet,
                        focusedBorderColor = AppTheme.colors.presentDeep,
                        unfocusedBorderColor = AppTheme.colors.rule,
                        focusedTextColor = AppTheme.colors.ink,
                        unfocusedTextColor = AppTheme.colors.ink,
                        focusedLabelColor = AppTheme.colors.presentDeep,
                        unfocusedLabelColor = AppTheme.colors.muted
                    ),
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Summary line at top inside a small WfoCard
            WfoCard(
                elevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = summaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTheme.colors.ink,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            // Content: Empty states or grouped LazyColumn
            when {
                uiState.logs.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            WfoEmptyState(
                                illustration = WfoIllustrationKind.BAR_CHART,
                                title = "No office days yet",
                                body = "Your first punch in will show up here."
                            )
                            WfoPrimaryButton(
                                text = "Add log",
                                onClick = {
                                    editingLog = null
                                    showManualDialog = true
                                }
                            )
                        }
                    }
                }

                filteredLogs.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            WfoEmptyState(
                                illustration = WfoIllustrationKind.BAR_CHART,
                                body = "Nothing matches that search."
                            )
                            WfoTextButton(
                                text = "Clear filters",
                                onClick = {
                                    searchQuery = ""
                                    filterStatus = StatusFilter.ALL
                                    filterFromDate = ""
                                    filterToDate = ""
                                }
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        monthGroups.forEach { group ->
                            stickyHeader(key = "header_${group.key}") {
                                Surface(
                                    color = AppTheme.colors.ground,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = group.monthTitle,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = AppTheme.colors.ink,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = group.summary,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AppTheme.colors.muted
                                        )
                                    }
                                }
                            }

                            items(
                                items = group.logs,
                                key = { it.date }
                            ) { log ->
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { dismissValue ->
                                        if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                            markForDeletion(log)
                                            true
                                        } else {
                                            false
                                        }
                                    }
                                )

                                SwipeToDismissBox(
                                    state = dismissState,
                                    enableDismissFromStartToEnd = false,
                                    enableDismissFromEndToStart = true,
                                    backgroundContent = {
                                        val isSwiping = dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart
                                        val bgColor = if (isSwiping) {
                                            AppTheme.colors.danger.copy(alpha = 0.12f)
                                        } else {
                                            Color.Transparent
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                                .clip(SurfaceGroupShape)
                                                .background(bgColor)
                                                .padding(horizontal = 20.dp),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            if (isSwiping) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete log",
                                                        tint = AppTheme.colors.danger,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Text(
                                                        text = "Delete",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = AppTheme.colors.danger
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    content = {
                                        LogRowItem(
                                            log = log,
                                            minHours = uiState.settings.minHours,
                                            onClick = {
                                                editingLog = log
                                                showManualDialog = true
                                            },
                                            onDeleteViaAccessibility = {
                                                markForDeletion(log)
                                            }
                                        )
                                    }
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LogRowItem(
    log: CheckInLog,
    minHours: Double,
    onClick: () -> Unit,
    onDeleteViaAccessibility: () -> Unit
) {
    val isValid = remember(log, minHours) { log.isValidWFODay(minHours) }
    val dateText = remember(log.date) {
        val parsed = DateUtils.parseDate(log.date)
        SimpleDateFormat("EEE d MMM", Locale.US).format(parsed)
    }
    val timeRangeText = remember(log.inTime, log.outTime) {
        val inStr = DateUtils.formatTime12(log.inTime).lowercase(Locale.US)
        val outStr = DateUtils.formatTime12(log.outTime).lowercase(Locale.US)
        "$inStr to $outStr"
    }
    val durationText = remember(log.durationMinutes) {
        val h = log.durationMinutes / 60
        val m = log.durationMinutes % 60
        String.format(Locale.US, "%dh %02dm", h, m)
    }

    WfoCard(
        elevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .semantics {
                    customActions = listOf(
                        CustomAccessibilityAction("Edit log") {
                            onClick()
                            true
                        },
                        CustomAccessibilityAction("Delete log") {
                            onDeleteViaAccessibility()
                            true
                        }
                    )
                },
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Line 1: date on the left, duration on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppTheme.colors.ink,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = durationText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppTheme.colors.ink,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.End
                )
            }

            // Line 2: time range and status chip
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = timeRangeText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTheme.colors.muted,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )

                if (isValid) {
                    Surface(
                        shape = ChipShape,
                        color = AppTheme.colors.presentContainer,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = AppTheme.colors.presentDeep,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Valid",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppTheme.colors.presentDeep,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = ChipShape,
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, AppTheme.colors.short),
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = AppTheme.colors.short,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Under minimum",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppTheme.colors.short,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Notes go on the last line, ellipsized
            if (!log.notes.isNullOrBlank()) {
                Text(
                    text = log.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.colors.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun FilterDateField(
    label: String,
    dateValue: String,
    onPickDate: () -> Unit,
    onClearDate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayDateText = if (dateValue.isNotBlank()) {
        try {
            val parsed = DateUtils.parseDate(dateValue)
            SimpleDateFormat("EEE d MMM", Locale.US).format(parsed)
        } catch (_: Exception) {
            dateValue
        }
    } else {
        "Select"
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = AppTheme.colors.sheet,
        border = BorderStroke(1.dp, AppTheme.colors.rule),
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clickable(onClick = onPickDate)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = AppTheme.colors.muted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = AppTheme.colors.muted
                    )
                    Text(
                        text = displayDateText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (dateValue.isNotBlank()) AppTheme.colors.ink else AppTheme.colors.muted,
                        fontWeight = if (dateValue.isNotBlank()) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }

            if (dateValue.isNotBlank()) {
                IconButton(
                    onClick = onClearDate,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear $label date",
                        tint = AppTheme.colors.muted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private fun escapeCsvField(value: String): String {
    val escaped = value.replace("\"", "\"\"")
    return if (escaped.contains(',') || escaped.contains('"') || escaped.contains('\n') || escaped.contains('\r')) {
        "\"$escaped\""
    } else {
        escaped
    }
}

private fun shareCsv(context: Context, logs: List<CheckInLog>, minHours: Double) {
    val csv = StringBuilder("Date,Check in,Check out,Duration (mins),Formatted duration,Total hours,Valid WFO day,Notes\n")
    for (r in logs) {
        val notesEscaped = escapeCsvField(r.notes ?: "")
        val durationEscaped = escapeCsvField(r.formattedDuration)
        csv.append("${r.date},${r.inTime},${r.outTime},${r.durationMinutes},$durationEscaped,${String.format(Locale.US, "%.2f", r.totalHours)},${r.isValidWFODay(minHours)},$notesEscaped\n")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_SUBJECT, "WFO attendance log export")
        putExtra(Intent.EXTRA_TEXT, csv.toString())
    }
    context.startActivity(Intent.createChooser(intent, "Export WFO CSV data"))
}
