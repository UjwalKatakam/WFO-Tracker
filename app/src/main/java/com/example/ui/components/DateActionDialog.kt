package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Login
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.CheckInLog
import com.example.data.local.LeaveHoliday
import com.example.ui.theme.AppTheme
import com.example.ui.theme.SurfaceGroupShape
import com.example.util.DateUtils
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun DateActionDialog(
    dateKey: String,
    log: CheckInLog?,
    leave: LeaveHoliday?,
    minHours: Double,
    onLogAttendanceClick: () -> Unit,
    onLogLeaveClick: () -> Unit,
    onDeleteLog: (() -> Unit)? = null,
    onDeleteLeave: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val parsedDate = remember(dateKey) { DateUtils.parseDate(dateKey) }
    val friendlyDate = remember(parsedDate) {
        SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US).format(parsedDate)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppTheme.colors.raised,
        titleContentColor = AppTheme.colors.ink,
        textContentColor = AppTheme.colors.ink,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Manage day",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppTheme.colors.muted
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = friendlyDate,
                        style = MaterialTheme.typography.titleMedium,
                        color = AppTheme.colors.ink,
                        fontWeight = FontWeight.Medium
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
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
                // Current status summary surface
                Surface(
                    shape = SurfaceGroupShape,
                    color = AppTheme.colors.ground,
                    border = BorderStroke(1.dp, AppTheme.colors.rule),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Current status",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppTheme.colors.muted
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        when {
                            leave != null -> {
                                Text(
                                    text = "${leave.title} (${leave.type})",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = AppTheme.colors.ink
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Working day excluded from monthly office quota requirement.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppTheme.colors.muted
                                )
                            }
                            log != null -> {
                                val isValid = log.isValidWFODay(minHours)
                                Text(
                                    text = "${DateUtils.formatTime12(log.inTime)} – ${DateUtils.formatTime12(log.outTime)} (${log.formattedDuration})",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isValid) AppTheme.colors.present else AppTheme.colors.short
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isValid) "Qualified office day (at least ${String.format(Locale.US, "%.1f", minHours)}h)" else "Short day (under ${String.format(Locale.US, "%.1f", minHours)}h minimum)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppTheme.colors.muted
                                )
                            }
                            else -> {
                                Text(
                                    text = "No activity recorded for this day",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = AppTheme.colors.muted
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = AppTheme.colors.rule, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Actions",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppTheme.colors.muted
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Action 1: Office attendance
                WfoPrimaryButton(
                    text = if (log != null) "Edit office attendance" else "Log office attendance",
                    onClick = {
                        onLogAttendanceClick()
                        onDismiss()
                    },
                    icon = if (log != null) Icons.Default.Edit else Icons.Default.Login,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Action 2: Leave / holiday
                WfoTonalButton(
                    text = if (leave != null) "Edit leave or holiday" else "Log leave or holiday",
                    onClick = {
                        onLogLeaveClick()
                        onDismiss()
                    },
                    icon = Icons.Default.EventBusy,
                    modifier = Modifier.fillMaxWidth()
                )

                // Delete options if records exist
                if (log != null && onDeleteLog != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    WfoTextButton(
                        text = "Delete attendance log",
                        onClick = {
                            onDeleteLog()
                            onDismiss()
                        },
                        isDanger = true,
                        icon = Icons.Default.Delete,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (leave != null && onDeleteLeave != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    WfoTextButton(
                        text = "Remove leave or holiday",
                        onClick = {
                            onDeleteLeave()
                            onDismiss()
                        },
                        isDanger = true,
                        icon = Icons.Default.Delete,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            WfoTextButton(
                text = "Close",
                onClick = onDismiss
            )
        }
    )
}
