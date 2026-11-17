package com.example

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.LeaveDefaults
import com.example.data.local.LeaveHoliday
import com.example.data.local.WfoDatabase
import com.example.data.preferences.WfoPreferences
import com.example.data.preferences.dataStore
import com.example.data.repository.WfoRepository
import com.example.util.HolidayImportParser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LeavesDataIntegrityTest {

    private lateinit var context: Context
    private lateinit var database: WfoDatabase
    private lateinit var repository: WfoRepository

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        context.dataStore.edit { it.clear() }
        database = Room.inMemoryDatabaseBuilder(context, WfoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val prefs = WfoPreferences(context, encryptFn = { it }, decryptFn = { it })
        repository = WfoRepository(
            context = context,
            database = database,
            preferences = prefs
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertLeaveRangeUsesDeterministicIdPreventsDuplicatesAndSkipsWeekends() = runBlocking {
        // 2026-01-05 (Mon) through 2026-01-11 (Sun): 5 weekdays + 2 weekend days
        val firstResult = repository.insertLeaveRange(
            startDateStr = "2026-01-05",
            endDateStr = "2026-01-11",
            type = LeaveDefaults.TYPE_HOLIDAY,
            title = "" // should resolve via LeaveDefaults
        ).getOrThrow()

        assertEquals(5, firstResult.added)
        assertEquals(2, firstResult.skippedWeekends)
        assertEquals(0, firstResult.skippedDuplicates)

        val storedAfterFirst = database.leaveHolidayDao().getAllLeaves().first()
        assertEquals(5, storedAfterFirst.size)
        assertTrue(storedAfterFirst.all { it.id == "holiday-${it.date}" })
        assertTrue(storedAfterFirst.all { it.title == "Holiday" })

        // Inserting overlapping range 2026-01-07..2026-01-09 must skip duplicates, not double-insert
        val secondResult = repository.insertLeaveRange(
            startDateStr = "2026-01-07",
            endDateStr = "2026-01-09",
            type = LeaveDefaults.TYPE_HOLIDAY,
            title = "Holiday"
        ).getOrThrow()

        assertEquals(0, secondResult.added)
        assertEquals(0, secondResult.skippedWeekends)
        assertEquals(3, secondResult.skippedDuplicates)
        assertEquals(5, database.leaveHolidayDao().getAllLeaves().first().size)
    }

    @Test
    fun insertLeaveRangeRejectsInvalidDatesStrictlyWithoutDefaultingToToday() = runBlocking {
        val invalidFebResult = repository.insertLeaveRange(
            startDateStr = "2026-02-30",
            endDateStr = "2026-03-02",
            type = LeaveDefaults.TYPE_LEAVE,
            title = "Trip"
        )
        assertTrue("2026-02-30 must fail strict validation", invalidFebResult.isFailure)

        val garbageResult = repository.insertLeaveRange(
            startDateStr = "not-a-date",
            endDateStr = "2026-03-02",
            type = LeaveDefaults.TYPE_SICK,
            title = ""
        )
        assertTrue("Non-ISO date must fail strict validation", garbageResult.isFailure)

        assertEquals(0, database.leaveHolidayDao().getAllLeaves().first().size)
    }

    @Test
    fun leaveDefaultsConsistentAcrossTypes() {
        assertEquals("Holiday", LeaveDefaults.defaultTitleFor(LeaveDefaults.TYPE_HOLIDAY))
        assertEquals("Sick leave", LeaveDefaults.defaultTitleFor(LeaveDefaults.TYPE_SICK))
        assertEquals("Vacation", LeaveDefaults.defaultTitleFor(LeaveDefaults.TYPE_LEAVE))
        assertEquals("Working from home", LeaveDefaults.defaultTitleFor(LeaveDefaults.TYPE_WFH))
        assertTrue(LeaveDefaults.TYPE_WFH !in LeaveDefaults.mutedGeofenceTypes)
        assertTrue(LeaveDefaults.TYPE_HOLIDAY in LeaveDefaults.mutedGeofenceTypes)
        assertTrue(LeaveDefaults.TYPE_SICK in LeaveDefaults.mutedGeofenceTypes)
        assertTrue(LeaveDefaults.TYPE_LEAVE in LeaveDefaults.mutedGeofenceTypes)
    }

    @Test
    fun migration1To2DeduplicatesLeavesKeepingLongestTitleAndNormalizingId() {
        val dbFile = File(context.cacheDir, "migration_test_v1.db").apply {
            if (exists()) delete()
        }
        val factory = FrameworkSQLiteOpenHelperFactory()
        val helper = factory.create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbFile.absolutePath)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `wfo_leaves` (`id` TEXT NOT NULL, `type` TEXT NOT NULL, `title` TEXT NOT NULL, `date` TEXT NOT NULL, PRIMARY KEY(`id`))"
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )

        val db = helper.writableDatabase
        db.execSQL("INSERT INTO wfo_leaves (id, type, title, date) VALUES ('uuid-a', 'holiday', 'Hol', '2026-01-26')")
        db.execSQL("INSERT INTO wfo_leaves (id, type, title, date) VALUES ('uuid-b', 'holiday', 'Republic Day Holiday', '2026-01-26')")
        db.execSQL("INSERT INTO wfo_leaves (id, type, title, date) VALUES ('uuid-c', 'sick', 'Fever', '2026-02-10')")

        WfoDatabase.MIGRATION_1_2.migrate(db)

        val rows = mutableListOf<LeaveHoliday>()
        db.query("SELECT id, type, title, date FROM wfo_leaves ORDER BY date ASC").use { c ->
            while (c.moveToNext()) {
                rows.add(
                    LeaveHoliday(
                        id = c.getString(0),
                        type = c.getString(1),
                        title = c.getString(2),
                        date = c.getString(3)
                    )
                )
            }
        }
        helper.close()
        dbFile.delete()

        assertEquals(2, rows.size)
        assertEquals("holiday-2026-01-26", rows[0].id)
        assertEquals("Republic Day Holiday", rows[0].title)
        assertEquals("sick-2026-02-10", rows[1].id)
        assertEquals("Fever", rows[1].title)
    }

    @Test
    fun holidayImportParserHandlesIcsExclusiveDtendRruleAndPlainTextFormats() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            BEGIN:VEVENT
            DTSTART;VALUE=DATE:20260126
            DTEND;VALUE=DATE:20260127
            SUMMARY:Republic Day
            END:VEVENT
            BEGIN:VEVENT
            DTSTART;VALUE=DATE:20260501
            DTEND;VALUE=DATE:20260502
            RRULE:FREQ=YEARLY
            SUMMARY:Recurring May Day
            END:VEVENT
            BEGIN:VEVENT
            DTSTART;VALUE=DATE:20260815
            DTEND;VALUE=DATE:20260816
            SUMMARY:Independence Day
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        // 2026-01-26 is Monday, 2026-08-15 is Saturday (weekend)
        val existing = listOf(
            LeaveHoliday(id = "holiday-2026-01-26", type = "holiday", title = "Existing", date = "2026-01-26")
        )
        val icsResult = HolidayImportParser.parseInput(ics, existingLeaves = existing, currentYear = 2026)

        assertEquals(1, icsResult.skippedRecurringCount)
        assertEquals(2, icsResult.candidates.size)

        val jan26 = icsResult.candidates[0]
        assertEquals("2026-01-26", jan26.date)
        assertEquals("Republic Day", jan26.title)
        assertTrue(jan26.isDuplicate)
        assertFalse(jan26.isWeekend)
        assertFalse(jan26.selectedByDefault)

        val aug15 = icsResult.candidates[1]
        assertEquals("2026-08-15", aug15.date)
        assertTrue(aug15.isWeekend)
        assertFalse(aug15.selectedByDefault)

        val plainText = """
            2026-10-02, Gandhi Jayanti
            25-12-2026, Christmas
            1 Jan 2027, New Year
            bad-date-line
        """.trimIndent()

        val textResult = HolidayImportParser.parseInput(plainText, existingLeaves = emptyList(), currentYear = 2026)
        assertEquals(1, textResult.unparsedLinesCount)
        assertEquals(3, textResult.candidates.size)
        assertEquals("2026-10-02", textResult.candidates[0].date)
        assertEquals("Gandhi Jayanti", textResult.candidates[0].title)
        assertTrue(textResult.candidates[0].selectedByDefault)
        assertFalse(textResult.candidates[0].isOutsideCurrentYear)

        assertEquals("2026-12-25", textResult.candidates[1].date)
        assertEquals("2027-01-01", textResult.candidates[2].date)
        assertTrue(textResult.candidates[2].isOutsideCurrentYear)
        assertTrue(textResult.candidates[2].selectedByDefault)
    }
}
