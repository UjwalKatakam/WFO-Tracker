package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.local.CheckInLog
import com.example.data.local.LeaveHoliday
import com.example.data.preferences.WfoSettings
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    fun generateMonthlyAuditReport(
        context: Context,
        year: Int,
        month: Int, // 0-indexed (0 = January)
        logs: List<CheckInLog>,
        leaves: List<LeaveHoliday>,
        settings: WfoSettings
    ): File {
        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val monthName = SimpleDateFormat("MMMM yyyy", Locale.US).format(cal.time)

        // Filter logs for this month
        val monthLogs = logs.filter {
            val d = DateUtils.parseDate(it.date)
            val c = Calendar.getInstance().apply { time = d }
            c.get(Calendar.YEAR) == year && c.get(Calendar.MONTH) == month
        }.sortedBy { it.date }

        // Filter leaves for this month
        val monthLeaves = leaves.filter {
            val d = DateUtils.parseDate(it.date)
            val c = Calendar.getInstance().apply { time = d }
            val dow = c.get(Calendar.DAY_OF_WEEK)
            c.get(Calendar.YEAR) == year && c.get(Calendar.MONTH) == month &&
                    dow != Calendar.SATURDAY && dow != Calendar.SUNDAY
        }

        val monthLeavesCount = monthLeaves.size
        val targetDays = maxOf(0, settings.targetDaysPerMonth - monthLeavesCount)
        val validDaysCount = monthLogs.count { it.isValidWFODay(settings.minHours) }
        val totalLoggedMinutes = monthLogs.sumOf { it.durationMinutes }
        val totalLoggedHours = totalLoggedMinutes / 60.0
        val isCompliant = validDaysCount >= targetDays
        val compliancePercentage = if (targetDays > 0) {
            minOf(100, (validDaysCount * 100) / targetDays)
        } else 100

        // 1. Top Header Banner
        paint.color = Color.rgb(30, 41, 59) // Slate 800
        canvas.drawRect(0f, 0f, 595f, 75f, paint)

        // Header Title
        paint.color = Color.WHITE
        paint.textSize = 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("WFO OFFICE ATTENDANCE AUDIT REPORT", 30f, 38f, paint)

        paint.color = Color.rgb(147, 197, 253) // Blue 300
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Official Monthly Compliance Summary | $monthName", 30f, 56f, paint)

        // 2. Metadata Section (Left)
        paint.color = Color.rgb(30, 41, 59)
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Employee Information & Audit Context", 30f, 100f, paint)

        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.rgb(71, 85, 105)
        canvas.drawText("Employee Name: ${settings.employeeName}", 30f, 118f, paint)
        canvas.drawText("Audit Period: $monthName", 30f, 134f, paint)
        canvas.drawText("Generated On: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())}", 30f, 150f, paint)
        canvas.drawText("Policy Goal: ${String.format(Locale.US, "%.1f", settings.minHours)}h/day • ${settings.targetDaysPerWeek} days/week", 30f, 166f, paint)

        // 3. Status Rating Badge (Right)
        val badgeRect = RectF(350f, 92f, 565f, 172f)
        paint.style = Paint.Style.FILL
        if (isCompliant) {
            paint.color = Color.rgb(236, 253, 245) // Light Emerald
            canvas.drawRoundRect(badgeRect, 8f, 8f, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.5f
            paint.color = Color.rgb(16, 185, 129) // Emerald
            canvas.drawRoundRect(badgeRect, 8f, 8f, paint)

            paint.style = Paint.Style.FILL
            paint.color = Color.rgb(5, 150, 105)
            paint.textSize = 11f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("STATUS: COMPLIANT [PASS]", 365f, 116f, paint)
        } else {
            paint.color = Color.rgb(254, 242, 242) // Light Red
            canvas.drawRoundRect(badgeRect, 8f, 8f, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.5f
            paint.color = Color.rgb(239, 68, 68) // Red
            canvas.drawRoundRect(badgeRect, 8f, 8f, paint)

            paint.style = Paint.Style.FILL
            paint.color = Color.rgb(220, 38, 38)
            paint.textSize = 11f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("STATUS: DEFICIT [NON-COMPLIANT]", 365f, 116f, paint)
        }

        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.rgb(71, 85, 105)
        canvas.drawText("Quota Met: $validDaysCount / $targetDays Days ($compliancePercentage%)", 365f, 136f, paint)
        canvas.drawText("Exclusions: $monthLeavesCount Approved Leaves/Holidays", 365f, 152f, paint)

        // 4. Metrics Summary Table
        var curY = 195f
        paint.color = Color.rgb(30, 41, 59)
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Compliance Metric Summary", 30f, curY, paint)

        curY += 10f
        val summaryRows = listOf(
            Pair("Required Monthly Goal", "$targetDays Valid Days (${String.format(Locale.US, "%.1f", settings.minHours)}h daily requirement)"),
            Pair("Valid Days Completed", "$validDaysCount Days (${if (validDaysCount >= targetDays) "Target Met" else "${targetDays - validDaysCount} Days Short"})"),
            Pair("Exclusions (Holidays/Leaves)", "$monthLeavesCount Days Excluded from Quota"),
            Pair("Total Office Time Logged", "${totalLoggedMinutes / 60}h ${totalLoggedMinutes % 60}m (${String.format(Locale.US, "%.1f", totalLoggedHours)} hours)")
        )

        paint.textSize = 9f
        for ((idx, row) in summaryRows.withIndex()) {
            val rowY = curY + 18f
            paint.color = if (idx % 2 == 0) Color.rgb(248, 250, 252) else Color.WHITE
            canvas.drawRect(30f, rowY - 14f, 565f, rowY + 8f, paint)

            paint.color = Color.rgb(51, 65, 85)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(row.first, 40f, rowY, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(row.second, 260f, rowY, paint)
            curY = rowY + 8f
        }

        // 5. Detailed Log History
        curY += 20f
        paint.color = Color.rgb(30, 41, 59)
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Detailed Office Check-in Log History", 30f, curY, paint)

        curY += 12f
        // Table Header
        paint.color = Color.rgb(51, 65, 85)
        canvas.drawRect(30f, curY, 565f, curY + 20f, paint)

        paint.color = Color.WHITE
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Date", 40f, curY + 14f, paint)
        canvas.drawText("Day", 130f, curY + 14f, paint)
        canvas.drawText("Punch In", 200f, curY + 14f, paint)
        canvas.drawText("Punch Out", 280f, curY + 14f, paint)
        canvas.drawText("Total Time", 370f, curY + 14f, paint)
        canvas.drawText("Compliance Status", 460f, curY + 14f, paint)
        curY += 20f

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val maxLogsToRender = minOf(monthLogs.size, 18) // prevent overflow on single A4 sheet
        if (monthLogs.isEmpty()) {
            paint.color = Color.rgb(100, 116, 139)
            canvas.drawText("No check-in records registered for this month.", 40f, curY + 18f, paint)
            curY += 26f
        } else {
            for (i in 0 until maxLogsToRender) {
                val log = monthLogs[i]
                val d = DateUtils.parseDate(log.date)
                val dayOfWeek = SimpleDateFormat("EEE", Locale.US).format(d)
                val isValid = log.isValidWFODay(settings.minHours)

                paint.color = if (i % 2 == 0) Color.rgb(248, 250, 252) else Color.WHITE
                canvas.drawRect(30f, curY, 565f, curY + 18f, paint)

                paint.color = Color.rgb(30, 41, 59)
                canvas.drawText(log.date, 40f, curY + 12f, paint)
                canvas.drawText(dayOfWeek, 130f, curY + 12f, paint)
                canvas.drawText(DateUtils.formatTime12(log.inTime), 200f, curY + 12f, paint)
                canvas.drawText(DateUtils.formatTime12(log.outTime), 280f, curY + 12f, paint)
                canvas.drawText(log.formattedDuration, 370f, curY + 12f, paint)

                if (isValid) {
                    paint.color = Color.rgb(5, 150, 105)
                    canvas.drawText("✓ Valid WFO", 460f, curY + 12f, paint)
                } else {
                    paint.color = Color.rgb(220, 38, 38)
                    canvas.drawText("⚠️ Left Early (<${String.format(Locale.US, "%.1f", settings.minHours)}h)", 460f, curY + 12f, paint)
                }
                curY += 18f
            }
            if (monthLogs.size > maxLogsToRender) {
                paint.color = Color.rgb(100, 116, 139)
                canvas.drawText("... and ${monthLogs.size - maxLogsToRender} more entries in this period", 40f, curY + 14f, paint)
                curY += 18f
            }
        }

        // 6. Signatures Section at the bottom
        val signY = 750f
        paint.color = Color.rgb(203, 213, 225)
        paint.strokeWidth = 1f
        canvas.drawLine(40f, signY, 230f, signY, paint)
        canvas.drawLine(350f, signY, 540f, signY, paint)

        paint.color = Color.rgb(100, 116, 139)
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Employee Signature", 40f, signY + 16f, paint)
        canvas.drawText("Reporting Manager / HR Signature", 350f, signY + 16f, paint)

        paint.textSize = 7.5f
        canvas.drawText("Generated by WFO Tracker & Insights Android App • Offline Verified Audit Seal", 40f, signY + 40f, paint)

        pdfDoc.finishPage(page)

        // Save PDF to cache reports dir
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val fileName = "WFO_Audit_Report_${year}_${String.format(Locale.US, "%02d", month + 1)}.pdf"
        val pdfFile = File(reportsDir, fileName)

        FileOutputStream(pdfFile).use { out ->
            pdfDoc.writeTo(out)
        }
        pdfDoc.close()

        return pdfFile
    }
}
