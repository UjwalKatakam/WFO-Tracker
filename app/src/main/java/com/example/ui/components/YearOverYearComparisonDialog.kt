package com.example.ui.components

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.GreenSuccess
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
                        tint = BluePrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Year-over-Year WFO Audit",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$currentYear vs $priorYear Compliance",
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
                // High level Delta banner
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = BluePrimary.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (daysDelta >= 0) "$daysDelta more WFO days logged than in $priorYear ($daysDeltaPct)" else "$daysDelta fewer WFO days logged than in $priorYear",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = BluePrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Table Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Metric", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.5f))
                    Text("$currentYear", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = BluePrimary, modifier = Modifier.weight(1f))
                    Text("$priorYear", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Metric Rows
                MetricCompareRow("WFO Days", "${currStats.totalWfoDays} days", "${priorStats.totalWfoDays} days")
                MetricCompareRow("Total Hours", "${String.format(Locale.US, "%.1f", currStats.totalHours)}h", "${String.format(Locale.US, "%.1f", priorStats.totalHours)}h")
                MetricCompareRow("Avg Daily Hours", "${String.format(Locale.US, "%.1f", currStats.avgDailyHours)}h", "${String.format(Locale.US, "%.1f", priorStats.avgDailyHours)}h")
                MetricCompareRow("Leaves / Holidays", "${currStats.totalLeaves} days", "${priorStats.totalLeaves} days")
                MetricCompareRow("Annual Compliance", "${currStats.compliancePct}%", "${priorStats.compliancePct}%")
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun MetricCompareRow(label: String, val1: String, val2: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1.5f))
            Text(val1, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BluePrimary, modifier = Modifier.weight(1f))
            Text(val2, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        }
    }
}
