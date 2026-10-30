package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.GreenSuccessGlow
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CircularProgressTimer(
    isPunchedIn: Boolean,
    elapsedSeconds: Long,
    targetHours: Double,
    countdownMode: Boolean,
    targetReached: Boolean,
    onToggleCountdown: () -> Unit,
    onPunchClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onPunchLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val targetSeconds = (targetHours * 3600).toLong()
    val progress = if (targetSeconds > 0) {
        (elapsedSeconds.toFloat() / targetSeconds.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600),
        label = "progress"
    )

    // Calculate display time
    val displaySeconds = if (countdownMode && isPunchedIn) {
        val remaining = targetSeconds - elapsedSeconds
        if (remaining < 0) -remaining else remaining
    } else {
        elapsedSeconds
    }

    val hours = displaySeconds / 3600
    val minutes = (displaySeconds % 3600) / 60
    val seconds = displaySeconds % 60

    val formattedTime = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        // Status Badge Pill
        Surface(
            shape = MaterialTheme.shapes.small,
            color = when {
                targetReached -> GreenSuccess.copy(alpha = 0.15f)
                isPunchedIn -> BluePrimary.copy(alpha = 0.15f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .padding(end = 0.dp)
                )
                Text(
                    text = when {
                        targetReached -> "🎉 TARGET MET (COMPLIANT)"
                        isPunchedIn -> if (countdownMode) "⏱️ COUNTDOWN REMAINING" else "🟢 LIVE AT WORK (CLICK TO TOGGLE)"
                        else -> "⚪ READY TO PUNCH IN"
                    },
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = when {
                        targetReached -> GreenSuccess
                        isPunchedIn -> BluePrimary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        // Circular Timer Visual with long-press support
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(240.dp)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onToggleCountdown() },
                    onLongClick = onLongClick
                )
                .testTag("timer_display_box")
        ) {
            val trackColor = MaterialTheme.colorScheme.surfaceVariant
            val primaryColor = if (targetReached) GreenSuccess else BluePrimary
            val glowColor = if (targetReached) GreenSuccessGlow else Color(0xFF60A5FA)

            Canvas(modifier = Modifier.size(220.dp)) {
                val strokeWidth = 14.dp.toPx()
                // Background Track
                drawCircle(
                    color = trackColor,
                    style = Stroke(width = strokeWidth)
                )

                // Animated Progress Arc
                if (animatedProgress > 0f) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(primaryColor, glowColor, primaryColor)
                        ),
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }

            // Digital Counter Inside Ring
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = formattedTime,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = when {
                        targetReached -> GreenSuccess
                        isPunchedIn -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                if (isPunchedIn) {
                    Text(
                        text = if (countdownMode) "remaining" else "logged today",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (targetReached) "Goal Achieved! 🎉" else "${(progress * 100).toInt()}% of daily goal",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (targetReached) GreenSuccess else BluePrimary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Big Primary Action Button with combinedClickable for holding
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = if (isPunchedIn) CrimsonDanger else BluePrimary,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(52.dp)
                .testTag("punch_action_button")
                .combinedClickable(
                    onClick = onPunchClick,
                    onLongClick = if (!isPunchedIn) onPunchLongClick else null
                )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isPunchedIn) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = if (isPunchedIn) "PUNCH OUT" else "PUNCH IN",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
