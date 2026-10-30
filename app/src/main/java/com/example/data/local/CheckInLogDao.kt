package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckInLogDao {
    @Query("SELECT * FROM wfo_logs ORDER BY date DESC")
    fun getAllLogs(): Flow<List<CheckInLog>>

    @Query("SELECT * FROM wfo_logs WHERE date = :date LIMIT 1")
    suspend fun getLogByDate(date: String): CheckInLog?

    @Query("SELECT * FROM wfo_logs WHERE date = :date LIMIT 1")
    fun observeLogByDate(date: String): Flow<CheckInLog?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: CheckInLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<CheckInLog>)

    @Query("DELETE FROM wfo_logs WHERE date = :date")
    suspend fun deleteByDate(date: String)

    @Query("DELETE FROM wfo_logs")
    suspend fun clearAll()
}
