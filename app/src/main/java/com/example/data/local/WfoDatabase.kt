package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [CheckInLog::class, LeaveHoliday::class],
    version = 1,
    exportSchema = false
)
abstract class WfoDatabase : RoomDatabase() {
    abstract fun checkInLogDao(): CheckInLogDao
    abstract fun leaveHolidayDao(): LeaveHolidayDao

    companion object {
        @Volatile
        private var INSTANCE: WfoDatabase? = null

        fun getInstance(context: Context): WfoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WfoDatabase::class.java,
                    "wfo_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
