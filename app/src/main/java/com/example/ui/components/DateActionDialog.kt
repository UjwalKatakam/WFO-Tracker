package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Park
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CheckInLog
import com.example.data.local.LeaveHoliday
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.PurpleExclusion
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
    val parsedDate = DateUtils.parseDate(dateKey)
    val friendlyDate = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US).format(parsedDate)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MANAGE DAY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = friendlyDate,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Current status box
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = when {
                        leave != null -> PurpleExclusion.copy(alpha = 0.12f)
                        log != null && log.isValidWFODay(minHours) -> GreenSuccess.copy(alpha = 0.12f)
                        log != null -> AmberWarning.copy(alpha = 0.12f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    },
                    border = BorderStroke(
                        1.dp,
                        when {
                            leave != null -> PurpleExclusion.copy(alpha = 0.4f)
                            log != null && log.isValidWFODay(minHours) -> GreenSuccess.copy(alpha = 0.4f)
                            log != null -> AmberWarning.copy(alpha = 0.4f)
                            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "CURRENT STATUS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        when {
                            leave != null -> {
                                Text(
                                    text = "🌴 ${leave.title} (${leave.type.uppercase()})",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PurpleExclusion
                                )
                                Text(
                                    text = "Working day excluded from monthly WFO quota requirement.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            log != null -> {
                                val isValid = log.isValidWFODay(minHours)
                                Text(
                                    text = "🏢 ${DateUtils.formatTime12(log.inTime)} – ${DateUtils.formatTime12(log.outTime)} (${log.formattedDuration})",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isValid) GreenSuccess else AmberWarning
                                )
                                Text(
                                    text = if (isValid) "Qualified WFO Day (≥ ${String.format(Locale.US, "%.1f", minHours)}h)" else "Under required ${String.format(Locale.US, "%.1f", minHours)}h target",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            else -> {
                                Text(
                                    text = "No activity recorded for this day",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "SELECT ACTION",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Action 1: Office Attendance
                Button(
                    onClick = {
                        onLogAttendanceClick()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (log != null) Icons.Default.Edit else Icons.Default.Login,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (log != null) "Edit Office Attendance" else "Log Office Attendance (Punch In/Out)")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action 2: Leave / Holiday
                OutlinedButton(
                    onClick = {
                        onLogLeaveClick()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Park, contentDescription = null, modifier = Modifier.size(18.dp), tint = PurpleExclusion)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Log Leave / Holiday / WFH", color = PurpleExclusion)
                }

                // Delete Options if records exist
                if (log != null && onDeleteLog != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = {
                            onDeleteLog()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = CrimsonDanger)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete Attendance Log", color = CrimsonDanger, fontSize = 12.sp)
                    }
                }

                if (leave != null && onDeleteLeave != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = {
                            onDeleteLeave()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = CrimsonDanger)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Remove Leave / Holiday", color = CrimsonDanger, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text("Close", color = MaterialTheme.colorScheme.onSurface)
            }
        }
    )
}
