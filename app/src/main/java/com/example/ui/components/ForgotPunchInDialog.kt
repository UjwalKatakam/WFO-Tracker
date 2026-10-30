package com.example.ui.components

import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BluePrimary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ForgotPunchInDialog(
    shiftStart: String,
    onDismiss: () -> Unit,
    onConfirmBackfill: (hour: Int, minute: Int) -> Unit
) {
    val context = LocalContext.current
    val now = Calendar.getInstance()

    // Default to shift start or 1 hour ago
    val shiftParts = shiftStart.split(":")
    val defaultH = shiftParts.getOrNull(0)?.toIntOrNull() ?: maxOf(0, now.get(Calendar.HOUR_OF_DAY) - 1)
    val defaultM = shiftParts.getOrNull(1)?.toIntOrNull() ?: 0

    var selectedHour by remember { mutableIntStateOf(defaultH) }
    var selectedMinute by remember { mutableIntStateOf(defaultM) }

    val formattedTime = remember(selectedHour, selectedMinute) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, selectedHour)
            set(Calendar.MINUTE, selectedMinute)
        }
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(cal.time)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = BluePrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Forgot to Punch In?",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Did you arrive earlier and forget to start the live timer? Pick your actual arrival time today to start tracking with accurate credit.",
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Time Display Box
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = BluePrimary.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, BluePrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            TimePickerDialog(
                                context,
                                { _, h, m ->
                                    selectedHour = h
                                    selectedMinute = m
                                },
                                selectedHour,
                                selectedMinute,
                                false
                            ).show()
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "BACKDATED ARRIVAL TIME",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = BluePrimary
                            )
                            Text(
                                text = formattedTime,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = BluePrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Change", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "QUICK ARRIVAL PRESETS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Shift Start Preset
                    FilterChip(
                        selected = selectedHour == defaultH && selectedMinute == defaultM,
                        onClick = {
                            selectedHour = defaultH
                            selectedMinute = defaultM
                        },
                        label = { Text("Shift Start ($shiftStart)", fontSize = 11.sp) }
                    )

                    // 1h Ago Preset
                    val oneHourAgo = maxOf(0, now.get(Calendar.HOUR_OF_DAY) - 1)
                    FilterChip(
                        selected = selectedHour == oneHourAgo && selectedMinute == now.get(Calendar.MINUTE),
                        onClick = {
                            selectedHour = oneHourAgo
                            selectedMinute = now.get(Calendar.MINUTE)
                        },
                        label = { Text("1 Hour Ago", fontSize = 11.sp) }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmBackfill(selectedHour, selectedMinute)
                }
            ) {
                Text("Punch In at $formattedTime")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
