package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppTheme
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
        shape = RoundedCornerShape(12.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = AppTheme.colors.present,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Weekly streak",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = AppTheme.colors.ink
                        )
                        Text(
                            text = if (streak == 1) "1 week streak" else "$streak weeks streak",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.colors.muted
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = AppTheme.colors.muted
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Streak Progress Summary Row
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AppTheme.colors.raised,
                    border = BorderStroke(1.dp, AppTheme.colors.rule),
                    modifier = Modifier.fillMaxWidth()
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
                                text = "Weekly target",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppTheme.colors.muted
                            )
                            Text(
                                text = "$validDaysThisWeek of $targetDays days completed",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = if (validDaysThisWeek >= targetDays) AppTheme.colors.present else AppTheme.colors.ink
                            )
                        }
                        Text(
                            text = if (validDaysThisWeek >= targetDays) {
                                "Target met"
                            } else {
                                if (daysRemaining == 1) "1 day to go" else "$daysRemaining days to go"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = if (validDaysThisWeek >= targetDays) AppTheme.colors.present else AppTheme.colors.short
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Days this week",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppTheme.colors.muted
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
                            color = AppTheme.colors.ground,
                            border = BorderStroke(1.dp, AppTheme.colors.rule),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 44.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = day.dayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.width(36.dp),
                                        color = AppTheme.colors.ink
                                    )
                                    Text(
                                        text = day.dateKey.substring(5),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AppTheme.colors.muted
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    when {
                                        day.isValidWfo -> {
                                            Text(
                                                text = "${String.format(Locale.US, "%.1f", day.hours)}h (office)",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                color = AppTheme.colors.present
                                            )
                                        }
                                        day.isLeave -> {
                                            Text(
                                                text = day.leaveTitle ?: "Leave",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = AppTheme.colors.muted
                                            )
                                        }
                                        day.hours > 0 -> {
                                            Text(
                                                text = "${String.format(Locale.US, "%.1f", day.hours)}h (short)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = AppTheme.colors.short
                                            )
                                        }
                                        else -> {
                                            Text(
                                                text = if (day.isFutureOrRest) "Rest" else "No log",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = AppTheme.colors.muted
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
                        tint = AppTheme.colors.muted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "A week qualifies when at least $targetDays days meet ${uiState.settings.minHours}h. Approved leaves preserve streak continuity.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTheme.colors.muted
                    )
                }
            }
        },
        confirmButton = {
            WfoTextButton(
                text = "Close",
                onClick = onDismiss
            )
        }
    )
}
