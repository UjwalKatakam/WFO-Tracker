package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LeaveHolidayDao {
    @Query("SELECT * FROM wfo_leaves ORDER BY date DESC")
    fun getAllLeaves(): Flow<List<LeaveHoliday>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeave(leave: LeaveHoliday)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(leaves: List<LeaveHoliday>)

    @Query("DELETE FROM wfo_leaves WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM wfo_leaves WHERE date = :date")
    suspend fun deleteByDate(date: String)

    @Query("DELETE FROM wfo_leaves")
    suspend fun clearAll()
}
