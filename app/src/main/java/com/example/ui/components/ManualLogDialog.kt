package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CheckInLog
import com.example.data.local.LeaveHoliday
import com.example.ui.theme.AppTheme
import com.example.util.DateUtils
import java.util.Locale

enum class ManualLogTab {
    ATTENDANCE,
    LEAVE_HOLIDAY
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManualLogDialog(
    initialDate: String = DateUtils.todayDateStr(),
    initialLog: CheckInLog? = null,
    initialLeave: LeaveHoliday? = null,
    initialTab: ManualLogTab = if (initialLeave != null) ManualLogTab.LEAVE_HOLIDAY else ManualLogTab.ATTENDANCE,
    existingLogDates: Set<String> = emptySet(),
    onSaveLog: ((date: String, inTime: String, outTime: String, notes: String?) -> Unit)? = null,
    onSaveLeave: ((from: String, to: String, type: String, title: String) -> Unit)? = null,
    onSave: ((date: String, inTime: String, outTime: String, notes: String?) -> Unit)? = null,
    onDeleteLog: ((date: String) -> Unit)? = null,
    onDeleteLeave: ((id: String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var activeTab by remember { mutableStateOf(initialTab) }

    // Attendance State
    var date by remember { mutableStateOf(initialLog?.date ?: initialDate) }
    var inTime by remember { mutableStateOf(initialLog?.inTime ?: "09:00") }
    var outTime by remember { mutableStateOf(initialLog?.outTime ?: "17:30") }
    var notes by remember { mutableStateOf(initialLog?.notes ?: "") }

    // Leave State
    var leaveFromDate by remember { mutableStateOf(initialLeave?.date ?: initialDate) }
    var leaveToDate by remember { mutableStateOf(initialLeave?.date ?: initialDate) }
    var leaveType by remember { mutableStateOf(initialLeave?.type ?: "holiday") }
    var leaveTitle by remember { mutableStateOf(initialLeave?.title ?: "") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Compute live duration estimate for attendance
    val liveDurationMinutes = remember(inTime, outTime) {
        val inParts = inTime.split(":")
        val outParts = outTime.split(":")
        val inM = (inParts.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (inParts.getOrNull(1)?.toIntOrNull() ?: 0)
        val outM = (outParts.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (outParts.getOrNull(1)?.toIntOrNull() ?: 0)
        outM - inM
    }

    val liveDurationFormatted = remember(liveDurationMinutes) {
        if (liveDurationMinutes <= 0) {
            "0h 00m"
        } else {
            val h = liveDurationMinutes / 60
            val m = liveDurationMinutes % 60
            String.format(Locale.US, "%dh %02dm", h, m)
        }
    }

    // Delete confirmation prompt
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            shape = RoundedCornerShape(12.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text(
                    text = if (initialLog != null) "Delete attendance log?" else "Delete leave record?",
                    style = MaterialTheme.typography.titleMedium,
                    color = AppTheme.colors.ink,
                    fontWeight = FontWeight.Medium
                )
            },
            text = {
                Text(
                    text = if (initialLog != null) {
                        "Are you sure you want to permanently delete the attendance record for ${DateUtils.formatDisplayDate(initialLog.date)}?"
                    } else {
                        "Are you sure you want to permanently remove this leave exclusion?"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTheme.colors.muted
                )
            },
            confirmButton = {
                WfoTextButton(
                    text = "Delete",
                    isDanger = true,
                    onClick = {
                        showDeleteConfirm = false
                        if (initialLog != null && onDeleteLog != null) {
                            onDeleteLog(initialLog.date)
                        } else if (initialLeave != null && onDeleteLeave != null) {
                            onDeleteLeave(initialLeave.id)
                        }
                        onDismiss()
                    }
                )
            },
            dismissButton = {
                WfoTextButton(
                    text = "Cancel",
                    onClick = { showDeleteConfirm = false }
                )
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(12.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Text(
                text = when {
                    initialLog != null -> "Edit attendance"
                    initialLeave != null -> "Edit leave"
                    else -> "Log day"
                },
                style = MaterialTheme.typography.titleLarge,
                color = AppTheme.colors.ink,
                fontWeight = FontWeight.Medium
            )
        },
        text = {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                // Two-segment control: Office day vs Leave or holiday (no emoji tabs)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AppTheme.colors.raised,
                    border = BorderStroke(1.dp, AppTheme.colors.rule),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val isAttendance = activeTab == ManualLogTab.ATTENDANCE
                        Surface(
                            selected = isAttendance,
                            onClick = {
                                activeTab = ManualLogTab.ATTENDANCE
                                errorMessage = null
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isAttendance) AppTheme.colors.ground else AppTheme.colors.raised,
                            border = if (isAttendance) BorderStroke(1.dp, AppTheme.colors.rule) else null,
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 40.dp)
                                .semantics { role = Role.Tab }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "Office day",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isAttendance) AppTheme.colors.ink else AppTheme.colors.muted,
                                    fontWeight = if (isAttendance) FontWeight.Medium else FontWeight.Normal
                                )
                            }
                        }

                        val isLeave = activeTab == ManualLogTab.LEAVE_HOLIDAY
                        Surface(
                            selected = isLeave,
                            onClick = {
                                activeTab = ManualLogTab.LEAVE_HOLIDAY
                                errorMessage = null
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isLeave) AppTheme.colors.ground else AppTheme.colors.raised,
                            border = if (isLeave) BorderStroke(1.dp, AppTheme.colors.rule) else null,
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 40.dp)
                                .semantics { role = Role.Tab }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "Leave or holiday",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isLeave) AppTheme.colors.ink else AppTheme.colors.muted,
                                    fontWeight = if (isLeave) FontWeight.Medium else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                when (activeTab) {
                    ManualLogTab.ATTENDANCE -> {
                        if (initialLog != null) {
                            // Read-only date representation when editing existing log
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Date",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppTheme.colors.muted
                                )
                                Text(
                                    text = DateUtils.formatDisplayDate(date),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = AppTheme.colors.ink,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            // Date picker field
                            LogPickerField(
                                label = "Date",
                                value = DateUtils.formatDisplayDate(date),
                                icon = Icons.Default.CalendarMonth,
                                onClick = {
                                    showNativeDatePicker(context, date) { pickedDate ->
                                        date = pickedDate
                                        errorMessage = null
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (date in existingLogDates) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "A log for this date already exists. Saving replaces it.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppTheme.colors.danger,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Time pickers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LogPickerField(
                                label = "Punch in",
                                value = DateUtils.formatTime12(inTime).lowercase(Locale.US),
                                icon = Icons.Default.Schedule,
                                onClick = {
                                    showNativeTimePicker(context, inTime) { pickedTime ->
                                        inTime = pickedTime
                                        errorMessage = null
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )

                            LogPickerField(
                                label = "Punch out",
                                value = DateUtils.formatTime12(outTime).lowercase(Locale.US),
                                icon = Icons.Default.AccessTime,
                                onClick = {
                                    showNativeTimePicker(context, outTime) { pickedTime ->
                                        outTime = pickedTime
                                        errorMessage = null
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Live duration in 14 sp Ink (not in a colored box)
                        Text(
                            text = "Duration: $liveDurationFormatted",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppTheme.colors.ink,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Notes (optional)", style = MaterialTheme.typography.bodySmall) },
                            placeholder = { Text("e.g. Client visit, forgot to punch", style = MaterialTheme.typography.bodyMedium) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = AppTheme.colors.raised,
                                unfocusedContainerColor = AppTheme.colors.raised,
                                focusedBorderColor = AppTheme.colors.present,
                                unfocusedBorderColor = AppTheme.colors.rule,
                                focusedTextColor = AppTheme.colors.ink,
                                unfocusedTextColor = AppTheme.colors.ink,
                                focusedLabelColor = AppTheme.colors.present,
                                unfocusedLabelColor = AppTheme.colors.muted
                            ),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    ManualLogTab.LEAVE_HOLIDAY -> {
                        // Category chips: FlowRow with wrap-content chips (no weight)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            WfoChip(
                                text = "Holiday",
                                selected = leaveType == "holiday",
                                onClick = { leaveType = "holiday" },
                                icon = Icons.Default.Celebration
                            )
                            WfoChip(
                                text = "Sick",
                                selected = leaveType == "sick",
                                onClick = { leaveType = "sick" },
                                icon = Icons.Default.MedicalServices
                            )
                            WfoChip(
                                text = "Vacation",
                                selected = leaveType == "leave",
                                onClick = { leaveType = "leave" },
                                icon = Icons.Default.Flight
                            )
                            WfoChip(
                                text = "WFH",
                                selected = leaveType == "wfh",
                                onClick = { leaveType = "wfh" },
                                icon = Icons.Default.HomeWork
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Date range pickers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LogPickerField(
                                label = "From",
                                value = DateUtils.formatDisplayDate(leaveFromDate),
                                icon = Icons.Default.CalendarMonth,
                                onClick = {
                                    showNativeDatePicker(context, leaveFromDate) { picked ->
                                        leaveFromDate = picked
                                        if (leaveToDate < picked) leaveToDate = picked
                                        errorMessage = null
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )

                            LogPickerField(
                                label = "To",
                                value = DateUtils.formatDisplayDate(leaveToDate),
                                icon = Icons.Default.CalendarMonth,
                                onClick = {
                                    showNativeDatePicker(context, leaveToDate) { picked ->
                                        leaveToDate = picked
                                        if (leaveFromDate > picked) leaveFromDate = picked
                                        errorMessage = null
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = leaveTitle,
                            onValueChange = { leaveTitle = it },
                            label = { Text("Reason (optional)", style = MaterialTheme.typography.bodySmall) },
                            placeholder = { Text("e.g. Festival, Doctor visit", style = MaterialTheme.typography.bodyMedium) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = AppTheme.colors.raised,
                                unfocusedContainerColor = AppTheme.colors.raised,
                                focusedBorderColor = AppTheme.colors.present,
                                unfocusedBorderColor = AppTheme.colors.rule,
                                focusedTextColor = AppTheme.colors.ink,
                                unfocusedTextColor = AppTheme.colors.ink,
                                focusedLabelColor = AppTheme.colors.present,
                                unfocusedLabelColor = AppTheme.colors.muted
                            ),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Validation error in plain language, 12 sp, Danger color
                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = AppTheme.colors.danger,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            val isReplacing = initialLog == null && activeTab == ManualLogTab.ATTENDANCE && date in existingLogDates
            val saveLabel = when {
                activeTab == ManualLogTab.LEAVE_HOLIDAY -> "Save leave"
                isReplacing -> "Replace log"
                else -> "Save attendance"
            }
            WfoPrimaryButton(
                text = saveLabel,
                onClick = {
                    when (activeTab) {
                        ManualLogTab.ATTENDANCE -> {
                            if (date.isBlank() || inTime.isBlank() || outTime.isBlank()) {
                                errorMessage = "Please select date, punch in, and punch out times."
                                return@WfoPrimaryButton
                            }
                            if (liveDurationMinutes <= 0) {
                                errorMessage = "Punch out must be after punch in."
                                return@WfoPrimaryButton
                            }
                            if (onSaveLog != null) {
                                onSaveLog(date.trim(), inTime.trim(), outTime.trim(), notes.ifBlank { null })
                            } else if (onSave != null) {
                                onSave(date.trim(), inTime.trim(), outTime.trim(), notes.ifBlank { null })
                            }
                            onDismiss()
                        }
                        ManualLogTab.LEAVE_HOLIDAY -> {
                            val titleToSave = leaveTitle.ifBlank {
                                when (leaveType) {
                                    "holiday" -> "Holiday"
                                    "sick" -> "Sick"
                                    "leave" -> "Vacation"
                                    "wfh" -> "WFH"
                                    else -> "Excused absence"
                                }
                            }
                            if (onSaveLeave != null) {
                                onSaveLeave(leaveFromDate, leaveToDate, leaveType, titleToSave)
                            }
                            onDismiss()
                        }
                    }
                }
            )
        },
        dismissButton = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if ((initialLog != null && onDeleteLog != null) || (initialLeave != null && onDeleteLeave != null)) {
                    WfoTextButton(
                        text = "Delete",
                        isDanger = true,
                        onClick = { showDeleteConfirm = true }
                    )
                }
                WfoTextButton(
                    text = "Cancel",
                    onClick = onDismiss
                )
            }
        }
    )
}

@Composable
private fun LogPickerField(
    label: String,
    value: String,
    icon: ImageVector,
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
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
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
