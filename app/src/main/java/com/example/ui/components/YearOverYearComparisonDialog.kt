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
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
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
import com.example.ui.theme.AppTheme
import com.example.ui.theme.SurfaceGroupShape
import com.example.ui.viewmodel.WfoUiState
import java.util.Locale

@Composable
fun YearOverYearComparisonDialog(
    uiState: WfoUiState,
    onDismiss: () -> Unit
) {
    val currentYear = uiState.selectedArchiveYear
    val priorYear = currentYear - 1

    val currStats = uiState.calculateYearStats(currentYear)
    val priorStats = uiState.calculateYearStats(priorYear)

    val daysDelta = currStats.totalWfoDays - priorStats.totalWfoDays
    val daysDeltaPct = if (priorStats.totalWfoDays > 0) {
        String.format(Locale.US, "%+.1f%%", (daysDelta.toDouble() / priorStats.totalWfoDays) * 100)
    } else {
        "+0%"
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CompareArrows,
                        contentDescription = null,
                        tint = AppTheme.colors.present,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Year-over-year audit",
                            style = MaterialTheme.typography.titleMedium,
                            color = AppTheme.colors.ink,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "$currentYear vs $priorYear comparison",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.colors.muted
                        )
                    }
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
                // High level Delta banner
                Surface(
                    shape = SurfaceGroupShape,
                    color = AppTheme.colors.ground,
                    border = BorderStroke(1.dp, AppTheme.colors.rule),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = AppTheme.colors.present,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (daysDelta >= 0) {
                                "$daysDelta more office days logged than in $priorYear ($daysDeltaPct)"
                            } else {
                                "${-daysDelta} fewer office days logged than in $priorYear"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = AppTheme.colors.ink
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Metric",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppTheme.colors.muted,
                        modifier = Modifier.weight(1.5f)
                    )
                    Text(
                        text = "$currentYear",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppTheme.colors.present,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "$priorYear",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppTheme.colors.muted,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = AppTheme.colors.rule, thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))

                // Metric Rows
                MetricCompareRow("Office days", "${currStats.totalWfoDays} days", "${priorStats.totalWfoDays} days")
                MetricCompareRow("Total hours", "${String.format(Locale.US, "%.1f", currStats.totalHours)}h", "${String.format(Locale.US, "%.1f", priorStats.totalHours)}h")
                MetricCompareRow("Avg daily hours", "${String.format(Locale.US, "%.1f", currStats.avgDailyHours)}h", "${String.format(Locale.US, "%.1f", priorStats.avgDailyHours)}h")
                MetricCompareRow("Leaves / holidays", "${currStats.totalLeaves} days", "${priorStats.totalLeaves} days")
                MetricCompareRow("Annual compliance", "${currStats.compliancePct}%", "${priorStats.compliancePct}%")
            }
        },
        confirmButton = {
            WfoPrimaryButton(
                text = "Done",
                onClick = onDismiss
            )
        }
    )
}

@Composable
private fun MetricCompareRow(label: String, val1: String, val2: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = AppTheme.colors.ink,
            modifier = Modifier.weight(1.5f)
        )
        Text(
            text = val1,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = AppTheme.colors.present,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = val2,
            style = MaterialTheme.typography.bodyMedium,
            color = AppTheme.colors.muted,
            modifier = Modifier.weight(1f)
        )
    }
}
