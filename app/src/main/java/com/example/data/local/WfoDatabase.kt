package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CheckInLog::class, LeaveHoliday::class],
    version = 2,
    exportSchema = false
)
abstract class WfoDatabase : RoomDatabase() {
    abstract fun checkInLogDao(): CheckInLogDao
    abstract fun leaveHolidayDao(): LeaveHolidayDao

    companion object {
        @Volatile
        private var INSTANCE: WfoDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                data class RawLeave(val id: String, val type: String, val title: String, val date: String)

                val existing = mutableListOf<RawLeave>()
                db.query("SELECT id, type, title, date FROM wfo_leaves").use { cursor ->
                    val idIdx = cursor.getColumnIndexOrThrow("id")
                    val typeIdx = cursor.getColumnIndexOrThrow("type")
                    val titleIdx = cursor.getColumnIndexOrThrow("title")
                    val dateIdx = cursor.getColumnIndexOrThrow("date")
                    while (cursor.moveToNext()) {
                        existing.add(
                            RawLeave(
                                id = cursor.getString(idIdx),
                                type = cursor.getString(typeIdx),
                                title = cursor.getString(titleIdx),
                                date = cursor.getString(dateIdx)
                            )
                        )
                    }
                }

                val deduplicated = existing
                    .groupBy { it.date to it.type }
                    .values
                    .map { group -> group.maxBy { it.title.length } }

                db.execSQL("DELETE FROM wfo_leaves")
                for (row in deduplicated) {
                    val normalizedId = "${row.type}-${row.date}"
                    db.execSQL(
                        "INSERT OR REPLACE INTO wfo_leaves (id, type, title, date) VALUES (?, ?, ?, ?)",
                        arrayOf<Any>(normalizedId, row.type, row.title, row.date)
                    )
                }
            }
        }

        fun getInstance(context: Context): WfoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WfoDatabase::class.java,
                    "wfo_database.db"
                ).addMigrations(MIGRATION_1_2).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
