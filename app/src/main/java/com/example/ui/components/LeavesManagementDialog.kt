package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.LeaveDefaults
import com.example.data.local.LeaveHoliday
import com.example.data.local.LeaveRangeResult
import com.example.util.DateUtils
import com.example.util.HolidayImportCandidate
import com.example.util.HolidayImportParser
import com.example.ui.theme.AppTheme
import kotlinx.coroutines.delay
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private sealed class DialogUndoAction {
    data class SingleDelete(val leaveId: String, val message: String) : DialogUndoAction()
    data class BatchImport(val insertedIds: List<String>, val message: String) : DialogUndoAction()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LeavesManagementDialog(
    leaves: List<LeaveHoliday>,
    onAddLeaveRange: (from: String, to: String, type: String, title: String, onResult: (Result<LeaveRangeResult>) -> Unit) -> Unit,
    onDeleteLeave: (id: String) -> Unit,
    onImportHolidays: (holidays: List<LeaveHoliday>, onInserted: (List<LeaveHoliday>) -> Unit) -> Unit = { _, _ -> },
    onDeleteLeavesBatch: (ids: List<String>) -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var fromDate by remember { mutableStateOf(DateUtils.todayDateStr()) }
    var toDate by remember { mutableStateOf(DateUtils.todayDateStr()) }
    var leaveType by remember { mutableStateOf(LeaveDefaults.TYPE_HOLIDAY) }
    var title by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // Pending delete and undo banner state (commits after 5s, on next delete, or on dialog dispose)
    var pendingUndoDeleteId by remember { mutableStateOf<String?>(null) }
    var committingDeletedIds by remember { mutableStateOf(emptySet<String>()) }
    var undoAction by remember { mutableStateOf<DialogUndoAction?>(null) }
    var undoNonce by remember { mutableIntStateOf(0) }

    val latestPendingUndoId by rememberUpdatedState(pendingUndoDeleteId)
    val latestOnDeleteLeave by rememberUpdatedState(onDeleteLeave)

    val hiddenIds = remember(pendingUndoDeleteId, committingDeletedIds) {
        if (pendingUndoDeleteId != null) committingDeletedIds + pendingUndoDeleteId!! else committingDeletedIds
    }

    // Import dates state
    var importInputText by remember { mutableStateOf("") }
    var importFileLabel by remember { mutableStateOf<String?>(null) }
    var parsedCandidates by remember { mutableStateOf<List<HolidayImportCandidate>>(emptyList()) }
    var selectionOverrides by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var skippedRecurringCount by remember { mutableIntStateOf(0) }
    var unparsedCount by remember { mutableIntStateOf(0) }

    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }

    fun parseAndPopulate(rawText: String, fileLabel: String? = null) {
        if (rawText.isBlank()) {
            parsedCandidates = emptyList()
            selectionOverrides = emptyMap()
            skippedRecurringCount = 0
            unparsedCount = 0
            importFileLabel = null
            return
        }
        val res = HolidayImportParser.parseInput(
            rawContent = rawText,
            existingLeaves = leaves.filter { it.id !in hiddenIds },
            currentYear = currentYear
        )
        parsedCandidates = res.candidates
        skippedRecurringCount = res.skippedRecurringCount
        unparsedCount = res.unparsedLinesCount
        importFileLabel = fileLabel
        selectionOverrides = res.candidates.associate { it.date to it.selectedByDefault }
    }

    val openDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val content = BufferedReader(InputStreamReader(stream)).readText()
                    parseAndPopulate(content, uri.lastPathSegment ?: "Imported file")
                }
            } catch (_: Exception) {
                errorMsg = "Could not read selected file."
            }
        }
    }

    // Auto-reparse paste field when text or existing leaves change
    LaunchedEffect(importInputText, leaves, hiddenIds) {
        if (importInputText.isNotBlank()) {
            val res = HolidayImportParser.parseInput(
                rawContent = importInputText,
                existingLeaves = leaves.filter { it.id !in hiddenIds },
                currentYear = currentYear
            )
            parsedCandidates = res.candidates
            skippedRecurringCount = res.skippedRecurringCount
            unparsedCount = res.unparsedLinesCount
            selectionOverrides = res.candidates.associate { candidate ->
                candidate.date to (selectionOverrides[candidate.date] ?: candidate.selectedByDefault)
            }
        } else if (importFileLabel == null) {
            parsedCandidates = emptyList()
            skippedRecurringCount = 0
            unparsedCount = 0
        }
    }

    // Reconcile committingDeletedIds once Room deletes them
    LaunchedEffect(leaves) {
        val currentIds = leaves.map { it.id }.toSet()
        committingDeletedIds = committingDeletedIds.filter { it in currentIds }.toSet()
    }

    // Finalize pending delete on 5-second timeout
    LaunchedEffect(undoNonce) {
        val current = undoAction ?: return@LaunchedEffect
        delay(5000L)
        if (current is DialogUndoAction.SingleDelete && pendingUndoDeleteId == current.leaveId) {
            pendingUndoDeleteId = null
            committingDeletedIds = committingDeletedIds + current.leaveId
            latestOnDeleteLeave(current.leaveId)
        }
        if (undoAction == current) {
            undoAction = null
        }
    }

    // Finalize any pending delete when dialog closes
    DisposableEffect(Unit) {
        onDispose {
            latestPendingUndoId?.let { id ->
                latestOnDeleteLeave(id)
            }
        }
    }

    val visibleLeaves = remember(leaves, hiddenIds) {
        leaves.filter { it.id !in hiddenIds }
    }

    val groupedLeaves = remember(visibleLeaves) {
        visibleLeaves
            .sortedByDescending { it.date }
            .groupBy { it.date.take(7) }
            .map { (yearMonth, itemsInMonth) ->
                val parsed = DateUtils.parseDate("$yearMonth-01")
                val headerLabel = SimpleDateFormat("MMMM yyyy", Locale.US).format(parsed)
                headerLabel to itemsInMonth
            }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(12.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Text(
                text = "Leaves and exclusions",
                style = MaterialTheme.typography.titleLarge,
                color = AppTheme.colors.ink,
                fontWeight = FontWeight.Medium
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                AnimatedVisibility(visible = undoAction != null) {
                    val activeUndo = undoAction
                    if (activeUndo != null) {
                        val msg = when (activeUndo) {
                            is DialogUndoAction.SingleDelete -> activeUndo.message
                            is DialogUndoAction.BatchImport -> activeUndo.message
                        }
                        WfoUndoBanner(
                            message = msg,
                            onUndoClick = {
                                when (activeUndo) {
                                    is DialogUndoAction.SingleDelete -> {
                                        if (pendingUndoDeleteId == activeUndo.leaveId) {
                                            pendingUndoDeleteId = null
                                        }
                                    }
                                    is DialogUndoAction.BatchImport -> {
                                        onDeleteLeavesBatch(activeUndo.insertedIds)
                                    }
                                }
                                undoAction = null
                            },
                            onDismissClick = {
                                if (activeUndo is DialogUndoAction.SingleDelete) {
                                    if (pendingUndoDeleteId == activeUndo.leaveId) {
                                        pendingUndoDeleteId = null
                                        committingDeletedIds = committingDeletedIds + activeUndo.leaveId
                                        onDeleteLeave(activeUndo.leaveId)
                                    }
                                }
                                undoAction = null
                            },
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                }

                // Date pickers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LeavePickerField(
                        label = "From",
                        value = DateUtils.formatDisplayDate(fromDate),
                        onClick = {
                            showNativeDatePicker(context, fromDate) { picked ->
                                fromDate = picked
                                if (toDate < picked) toDate = picked
                                errorMsg = null
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    LeavePickerField(
                        label = "To",
                        value = DateUtils.formatDisplayDate(toDate),
                        onClick = {
                            showNativeDatePicker(context, toDate) { picked ->
                                toDate = picked
                                if (fromDate > picked) fromDate = picked
                                errorMsg = null
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Type chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LeaveDefaults.typeOptions.forEach { (typeKey, label) ->
                        WfoChip(
                            text = label,
                            selected = leaveType == typeKey,
                            onClick = { leaveType = typeKey }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title or description (optional)", style = MaterialTheme.typography.bodySmall) },
                    placeholder = { Text("e.g. Vacation, Public holiday", style = MaterialTheme.typography.bodyMedium) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AppTheme.colors.raised,
                        unfocusedContainerColor = AppTheme.colors.raised,
                        focusedBorderColor = AppTheme.colors.present,
                        unfocusedBorderColor = AppTheme.colors.rule,
                        focusedTextColor = AppTheme.colors.ink,
                        unfocusedTextColor = AppTheme.colors.ink,
                        focusedLabelColor = AppTheme.colors.present,
                        unfocusedLabelColor = AppTheme.colors.muted,
                        cursorColor = AppTheme.colors.present
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMsg != null) {
                    Text(
                        text = errorMsg ?: "",
                        color = AppTheme.colors.danger,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                WfoPrimaryButton(
                    text = "Add exclusion",
                    onClick = {
                        if (fromDate.isBlank() || toDate.isBlank()) {
                            errorMsg = "Please choose valid dates."
                            return@WfoPrimaryButton
                        }
                        val titleToSave = title.ifBlank {
                            LeaveDefaults.defaultTitleFor(leaveType)
                        }
                        onAddLeaveRange(fromDate, toDate, leaveType, titleToSave) { result ->
                            result.onSuccess {
                                title = ""
                                errorMsg = null
                            }.onFailure { err ->
                                errorMsg = err.message ?: "Invalid date format. Expected YYYY-MM-DD."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = AppTheme.colors.rule, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // Import dates section for holidays
                WfoSectionHeader(
                    title = "Import dates",
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                WfoTonalButton(
                    text = if (importFileLabel != null) "Change file ($importFileLabel)" else "Import from file",
                    icon = Icons.Default.FileUpload,
                    onClick = {
                        openDocLauncher.launch(arrayOf("text/calendar", "text/plain"))
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = importInputText,
                    onValueChange = {
                        importFileLabel = null
                        importInputText = it
                    },
                    label = { Text("Or paste dates, one per line", style = MaterialTheme.typography.bodySmall) },
                    placeholder = { Text("2026-01-26, Republic Day\n15 Aug 2026", style = MaterialTheme.typography.bodyMedium) },
                    minLines = 3,
                    maxLines = 6,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AppTheme.colors.raised,
                        unfocusedContainerColor = AppTheme.colors.raised,
                        focusedBorderColor = AppTheme.colors.present,
                        unfocusedBorderColor = AppTheme.colors.rule,
                        focusedTextColor = AppTheme.colors.ink,
                        unfocusedTextColor = AppTheme.colors.ink,
                        focusedLabelColor = AppTheme.colors.present,
                        unfocusedLabelColor = AppTheme.colors.muted,
                        cursorColor = AppTheme.colors.present
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (skippedRecurringCount > 0 || unparsedCount > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    val notices = buildList {
                        if (skippedRecurringCount > 0) {
                            add("Skipped $skippedRecurringCount recurring event${if (skippedRecurringCount == 1) "" else "s"}")
                        }
                        if (unparsedCount > 0) {
                            add("Could not parse $unparsedCount line${if (unparsedCount == 1) "" else "s"}")
                        }
                    }
                    Text(
                        text = notices.joinToString(". ") + ".",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTheme.colors.short
                    )
                }

                if (parsedCandidates.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))

                    parsedCandidates.forEach { candidate ->
                        val isChecked = selectionOverrides[candidate.date] ?: candidate.selectedByDefault
                        val flagLabels = buildList {
                            if (candidate.isWeekend) add("Weekend")
                            if (candidate.isDuplicate) add("Duplicate")
                            if (candidate.isOutsideCurrentYear) add("Outside $currentYear")
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .toggleable(
                                    value = isChecked,
                                    role = Role.Checkbox,
                                    onValueChange = { checked ->
                                        selectionOverrides = selectionOverrides + (candidate.date to checked)
                                    }
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = AppTheme.colors.present,
                                    uncheckedColor = AppTheme.colors.muted,
                                    checkmarkColor = if (AppTheme.colors.isDark) AppTheme.colors.ground else AppTheme.colors.raised
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = candidate.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AppTheme.colors.ink,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = buildString {
                                        append(DateUtils.formatDisplayDate(candidate.date))
                                        if (flagLabels.isNotEmpty()) {
                                            append(" · ${flagLabels.joinToString(", ")}")
                                        }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (flagLabels.isNotEmpty()) AppTheme.colors.short else AppTheme.colors.muted
                                )
                            }
                        }
                        HorizontalDivider(color = AppTheme.colors.rule, thickness = 1.dp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val selectedToImport = parsedCandidates.filter { candidate ->
                        selectionOverrides[candidate.date] ?: candidate.selectedByDefault
                    }
                    val selectedCount = selectedToImport.size
                    val holidayWord = if (selectedCount == 1) "holiday" else "holidays"

                    WfoPrimaryButton(
                        text = "Add $selectedCount $holidayWord",
                        enabled = selectedCount > 0,
                        onClick = {
                            val leavesToInsert = selectedToImport.map { candidate ->
                                LeaveHoliday(
                                    id = "${LeaveDefaults.TYPE_HOLIDAY}-${candidate.date}",
                                    type = LeaveDefaults.TYPE_HOLIDAY,
                                    title = candidate.title,
                                    date = candidate.date
                                )
                            }
                            onImportHolidays(leavesToInsert) { inserted ->
                                if (inserted.isNotEmpty()) {
                                    // Finalize any lingering single delete first
                                    val previousUndo = undoAction
                                    if (previousUndo is DialogUndoAction.SingleDelete && pendingUndoDeleteId == previousUndo.leaveId) {
                                        pendingUndoDeleteId = null
                                        committingDeletedIds = committingDeletedIds + previousUndo.leaveId
                                        onDeleteLeave(previousUndo.leaveId)
                                    }
                                    val count = inserted.size
                                    val word = if (count == 1) "holiday" else "holidays"
                                    undoAction = DialogUndoAction.BatchImport(
                                        insertedIds = inserted.map { it.id },
                                        message = "Added $count $word"
                                    )
                                    undoNonce++
                                }
                                importInputText = ""
                                importFileLabel = null
                                parsedCandidates = emptyList()
                                selectionOverrides = emptyMap()
                                skippedRecurringCount = 0
                                unparsedCount = 0
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (groupedLeaves.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = AppTheme.colors.rule, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    groupedLeaves.forEach { (monthHeader, monthLeaves) ->
                        WfoSectionHeader(title = monthHeader)
                        monthLeaves.forEach { leave ->
                            WfoListRow(
                                title = leave.title,
                                subtitle = "${DateUtils.formatDisplayDate(leave.date)} · ${LeaveDefaults.chipLabelFor(leave.type)}",
                                trailingContent = {
                                    IconButton(
                                        onClick = {
                                            // Commit previous pending delete immediately when another item is deleted
                                            pendingUndoDeleteId?.let { prevId ->
                                                if (prevId != leave.id) {
                                                    committingDeletedIds = committingDeletedIds + prevId
                                                    onDeleteLeave(prevId)
                                                }
                                            }
                                            pendingUndoDeleteId = leave.id
                                            undoAction = DialogUndoAction.SingleDelete(
                                                leaveId = leave.id,
                                                message = "Removed ${leave.title}"
                                            )
                                            undoNonce++
                                        },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete ${leave.title} on ${leave.date}",
                                            tint = AppTheme.colors.danger,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                },
                                showDivider = true
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            WfoTextButton(
                text = "Done",
                onClick = onDismiss
            )
        }
    )
}

@Composable
private fun LeavePickerField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = AppTheme.colors.raised,
        border = BorderStroke(1.dp, AppTheme.colors.rule),
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
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
                    text = value.ifBlank { "Select" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTheme.colors.ink,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Native Android DatePickerDialog helper.
 */
fun showNativeDatePicker(
    context: Context,
    initialDate: String,
    onDateSelected: (String) -> Unit
) {
    val cal = Calendar.getInstance()
    if (initialDate.isNotBlank()) {
        val parts = initialDate.split("-")
        if (parts.size == 3) {
            val y = parts[0].toIntOrNull() ?: cal.get(Calendar.YEAR)
            val m = (parts[1].toIntOrNull() ?: 1) - 1
            val d = parts[2].toIntOrNull() ?: cal.get(Calendar.DAY_OF_MONTH)
            cal.set(y, m, d)
        }
    }

    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val formatted = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
            onDateSelected(formatted)
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    ).show()
}

/**
 * Native Android TimePickerDialog helper.
 */
fun showNativeTimePicker(
    context: Context,
    initialTime: String,
    onTimeSelected: (String) -> Unit
) {
    val cal = Calendar.getInstance()
    var hour = cal.get(Calendar.HOUR_OF_DAY)
    var minute = cal.get(Calendar.MINUTE)

    if (initialTime.isNotBlank()) {
        val parts = initialTime.split(":")
        if (parts.size >= 2) {
            hour = parts[0].toIntOrNull() ?: hour
            minute = parts[1].toIntOrNull() ?: minute
        }
    }

    TimePickerDialog(
        context,
        { _, h, m ->
            val formatted = String.format(Locale.US, "%02d:%02d", h, m)
            onTimeSelected(formatted)
        },
        hour,
        minute,
        false
    ).show()
}
