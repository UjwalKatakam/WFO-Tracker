package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CheckInLog
import com.example.ui.components.ManualLogDialog
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.GreenSuccess
import com.example.ui.viewmodel.WfoUiState
import com.example.ui.viewmodel.WfoViewModel
import com.example.util.DateUtils
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogHistoryScreen(
    uiState: WfoUiState,
    viewModel: WfoViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    var showManualDialog by remember { mutableStateOf(false) }
    var editingLog by remember { mutableStateOf<CheckInLog?>(null) }
    var deletingLogDate by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("all") } // "all", "valid", "short"

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

    // Manual Log / Edit Dialog
    if (showManualDialog) {
        ManualLogDialog(
            initialLog = editingLog,
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

    // Delete confirmation dialog
    if (deletingLogDate != null) {
        AlertDialog(
            onDismissRequest = { deletingLogDate = null },
            title = { Text("Delete Attendance Log?") },
            text = { Text("Are you sure you want to permanently delete the log record for $deletingLogDate?") },
            confirmButton = {
                Button(
                    onClick = {
                        deletingLogDate?.let { viewModel.deleteLog(it) }
                        deletingLogDate = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonDanger)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                Button(
                    onClick = { deletingLogDate = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        )
    }

    // Calculate Summary Stats for clarity
    val totalRecords = uiState.logs.size
    val validWfoCount = uiState.logs.count { it.isValidWFODay(uiState.settings.minHours) }
    val shortWfoCount = totalRecords - validWfoCount
    val totalHoursSum = uiState.logs.sumOf { it.totalHours }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp)
    ) {
        // Top Header & Manual Log CTA
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "OFFICE ATTENDANCE HISTORY",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "$totalRecords Logged Days",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Button(
                onClick = {
                    editingLog = null
                    showManualDialog = true
                },
                modifier = Modifier.testTag("add_manual_log_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Manual Log", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // High-clarity summary dashboard card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp, horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$validWfoCount",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenSuccess
                    )
                    Text(
                        text = "Valid (≥${String.format(Locale.US, "%.1f", uiState.settings.minHours)}h)",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .height(26.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$shortWfoCount",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberWarning
                    )
                    Text(
                        text = "Under Target",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .height(26.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format(Locale.US, "%.1fh", totalHoursSum),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary
                    )
                    Text(
                        text = "Total Logged",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // File Export / Import Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { exportJsonLauncher.launch("WFO_Backup_${DateUtils.todayDateStr()}.json") },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export JSON", fontSize = 11.sp)
            }

            OutlinedButton(
                onClick = { importJsonLauncher.launch(arrayOf("application/json", "text/*")) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Import JSON", fontSize = 11.sp)
            }

            OutlinedButton(
                onClick = { shareCsv(context, uiState.logs, uiState.settings.minHours) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("CSV", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search and Filter Row
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Filter by date (YYYY-MM-DD) or notes...") },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search", modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Quick status filter chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedFilter == "all",
                onClick = { selectedFilter = "all" },
                label = { Text("All ($totalRecords)", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedFilter == "valid",
                onClick = { selectedFilter = "valid" },
                label = { Text("Valid WFO ($validWfoCount)", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedFilter == "short",
                onClick = { selectedFilter = "short" },
                label = { Text("Under Target ($shortWfoCount)", fontSize = 11.sp) }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Filtered logs list
        val filteredLogs = uiState.logs.filter { log ->
            val matchesSearch = if (searchQuery.isBlank()) true
            else log.date.contains(searchQuery.trim()) || log.notes?.contains(searchQuery.trim(), ignoreCase = true) == true

            val matchesFilter = when (selectedFilter) {
                "valid" -> log.isValidWFODay(uiState.settings.minHours)
                "short" -> !log.isValidWFODay(uiState.settings.minHours)
                else -> true
            }

            matchesSearch && matchesFilter
        }

        if (filteredLogs.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(BluePrimary.copy(alpha = 0.15f), RoundedCornerShape(27.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = BluePrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (uiState.logs.isEmpty()) "No Attendance Logs Yet" else "No Matching Logs Found",
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (uiState.logs.isEmpty())
                            "Live punch sessions and manual day logs will appear here with instant daily qualification statuses."
                        else
                            "Try clearing your search query or changing the filter chip above.",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    if (uiState.logs.isEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                editingLog = null
                                showManualDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add First Attendance Log", fontSize = 12.5.sp)
                        }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💡 Swipe right to edit • Swipe left to delete",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredLogs, key = { it.date }) { log ->
                    val isValid = log.isValidWFODay(uiState.settings.minHours)
                    val friendlyDate = DateUtils.formatDisplayDate(log.date)

                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            when (value) {
                                SwipeToDismissBoxValue.EndToStart -> {
                                    deletingLogDate = log.date
                                    false
                                }
                                SwipeToDismissBoxValue.StartToEnd -> {
                                    editingLog = log
                                    showManualDialog = true
                                    false
                                }
                                SwipeToDismissBoxValue.Settled -> false
                            }
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = true,
                        enableDismissFromEndToStart = true,
                        backgroundContent = {
                            val direction = dismissState.dismissDirection
                            val color = when (direction) {
                                SwipeToDismissBoxValue.EndToStart -> CrimsonDanger.copy(alpha = 0.85f)
                                SwipeToDismissBoxValue.StartToEnd -> BluePrimary.copy(alpha = 0.85f)
                                else -> Color.Transparent
                            }
                            val alignment = when (direction) {
                                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                else -> Alignment.Center
                            }
                            val icon = when (direction) {
                                SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Edit
                                else -> null
                            }
                            val label = when (direction) {
                                SwipeToDismissBoxValue.EndToStart -> "Delete"
                                SwipeToDismissBoxValue.StartToEnd -> "Edit"
                                else -> ""
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(color)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = alignment
                            ) {
                                if (icon != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (direction == SwipeToDismissBoxValue.StartToEnd) {
                                            Icon(icon, contentDescription = null, tint = Color.White)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        } else {
                                            Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(icon, contentDescription = null, tint = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    ) {
                        Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isValid) GreenSuccess.copy(alpha = 0.35f) else AmberWarning.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            // Top Row: Date, Friendly Name, Status Pill, Edit & Delete actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = friendlyDate,
                                                fontSize = 14.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "(${log.date})",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        // Status Pill
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isValid) GreenSuccess.copy(alpha = 0.14f) else AmberWarning.copy(alpha = 0.14f)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isValid) Icons.Default.CheckCircle else Icons.Default.Info,
                                                    contentDescription = null,
                                                    tint = if (isValid) GreenSuccess else AmberWarning,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isValid) "Qualified WFO Day" else "Under ${String.format(Locale.US, "%.1f", uiState.settings.minHours)}h target",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isValid) GreenSuccess else AmberWarning
                                                )
                                            }
                                        }
                                    }
                                }

                                Row {
                                    IconButton(
                                        onClick = {
                                            editingLog = log
                                            showManualDialog = true
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint = BluePrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { deletingLogDate = log.date },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = CrimsonDanger,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Data Metrics Cards Row: In Time, Out Time, Duration
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Punch In
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Login,
                                            contentDescription = null,
                                            tint = BluePrimary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Column {
                                            Text(
                                                text = "Punch In",
                                                fontSize = 9.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = DateUtils.formatTime12(log.inTime),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    // Punch Out
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Logout,
                                            contentDescription = null,
                                            tint = AmberWarning,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Column {
                                            Text(
                                                text = "Punch Out",
                                                fontSize = 9.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = DateUtils.formatTime12(log.outTime),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    // Total Duration
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = if (isValid) GreenSuccess else AmberWarning,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Column {
                                            Text(
                                                text = "Total Time",
                                                fontSize = 9.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = log.formattedDuration,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isValid) GreenSuccess else AmberWarning
                                            )
                                        }
                                    }
                                }
                            }

                            if (!log.notes.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Note: ${log.notes}",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
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
}

private fun shareCsv(context: Context, logs: List<CheckInLog>, minHours: Double) {
    val csv = StringBuilder("Date,Check In,Check Out,Duration (Mins),Formatted Duration,Total Hours,Valid WFO Day,Notes\n")
    for (r in logs) {
        csv.append("${r.date},${r.inTime},${r.outTime},${r.durationMinutes},${r.formattedDuration},${String.format(Locale.US, "%.2f", r.totalHours)},${r.isValidWFODay(minHours)},\"${r.notes ?: ""}\"\n")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_SUBJECT, "WFO Attendance Log Export")
        putExtra(Intent.EXTRA_TEXT, csv.toString())
    }
    context.startActivity(Intent.createChooser(intent, "Export WFO CSV Data"))
}
