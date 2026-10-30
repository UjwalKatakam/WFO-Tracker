package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BluePrimary
import com.example.util.DateUtils
import java.util.Calendar
import java.util.Locale

/**
 * Modern touchable picker trigger field that shows label, formatted display value,
 * and launches native Android DatePickerDialog or TimePickerDialog when clicked.
 */
@Composable
fun DateTimePickerField(
    label: String,
    valueText: String,
    subtitle: String? = null,
    icon: ImageVector = Icons.Default.CalendarToday,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
        ),
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BluePrimary.copy(alpha = 0.12f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = BluePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = valueText.ifBlank { "Select..." },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Launches native system DatePickerDialog initialized to dateStr (YYYY-MM-DD)
 */
fun showNativeDatePicker(
    context: Context,
    currentDateStr: String,
    onDateSelected: (String) -> Unit
) {
    val cal = Calendar.getInstance()
    try {
        val parts = currentDateStr.split("-")
        if (parts.size == 3) {
            cal.set(Calendar.YEAR, parts[0].toInt())
            cal.set(Calendar.MONTH, parts[1].toInt() - 1)
            cal.set(Calendar.DAY_OF_MONTH, parts[2].toInt())
        }
    } catch (_: Exception) {}

    val dialog = DatePickerDialog(
        context,
        { _, year, monthOfYear, dayOfMonth ->
            val formatted = String.format(Locale.US, "%04d-%02d-%02d", year, monthOfYear + 1, dayOfMonth)
            onDateSelected(formatted)
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    )
    dialog.show()
}

/**
 * Launches native system TimePickerDialog initialized to timeStr (HH:mm)
 */
fun showNativeTimePicker(
    context: Context,
    currentTimeStr: String,
    onTimeSelected: (String) -> Unit
) {
    var initialHour = 9
    var initialMinute = 0
    try {
        val parts = currentTimeStr.split(":")
        if (parts.size >= 2) {
            initialHour = parts[0].trim().toInt()
            initialMinute = parts[1].trim().take(2).toInt()
        }
    } catch (_: Exception) {}

    val dialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            val formatted = String.format(Locale.US, "%02d:%02d", hourOfDay, minute)
            onTimeSelected(formatted)
        },
        initialHour,
        initialMinute,
        false // 12-hour AM/PM view for best user experience
    )
    dialog.show()
}
