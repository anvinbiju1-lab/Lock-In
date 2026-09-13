package com.example.data.repository

import com.example.data.dao.SleepDao
import com.example.data.entity.SleepLogEntity
import com.example.util.DateUtils
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SleepRepository(private val sleepDao: SleepDao) {

    val allSleepLogs: Flow<List<SleepLogEntity>> = sleepDao.getAllSleepLogs()
    val recentSleepLogs: Flow<List<SleepLogEntity>> = sleepDao.getRecentSleepLogs(14)

    fun getSleepLogForDateFlow(dateString: String): Flow<SleepLogEntity?> {
        return sleepDao.getSleepLogForDateFlow(dateString)
    }

    suspend fun getSleepLogForDate(dateString: String): SleepLogEntity? {
        return sleepDao.getSleepLogForDate(dateString)
    }

    suspend fun saveSleepSchedule(
        dateString: String,
        targetBedHour: Int,
        targetBedMin: Int,
        targetWakeHour: Int,
        targetWakeMin: Int,
        actualBedHour: Int,
        actualBedMin: Int,
        actualWakeHour: Int,
        actualWakeMin: Int,
        qualityRating: Int = 4,
        bedtimeFocusEnabled: Boolean = true,
        notes: String = ""
    ): Long {
        // Calculate duration and delta offset
        val isPostMidnight = actualBedHour in 0..5
        val durationMinutes = calculateDuration(actualBedHour, actualBedMin, actualWakeHour, actualWakeMin)
        val deltaBedtimeMinutes = calculateDeltaMinutes(actualBedHour, actualBedMin, targetBedHour, targetBedMin)

        val status = when {
            isPostMidnight -> "POST_MIDNIGHT"
            deltaBedtimeMinutes < -15 -> "EARLY"
            deltaBedtimeMinutes in -15..15 -> "ON_TIME"
            deltaBedtimeMinutes in 16..60 -> "LATE"
            else -> "VERY_LATE"
        }

        // Calculate sleep score (1-100) based on duration, punctuality, and rating
        val durationScore = when {
            durationMinutes in 420..540 -> 40 // 7-9 hours ideal
            durationMinutes in 360..600 -> 30 // 6-10 hours acceptable
            else -> 15
        }
        val punctualityScore = when {
            deltaBedtimeMinutes <= 0 -> 40 // Early or on-time
            deltaBedtimeMinutes in 1..30 -> 30
            deltaBedtimeMinutes in 31..90 -> 20
            else -> 10
        }
        val ratingScore = (qualityRating * 4).coerceIn(4, 20)
        val totalScore = (durationScore + punctualityScore + ratingScore).coerceIn(20, 100)

        val existing = sleepDao.getSleepLogForDate(dateString)
        val entity = SleepLogEntity(
            id = existing?.id ?: 0,
            dateString = dateString,
            targetBedtimeHour = targetBedHour,
            targetBedtimeMinute = targetBedMin,
            targetWakeHour = targetWakeHour,
            targetWakeMinute = targetWakeMin,
            actualBedtimeHour = actualBedHour,
            actualBedtimeMinute = actualBedMin,
            actualWakeHour = actualWakeHour,
            actualWakeMinute = actualWakeMin,
            durationMinutes = durationMinutes,
            sleepScore = totalScore,
            qualityRating = qualityRating,
            status = status,
            deltaBedtimeMinutes = deltaBedtimeMinutes,
            isPostMidnight = isPostMidnight,
            bedtimeFocusEnabled = bedtimeFocusEnabled,
            notes = notes
        )

        return sleepDao.insertSleepLog(entity)
    }

    companion object {
        fun calculateDuration(bedHour: Int, bedMin: Int, wakeHour: Int, wakeMin: Int): Int {
            val bedTotalMinutes = if (bedHour < 12) {
                // Post midnight (e.g. 00:30 is 24*60 + 30 = 1470)
                (bedHour + 24) * 60 + bedMin
            } else {
                bedHour * 60 + bedMin
            }

            val wakeTotalMinutes = (wakeHour + 24) * 60 + wakeMin
            var diff = wakeTotalMinutes - bedTotalMinutes
            if (diff < 0) diff += 24 * 60
            return diff
        }

        fun calculateDeltaMinutes(
            actualHour: Int,
            actualMin: Int,
            targetHour: Int,
            targetMin: Int
        ): Int {
            val actualNorm = if (actualHour in 0..12) (actualHour + 24) * 60 + actualMin else actualHour * 60 + actualMin
            val targetNorm = if (targetHour in 0..12) (targetHour + 24) * 60 + targetMin else targetHour * 60 + targetMin
            return actualNorm - targetNorm
        }

        fun formatTime12H(hour: Int, minute: Int): String {
            val amPm = if (hour >= 12) "PM" else "AM"
            val displayHour = when (hour % 12) {
                0 -> 12
                else -> hour % 12
            }
            return String.format(Locale.getDefault(), "%d:%02d %s", displayHour, minute, amPm)
        }

        fun formatDuration(durationMinutes: Int): String {
            val hours = durationMinutes / 60
            val minutes = durationMinutes % 60
            return if (minutes > 0) "${hours}h ${minutes}m" else "${hours}h"
        }
    }
}
