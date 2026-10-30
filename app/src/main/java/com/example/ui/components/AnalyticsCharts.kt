package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.GreenSuccess
import com.example.util.DateUtils
import java.util.Locale

@Composable
fun ArrivalHistogramCard(
    arrivalBins: List<DateUtils.ArrivalBin>,
    modifier: Modifier = Modifier
) {
    val maxCount = arrivalBins.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    val peakBin = arrivalBins.maxByOrNull { it.count }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⏰ ARRIVAL TIME CONSISTENCY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
                if (peakBin != null && peakBin.count > 0) {
                    Text(
                        text = "Peak: ${peakBin.label} (${peakBin.count}x)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary
                    )
                }
            }

            Text(
                text = "Check-in distribution across 30-min office arrival windows.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            // Histogram Bars
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                arrivalBins.forEach { bin ->
                    val isPeak = bin.count == peakBin?.count && bin.count > 0
                    val barFraction = if (maxCount > 0) (bin.count.toFloat() / maxCount.toFloat()) else 0f

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        // Count Label
                        if (bin.count > 0) {
                            Text(
                                text = "${bin.count}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPeak) BluePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }

                        // Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .fillMaxHeight(barFraction.coerceAtLeast(0.06f))
                                .background(
                                    color = if (isPeak) BluePrimary else BluePrimary.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Bin Label
                        Text(
                            text = bin.label,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HoursTrendCard(
    weeklyTrends: List<DateUtils.TrendPoint>,
    modifier: Modifier = Modifier
) {
    val maxHours = weeklyTrends.maxOfOrNull { maxOf(it.actualHours, it.targetHours) }?.coerceAtLeast(1.0) ?: 24.0

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📈 ATTENDANCE & HOURS TRENDS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(CrimsonDanger, CircleShape)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = "Target Goal",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "Total logged office hours vs target threshold week-over-week.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            // Trend Chart with Canvas Reference Line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                // Dashed target line
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val targetYFraction = 1f - (weeklyTrends.firstOrNull()?.targetHours?.toFloat() ?: 24f) / maxHours.toFloat()
                    val yPos = size.height * targetYFraction.coerceIn(0.1f, 0.9f)

                    drawLine(
                        color = CrimsonDanger.copy(alpha = 0.7f),
                        start = Offset(0f, yPos),
                        end = Offset(size.width, yPos),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    )
                }

                // Bars
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    weeklyTrends.forEach { point ->
                        val isMet = point.actualHours >= point.targetHours
                        val barFraction = (point.actualHours / maxHours).toFloat().coerceIn(0.05f, 1f)

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.1fh", point.actualHours),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isMet) GreenSuccess else AmberWarning,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.55f)
                                    .fillMaxHeight(barFraction)
                                    .background(
                                        color = if (isMet) GreenSuccess.copy(alpha = 0.85f) else AmberWarning.copy(alpha = 0.85f),
                                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                    )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "${point.label}\n(${point.daysCount}d)",
                                fontSize = 8.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
