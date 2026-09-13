package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.entity.AppRestrictionEntity
import com.example.data.entity.EmergencyUnlockEntity
import com.example.data.entity.HabitEntity
import com.example.data.entity.HabitLogEntity
import com.example.data.entity.SleepLogEntity
import com.example.data.repository.AppBlockerRepository
import com.example.data.repository.HabitRepository
import com.example.data.repository.SleepRepository
import com.example.data.repository.UserProfileRepository
import com.example.util.DateUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var database: AppDatabase
    private lateinit var habitRepository: HabitRepository
    private lateinit var blockerRepository: AppBlockerRepository
    private lateinit var sleepRepository: SleepRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        habitRepository = HabitRepository(database.habitDao())
        blockerRepository = AppBlockerRepository(database.appBlockerDao())
        sleepRepository = SleepRepository(database.sleepDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `verify user profile initials derivation and persistence`() {
        // Test initials logic
        assertEquals("AB", UserProfileRepository.deriveInitials("Anvin Biju"))
        assertEquals("AB", UserProfileRepository.deriveInitials("Alex Brown"))
        assertEquals("JD", UserProfileRepository.deriveInitials("John Doe"))
        assertEquals("FL", UserProfileRepository.deriveInitials("Focus Lock"))
        assertEquals("AL", UserProfileRepository.deriveInitials("Alex"))
        assertEquals("AB", UserProfileRepository.deriveInitials(""))

        // Test UserProfileRepository
        val context = ApplicationProvider.getApplicationContext<Context>()
        val profileRepo = UserProfileRepository(context)
        assertEquals("AB", profileRepo.profileState.value.initials)

        profileRepo.updateProfile("Sarah Connor", "Be relentless", "#38BDF8")
        val updated = profileRepo.profileState.value
        assertEquals("Sarah Connor", updated.name)
        assertEquals("SC", updated.initials)
        assertEquals("Be relentless", updated.focusMotto)
        assertEquals("#38BDF8", updated.avatarColorHex)

        // Test onboarding & first launch flags
        assertFalse(profileRepo.hasCompletedPermissionOnboarding())
        profileRepo.setPermissionOnboardingCompleted(true)
        assertTrue(profileRepo.hasCompletedPermissionOnboarding())

        profileRepo.markFirstLaunchComplete()
        assertFalse(profileRepo.isFirstLaunch())
    }

    @Test
    fun `verify app name resource`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("FocusLock", appName)
    }

    @Test
    fun `verify date utils time window logic`() {
        // Standard daytime window: 09:00 to 17:00
        assertTrue(DateUtils.isWithinTimeWindow("09:00", "09:00", "17:00"))
        assertTrue(DateUtils.isWithinTimeWindow("12:30", "09:00", "17:00"))
        assertTrue(DateUtils.isWithinTimeWindow("17:00", "09:00", "17:00"))
        assertFalse(DateUtils.isWithinTimeWindow("08:59", "09:00", "17:00"))
        assertFalse(DateUtils.isWithinTimeWindow("17:01", "09:00", "17:00"))

        // Cross-midnight window: 22:00 to 06:00
        assertTrue(DateUtils.isWithinTimeWindow("23:15", "22:00", "06:00"))
        assertTrue(DateUtils.isWithinTimeWindow("02:00", "22:00", "06:00"))
        assertTrue(DateUtils.isWithinTimeWindow("06:00", "22:00", "06:00"))
        assertFalse(DateUtils.isWithinTimeWindow("06:01", "22:00", "06:00"))
        assertFalse(DateUtils.isWithinTimeWindow("21:59", "22:00", "06:00"))
    }

    @Test
    fun `verify habit logging and streak calculation`() = runBlocking {
        val habit1Id = habitRepository.insertHabit(
            HabitEntity(
                title = "Deep Focus Work",
                description = "45 mins deep work",
                targetTime = "09:00",
                colorHex = "#D0BCFF"
            )
        )
        val habit2Id = habitRepository.insertHabit(
            HabitEntity(
                title = "Hydration & Exercise",
                description = "Drink water & stretch",
                targetTime = "14:00",
                colorHex = "#7DDC97"
            )
        )

        val habits = habitRepository.allHabits.first()
        assertEquals(2, habits.size)

        val today = DateUtils.getTodayDateString()
        habitRepository.logHabitCompletion(habit1Id, today, true)
        habitRepository.logHabitCompletion(habit2Id, today, true)

        val todayLogs = habitRepository.getLogsForDate(today).first()
        assertEquals(2, todayLogs.size)
        assertTrue(todayLogs.all { it.status })

        val streak = habitRepository.calculateCurrentStreak()
        assertEquals(1, streak)

        // Toggle one off
        habitRepository.logHabitCompletion(habit2Id, today, false)
        val updatedLogs = habitRepository.getLogsForDate(today).first()
        val habit2Log = updatedLogs.find { it.habitId == habit2Id }
        assertNotNull(habit2Log)
        assertFalse(habit2Log!!.status)
    }

    @Test
    fun `verify app blocker evaluation and emergency bypass`() = runBlocking {
        val pkg = "com.social.distraction"
        blockerRepository.saveRestriction(
            AppRestrictionEntity(
                packageName = pkg,
                appName = "Social App",
                isRestricted = true,
                dailyLimitMinutes = 30,
                scheduleEnabled = false
            )
        )

        // Below limit (20 min spent) -> should NOT block
        val resultUnder = blockerRepository.evaluateAppBlock(pkg, 20)
        assertFalse(resultUnder.shouldBlock)

        // Above limit (35 min spent) -> SHOULD block
        val resultOver = blockerRepository.evaluateAppBlock(pkg, 35)
        assertTrue(resultOver.shouldBlock)
        assertTrue(resultOver.reason.contains("Daily Limit Reached"))
        assertTrue(resultOver.reason.contains("30 min limit exceeded"))

        // Trigger emergency 5-min unlock
        blockerRepository.recordEmergencyUnlock(pkg, "Social App")
        val resultEmergency = blockerRepository.evaluateAppBlock(pkg, 35)
        assertFalse(resultEmergency.shouldBlock)
        assertTrue(resultEmergency.isEmergencyActive)

        // Verify emergency record stored
        val emergencyList = blockerRepository.allEmergencyUnlocks.first()
        assertEquals(1, emergencyList.size)
        assertEquals(pkg, emergencyList[0].packageName)
    }

    @Test
    fun `verify scheduled lockout window blocking`() = runBlocking {
        val pkg = "com.gaming.app"
        val currentTime = DateUtils.getCurrentTimeHHmm()
        val parts = currentTime.split(":")
        val curH = parts[0].toInt()

        // Create window spanning current hour
        val startH = String.format("%02d:00", (curH - 1 + 24) % 24)
        val endH = String.format("%02d:59", (curH + 1) % 24)

        blockerRepository.saveRestriction(
            AppRestrictionEntity(
                packageName = pkg,
                appName = "Gaming App",
                isRestricted = true,
                dailyLimitMinutes = 120, // high limit
                scheduleEnabled = true,
                scheduleStart = startH,
                scheduleEnd = endH
            )
        )

        val result = blockerRepository.evaluateAppBlock(pkg, 10)
        assertTrue(result.shouldBlock)
        assertTrue(result.reason.contains("Scheduled Lockout Window"))
    }

    @Test
    fun `verify sleep duration and midnight boundary calculations`() {
        // Normal pre-midnight bedtime: 22:30 to 06:30 -> 8 hours (480 min)
        val dur1 = SleepRepository.calculateDuration(22, 30, 6, 30)
        assertEquals(480, dur1)

        // Cross-midnight bedtime: 23:45 to 07:15 -> 7 hours 30 min (450 min)
        val dur2 = SleepRepository.calculateDuration(23, 45, 7, 15)
        assertEquals(450, dur2)

        // Post-midnight bedtime: 00:30 (12:30 AM) to 08:00 -> 7 hours 30 min (450 min)
        val dur3 = SleepRepository.calculateDuration(0, 30, 8, 0)
        assertEquals(450, dur3)

        // Post-midnight late bedtime: 01:45 (01:45 AM) to 07:15 -> 5 hours 30 min (330 min)
        val dur4 = SleepRepository.calculateDuration(1, 45, 7, 15)
        assertEquals(330, dur4)

        // Delta calculations vs Target 23:00 (11:00 PM)
        // Early (10:15 PM) -> -45 min
        val deltaEarly = SleepRepository.calculateDeltaMinutes(22, 15, 23, 0)
        assertEquals(-45, deltaEarly)

        // Late (11:30 PM) -> +30 min
        val deltaLate = SleepRepository.calculateDeltaMinutes(23, 30, 23, 0)
        assertEquals(30, deltaLate)

        // Post-midnight (12:30 AM) -> +90 min
        val deltaPostMidnight = SleepRepository.calculateDeltaMinutes(0, 30, 23, 0)
        assertEquals(90, deltaPostMidnight)

        // Format helpers
        assertEquals("10:30 PM", SleepRepository.formatTime12H(22, 30))
        assertEquals("12:00 AM", SleepRepository.formatTime12H(0, 0))
        assertEquals("12:30 AM", SleepRepository.formatTime12H(0, 30))
        assertEquals("1:15 AM", SleepRepository.formatTime12H(1, 15))
        assertEquals("7h 30m", SleepRepository.formatDuration(450))
    }

    @Test
    fun `verify sleep repository schedule persistence and seeding`() = runBlocking {
        // Seed initial history
        sleepRepository.seedInitialHistoryIfEmpty()
        val initialLogs = sleepRepository.recentSleepLogs.first()
        assertTrue(initialLogs.isNotEmpty())
        assertEquals(7, initialLogs.size)

        // Test saving today's sleep schedule
        val today = DateUtils.getTodayDateString()
        sleepRepository.saveSleepSchedule(
            dateString = today,
            targetBedHour = 23,
            targetBedMin = 0,
            targetWakeHour = 7,
            targetWakeMin = 0,
            actualBedHour = 0, // 12:30 AM post-midnight
            actualBedMin = 30,
            actualWakeHour = 7,
            actualWakeMin = 30,
            qualityRating = 5,
            bedtimeFocusEnabled = true,
            notes = "Late night work done"
        )

        val savedLog = sleepRepository.getSleepLogForDate(today)
        assertNotNull(savedLog)
        assertEquals(0, savedLog!!.actualBedtimeHour)
        assertEquals(30, savedLog.actualBedtimeMinute)
        assertTrue(savedLog.isPostMidnight)
        assertEquals("POST_MIDNIGHT", savedLog.status)
        assertEquals(420, savedLog.durationMinutes) // 7 hours
        assertEquals(5, savedLog.qualityRating)
    }
}
