package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "wfo_leaves")
data class LeaveHoliday(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val type: String, // "holiday" | "leave" | "sick" | "wfh"
    val title: String,
    val date: String // YYYY-MM-DD
)

data class LeaveRangeResult(
    val added: Int,
    val skippedWeekends: Int,
    val skippedDuplicates: Int
)

object LeaveDefaults {
    const val TYPE_HOLIDAY = "holiday"
    const val TYPE_SICK = "sick"
    const val TYPE_LEAVE = "leave"
    const val TYPE_WFH = "wfh"

    val typeOptions: List<Pair<String, String>> = listOf(
        TYPE_HOLIDAY to "Holiday",
        TYPE_SICK to "Sick",
        TYPE_LEAVE to "Vacation",
        TYPE_WFH to "WFH"
    )

    val mutedGeofenceTypes: Set<String> = setOf(
        TYPE_HOLIDAY,
        TYPE_SICK,
        TYPE_LEAVE
    )

    fun defaultTitleFor(type: String): String = when (type) {
        TYPE_HOLIDAY -> "Holiday"
        TYPE_SICK -> "Sick leave"
        TYPE_LEAVE -> "Vacation"
        TYPE_WFH -> "Working from home"
        else -> "Excused absence"
    }

    fun chipLabelFor(type: String): String = when (type) {
        TYPE_HOLIDAY -> "Holiday"
        TYPE_SICK -> "Sick"
        TYPE_LEAVE -> "Vacation"
        TYPE_WFH -> "WFH"
        else -> type.replaceFirstChar { it.titlecase(java.util.Locale.US) }
    }
}
