package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.LeaveHoliday
import com.example.ui.theme.AppTheme
import com.example.util.DateUtils
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LeavesManagementDialog(
    leaves: List<LeaveHoliday>,
    onAddLeaveRange: (from: String, to: String, type: String, title: String) -> Unit,
    onDeleteLeave: (id: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var fromDate by remember { mutableStateOf(DateUtils.todayDateStr()) }
    var toDate by remember { mutableStateOf(DateUtils.todayDateStr()) }
    var leaveType by remember { mutableStateOf("holiday") }
    var title by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val leaveTypes = listOf(
        "holiday" to "Holiday",
        "sick" to "Sick",
        "leave" to "Vacation",
        "wfh" to "WFH"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(12.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Leaves and exclusions",
                    style = MaterialTheme.typography.titleLarge,
                    color = AppTheme.colors.ink,
                    fontWeight = FontWeight.Medium
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = AppTheme.colors.muted
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Add exclusion",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppTheme.colors.muted
                )

                Spacer(modifier = Modifier.height(8.dp))

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
                    leaveTypes.forEach { (typeKey, label) ->
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
                    placeholder = { Text("e.g. Annual leave, Public holiday", style = MaterialTheme.typography.bodyMedium) },
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
                        modifier = Modifier.padding(top = 4.dp)
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
                            leaveTypes.firstOrNull { it.first == leaveType }?.second ?: "Exclusion"
                        }
                        onAddLeaveRange(fromDate, toDate, leaveType, titleToSave)
                        title = ""
                        errorMsg = null
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (leaves.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Existing exclusions (${leaves.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppTheme.colors.muted
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(leaves, key = { it.id }) { leave ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AppTheme.colors.raised,
                                border = BorderStroke(1.dp, AppTheme.colors.rule),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = leave.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = AppTheme.colors.ink
                                        )
                                        Text(
                                            text = "${DateUtils.formatDisplayDate(leave.date)} · ${leave.type.replaceFirstChar { it.titlecase(Locale.US) }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AppTheme.colors.muted
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeleteLeave(leave.id) },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete exclusion",
                                            tint = AppTheme.colors.danger,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
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
 * Uses the activity context so it inherits Theme.MyApplication (light/dark styling).
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
 * Uses the activity context so it inherits Theme.MyApplication (light/dark styling).
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
