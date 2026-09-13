package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sleep_logs")
data class SleepLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateString: String, // "yyyy-MM-dd"
    val targetBedtimeHour: Int = 23, // 11 PM default
    val targetBedtimeMinute: Int = 0,
    val targetWakeHour: Int = 7, // 7 AM default
    val targetWakeMinute: Int = 0,
    val actualBedtimeHour: Int = 23,
    val actualBedtimeMinute: Int = 0,
    val actualWakeHour: Int = 7,
    val actualWakeMinute: Int = 0,
    val durationMinutes: Int = 480, // 8 hours
    val sleepScore: Int = 85, // 1 - 100
    val qualityRating: Int = 4, // 1 - 5 stars
    val status: String = "ON_TIME", // "EARLY", "ON_TIME", "LATE", "POST_MIDNIGHT"
    val deltaBedtimeMinutes: Int = 0, // Negative = Early, Positive = Late
    val isPostMidnight: Boolean = false, // True if bedtime is between 00:00 and 06:00
    val bedtimeFocusEnabled: Boolean = true,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
