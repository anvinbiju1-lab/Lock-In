package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val description: String = "",
    val targetTime: String = "09:00", // HH:mm format
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val colorHex: String = "#38BDF8", // Theme color accent
    val iconName: String = "CheckCircle"
)
