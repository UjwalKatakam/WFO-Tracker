package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.PurpleExclusion
import com.example.ui.viewmodel.WfoUiState
import java.util.Locale

@Composable
fun WeeklyStreakBreakdownDialog(
    uiState: WfoUiState,
    onDismiss: () -> Unit
) {
    val streak = uiState.weeklyStreak
    val validDaysThisWeek = uiState.validDaysThisWeek
    val targetDays = uiState.settings.targetDaysPerWeek
    val daysRemaining = maxOf(0, targetDays - validDaysThisWeek)
    val breakdown = uiState.currentWeekBreakdown

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(AmberWarning.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = AmberWarning,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Weekly Streak Breakdown",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$streak-week consistency streak",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Streak Progress Summary Card
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "THIS WEEK'S TARGET",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$validDaysThisWeek / $targetDays Days Completed",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (validDaysThisWeek >= targetDays) GreenSuccess else BluePrimary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (validDaysThisWeek >= targetDays) GreenSuccess.copy(alpha = 0.2f) else AmberWarning.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = if (validDaysThisWeek >= targetDays) "STREAK SAFE ✓" else "$daysRemaining DAY${if (daysRemaining == 1) "" else "S"} TO GO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (validDaysThisWeek >= targetDays) GreenSuccess else AmberWarning,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "DAYS IN CURRENT WEEK",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Days list
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    breakdown.forEach { day ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                when {
                                    day.isValidWfo -> GreenSuccess.copy(alpha = 0.5f)
                                    day.isLeave -> PurpleExclusion.copy(alpha = 0.5f)
                                    day.hours > 0 -> AmberWarning.copy(alpha = 0.5f)
                                    else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = day.dayName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(36.dp),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = day.dateKey.substring(5),
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    when {
                                        day.isValidWfo -> {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = GreenSuccess.copy(alpha = 0.18f)
                                            ) {
                                                Text(
                                                    text = "✓ ${String.format(Locale.US, "%.1f", day.hours)}h (Valid)",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = GreenSuccess,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        day.isLeave -> {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = PurpleExclusion.copy(alpha = 0.18f)
                                            ) {
                                                Text(
                                                    text = "🌴 ${day.leaveTitle ?: "Leave"}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = PurpleExclusion,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        day.hours > 0 -> {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = AmberWarning.copy(alpha = 0.18f)
                                            ) {
                                                Text(
                                                    text = "◷ ${String.format(Locale.US, "%.1f", day.hours)}h (<${uiState.settings.minHours}h)",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = AmberWarning,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        else -> {
                                            Text(
                                                text = if (day.isFutureOrRest) "— Rest / Pending" else "— No attendance",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "A week qualifies when ≥$targetDays days meet ${uiState.settings.minHours}h. Leaves preserve streak continuity.",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Got It")
            }
        }
    )
}
