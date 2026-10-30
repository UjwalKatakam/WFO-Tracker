package com.example.ui.components

import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.ui.theme.AppTheme
import java.io.File
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportMonthPickerSheet(
    initialYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    initialMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    onDismissRequest: () -> Unit,
    onConfirm: (year: Int, month: Int) -> Unit
) {
    val currentCal = remember { Calendar.getInstance() }
    val currentYear = currentCal.get(Calendar.YEAR)
    val currentMonth = currentCal.get(Calendar.MONTH)

    var selectedYear by remember { mutableIntStateOf(initialYear.coerceAtMost(currentYear)) }
    var selectedMonth by remember {
        mutableIntStateOf(
            if (initialYear == currentYear) initialMonth.coerceAtMost(currentMonth) else initialMonth
        )
    }

    val monthSymbols = remember { DateFormatSymbols.getInstance(Locale.US) }
    val shortMonths = remember { monthSymbols.shortMonths.take(12) }
    val fullMonths = remember { monthSymbols.months.take(12) }

    WfoBottomSheet(
        title = "Monthly audit report",
        onDismissRequest = onDismissRequest
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Year navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        selectedYear -= 1
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous year",
                        tint = AppTheme.colors.ink
                    )
                }

                Text(
                    text = selectedYear.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = AppTheme.colors.ink,
                    fontWeight = FontWeight.Medium
                )

                IconButton(
                    onClick = {
                        if (selectedYear < currentYear) {
                            selectedYear += 1
                            if (selectedYear == currentYear && selectedMonth > currentMonth) {
                                selectedMonth = currentMonth
                            }
                        }
                    },
                    enabled = selectedYear < currentYear,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next year",
                        tint = if (selectedYear < currentYear) AppTheme.colors.ink else AppTheme.colors.muted
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 12 month chips in 4 rows of 3 columns
            for (row in 0 until 4) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (col in 0 until 3) {
                        val monthIndex = row * 3 + col
                        val isFuture = selectedYear == currentYear && monthIndex > currentMonth
                        val isSelected = selectedMonth == monthIndex

                        WfoChip(
                            text = shortMonths.getOrElse(monthIndex) { "Month ${monthIndex + 1}" },
                            selected = isSelected,
                            enabled = !isFuture,
                            onClick = {
                                if (!isFuture) {
                                    selectedMonth = monthIndex
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            val selectedFullMonth = fullMonths.getOrElse(selectedMonth) { shortMonths.getOrElse(selectedMonth) { "" } }
            WfoPrimaryButton(
                text = "Create PDF for $selectedFullMonth $selectedYear",
                onClick = {
                    onConfirm(selectedYear, selectedMonth)
                    onDismissRequest()
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

fun sharePdfReport(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "WFO Monthly Compliance Audit Report")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share attendance report"))
    } catch (_: Exception) {}
}
