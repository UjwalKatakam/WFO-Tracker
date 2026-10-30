package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CheckInLog
import com.example.data.local.LeaveHoliday
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.PurpleExclusion
import com.example.util.DateUtils

enum class ManualLogTab {
    ATTENDANCE,
    LEAVE_HOLIDAY
}

@Composable
fun ManualLogDialog(
    initialDate: String = DateUtils.todayDateStr(),
    initialLog: CheckInLog? = null,
    initialLeave: LeaveHoliday? = null,
    initialTab: ManualLogTab = if (initialLeave != null) ManualLogTab.LEAVE_HOLIDAY else ManualLogTab.ATTENDANCE,
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

    // Compute live duration estimate for attendance
    val tempLog = remember(date, inTime, outTime) {
        val inParts = inTime.split(":")
        val outParts = outTime.split(":")
        val inM = (inParts.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (inParts.getOrNull(1)?.toIntOrNull() ?: 0)
        val outM = (outParts.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (outParts.getOrNull(1)?.toIntOrNull() ?: 0)
        var diffM = outM - inM
        if (diffM < 0) diffM += 24 * 60
        CheckInLog(
            date = date,
            inTime = inTime,
            outTime = outTime,
            durationMinutes = diffM
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        initialLog != null -> "Edit Office Attendance"
                        initialLeave != null -> "Edit Leave / Holiday"
                        else -> "Log Attendance or Leave"
                    },
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Tab Selection: Office Attendance vs Leave/Holiday
                TabRow(
                    selectedTabIndex = if (activeTab == ManualLogTab.ATTENDANCE) 0 else 1,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Tab(
                        selected = activeTab == ManualLogTab.ATTENDANCE,
                        onClick = {
                            activeTab = ManualLogTab.ATTENDANCE
                            errorMessage = null
                        },
                        text = {
                            Text(
                                text = "🏢 Office Work",
                                fontSize = 12.5.sp,
                                fontWeight = if (activeTab == ManualLogTab.ATTENDANCE) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                    Tab(
                        selected = activeTab == ManualLogTab.LEAVE_HOLIDAY,
                        onClick = {
                            activeTab = ManualLogTab.LEAVE_HOLIDAY
                            errorMessage = null
                        },
                        text = {
                            Text(
                                text = "🌴 Leave / Holiday",
                                fontSize = 12.5.sp,
                                fontWeight = if (activeTab == ManualLogTab.LEAVE_HOLIDAY) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }

                when (activeTab) {
                    ManualLogTab.ATTENDANCE -> {
                        Text(
                            text = "Log your office punch in/out. Tap any field to use the native calendar or clock picker.",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        // Calendar Selection Field
                        DateTimePickerField(
                            label = "Log Date",
                            valueText = DateUtils.formatDisplayDate(date),
                            subtitle = date,
                            icon = Icons.Default.CalendarMonth,
                            onClick = {
                                showNativeDatePicker(context, date) { pickedDate ->
                                    date = pickedDate
                                    errorMessage = null
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Time Selection Pickers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DateTimePickerField(
                                label = "Punch In Time",
                                valueText = DateUtils.formatTime12(inTime),
                                subtitle = "$inTime (24h)",
                                icon = Icons.Default.Schedule,
                                onClick = {
                                    showNativeTimePicker(context, inTime) { pickedTime ->
                                        inTime = pickedTime
                                        errorMessage = null
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )

                            DateTimePickerField(
                                label = "Punch Out Time",
                                valueText = DateUtils.formatTime12(outTime),
                                subtitle = "$outTime (24h)",
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

                        // Calculated Duration Pill
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BluePrimary.copy(alpha = 0.08f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Computed Duration:",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = tempLog.formattedDuration,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BluePrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Notes (optional)") },
                            placeholder = { Text("e.g. Client visit, forgot to punch") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    ManualLogTab.LEAVE_HOLIDAY -> {
                        Text(
                            text = "Leaves and official holidays are deducted from your required WFO quota so your compliance rate stays 100% accurate.",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        // Category selection
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = leaveType == "holiday",
                                onClick = { leaveType = "holiday" },
                                label = { Text("🌴 Holiday", fontSize = 11.sp) },
                                leadingIcon = { Icon(Icons.Default.Park, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            )
                            FilterChip(
                                selected = leaveType == "sick",
                                onClick = { leaveType = "sick" },
                                label = { Text("🤒 Sick", fontSize = 11.sp) },
                                leadingIcon = { Icon(Icons.Default.MedicalServices, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            )
                            FilterChip(
                                selected = leaveType == "leave",
                                onClick = { leaveType = "leave" },
                                label = { Text("✈️ Vacation", fontSize = 11.sp) },
                                leadingIcon = { Icon(Icons.Default.Luggage, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = leaveType == "wfh",
                                onClick = { leaveType = "wfh" },
                                label = { Text("🏠 Approved WFH", fontSize = 11.sp) },
                                leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Date Range Pickers (From / To)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DateTimePickerField(
                                label = "From Date",
                                valueText = DateUtils.formatDisplayDate(leaveFromDate),
                                subtitle = leaveFromDate,
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

                            DateTimePickerField(
                                label = "To Date",
                                valueText = DateUtils.formatDisplayDate(leaveToDate),
                                subtitle = leaveToDate,
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

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = leaveTitle,
                            onValueChange = { leaveTitle = it },
                            label = { Text("Title / Reason") },
                            placeholder = { Text("e.g. Diwali, Doctor Visit, Family Vacation") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.5.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when (activeTab) {
                        ManualLogTab.ATTENDANCE -> {
                            if (date.isBlank() || inTime.isBlank() || outTime.isBlank()) {
                                errorMessage = "Please select date, punch in, and punch out times."
                                return@Button
                            }
                            if (tempLog.totalHours <= 0) {
                                errorMessage = "Punch out time must be after punch in time."
                                return@Button
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
                                    "holiday" -> "Public Holiday"
                                    "sick" -> "Sick Leave"
                                    "leave" -> "Casual / Vacation Leave"
                                    "wfh" -> "Approved WFH"
                                    else -> "Excused Absence"
                                }
                            }
                            if (onSaveLeave != null) {
                                onSaveLeave(leaveFromDate, leaveToDate, leaveType, titleToSave)
                            }
                            onDismiss()
                        }
                    }
                }
            ) {
                Text(if (activeTab == ManualLogTab.ATTENDANCE) "Save Attendance" else "Save Leave / Holiday")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (initialLog != null && onDeleteLog != null) {
                    IconButton(
                        onClick = {
                            onDeleteLog(initialLog.date)
                            onDismiss()
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Log", tint = CrimsonDanger)
                    }
                }
                if (initialLeave != null && onDeleteLeave != null) {
                    IconButton(
                        onClick = {
                            onDeleteLeave(initialLeave.id)
                            onDismiss()
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Leave", tint = CrimsonDanger)
                    }
                }
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    )
}
