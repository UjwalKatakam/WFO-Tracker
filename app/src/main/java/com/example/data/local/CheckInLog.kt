package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Locale

@Entity(tableName = "wfo_logs")
data class CheckInLog(
    @PrimaryKey
    val date: String, // YYYY-MM-DD
    val inTime: String, // HH:MM
    val outTime: String, // HH:MM
    val durationMinutes: Int,
    val notes: String? = null
) {
    val totalHours: Double
        get() = durationMinutes / 60.0

    fun isValidWFODay(minHours: Double): Boolean {
        return totalHours >= minHours
    }

    val formattedDuration: String
        get() {
            val h = durationMinutes / 60
            val m = durationMinutes % 60
            return when {
                h == 0 -> "${m}m"
                m == 0 -> "${h}h"
                else -> "${h}h ${m}m"
            }
        }

    val formattedOutTime: String
        get() = outTime

    val formattedInTime: String
        get() = inTime
}
