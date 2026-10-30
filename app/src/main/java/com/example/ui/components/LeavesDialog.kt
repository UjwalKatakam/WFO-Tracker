package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.ui.platform.LocalContext
import com.example.data.local.LeaveHoliday
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.PurpleExclusion
import com.example.util.DateUtils

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
    var selectedType by remember { mutableStateOf("holiday") }
    var reason by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🌴 Holidays & Leave Exclusions",
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
                Text(
                    text = "Working days excluded from WFO quota. Weekends (Sat/Sun) are automatically excluded.",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Category selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedType == "holiday",
                        onClick = { selectedType = "holiday" },
                        label = { Text("🌴 Holiday", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedType == "sick",
                        onClick = { selectedType = "sick" },
                        label = { Text("🤒 Sick", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedType == "leave",
                        onClick = { selectedType = "leave" },
                        label = { Text("✈️ Annual", fontSize = 11.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Date pickers input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DateTimePickerField(
                        label = "From Date",
                        valueText = DateUtils.formatShortDate(fromDate),
                        subtitle = fromDate,
                        icon = Icons.Default.CalendarMonth,
                        onClick = {
                            showNativeDatePicker(context, fromDate) { picked ->
                                fromDate = picked
                                if (toDate < picked) toDate = picked
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    DateTimePickerField(
                        label = "To Date",
                        valueText = DateUtils.formatShortDate(toDate),
                        subtitle = toDate,
                        icon = Icons.Default.CalendarMonth,
                        onClick = {
                            showNativeDatePicker(context, toDate) { picked ->
                                toDate = picked
                                if (fromDate > picked) fromDate = picked
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason / Description (optional)") },
                    placeholder = { Text("e.g. Festival, Medical, Vacation") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (fromDate.isNotBlank()) {
                            onAddLeaveRange(
                                fromDate.trim(),
                                toDate.ifBlank { fromDate }.trim(),
                                selectedType,
                                reason.trim()
                            )
                            reason = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("+ Add Excluded Working Days")
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "RECORDED EXCLUSIONS (${leaves.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (leaves.isEmpty()) {
                    Text(
                        text = "No holidays or leave exclusions recorded.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                    ) {
                        items(leaves, key = { it.id }) { item ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = item.date,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = when (item.type) {
                                                    "holiday" -> "🌴 Holiday"
                                                    "sick" -> "🤒 Sick"
                                                    else -> "✈️ Leave"
                                                },
                                                fontSize = 10.sp,
                                                color = PurpleExclusion,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        if (item.title.isNotBlank()) {
                                            Text(
                                                text = item.title,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { onDeleteLeave(item.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = CrimsonDanger,
                                            modifier = Modifier.size(16.dp)
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
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text("Close", color = MaterialTheme.colorScheme.onSurface)
            }
        }
    )
}
