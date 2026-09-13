package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_restrictions")
data class AppRestrictionEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val isRestricted: Boolean = true,
    val dailyLimitMinutes: Int = 20, // max minutes/day
    val scheduleEnabled: Boolean = false,
    val scheduleStart: String = "09:00", // HH:mm
    val scheduleEnd: String = "17:00", // HH:mm
    val strictMode: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)
