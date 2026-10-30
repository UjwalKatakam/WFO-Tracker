package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "wfo_leaves")
data class LeaveHoliday(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val type: String, // "holiday" | "leave" | "sick"
    val title: String,
    val date: String // YYYY-MM-DD
)
