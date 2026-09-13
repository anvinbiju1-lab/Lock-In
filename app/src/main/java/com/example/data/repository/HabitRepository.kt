package com.example.data.repository

import com.example.data.dao.HabitDao
import com.example.data.entity.HabitEntity
import com.example.data.entity.HabitLogEntity
import com.example.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Calendar

class HabitRepository(private val habitDao: HabitDao) {

    val allHabits: Flow<List<HabitEntity>> = habitDao.getAllHabits()
    val activeHabits: Flow<List<HabitEntity>> = habitDao.getActiveHabits()
    val totalCompletedCount: Flow<Int> = habitDao.getTotalCompletedCount()

    fun getLogsForDate(date: String): Flow<List<HabitLogEntity>> {
        return habitDao.getLogsForDate(date)
    }

    fun getLogsForMonth(yearMonthPrefix: String): Flow<List<HabitLogEntity>> {
        return habitDao.getLogsForMonth(yearMonthPrefix)
    }

    fun getAllLogs(): Flow<List<HabitLogEntity>> {
        return habitDao.getAllLogs()
    }

    suspend fun insertHabit(habit: HabitEntity): Long {
        return habitDao.insertHabit(habit)
    }

    suspend fun updateHabit(habit: HabitEntity) {
        habitDao.updateHabit(habit)
    }

    suspend fun deleteHabit(habitId: Long) {
        habitDao.deleteHabitById(habitId)
    }

    suspend fun logHabitCompletion(habitId: Long, date: String, status: Boolean = true): Long {
        val log = HabitLogEntity(
            habitId = habitId,
            completedDate = date,
            completedTimestamp = System.currentTimeMillis(),
            status = status
        )
        return habitDao.insertLog(log)
    }

    suspend fun toggleHabitStatus(habitId: Long, date: String): Boolean {
        val existing = habitDao.getLogsForDateDirect(date).find { it.habitId == habitId }
        return if (existing != null && existing.status) {
            habitDao.deleteLogForDate(habitId, date)
            false
        } else {
            habitDao.insertLog(
                HabitLogEntity(
                    habitId = habitId,
                    completedDate = date,
                    completedTimestamp = System.currentTimeMillis(),
                    status = true
                )
            )
            true
        }
    }

    suspend fun calculateCurrentStreak(): Int {
        val habits = habitDao.getActiveHabits().first()
        if (habits.isEmpty()) return 0

        var streak = 0
        val cal = Calendar.getInstance()
        var checkDate = DateUtils.formatDate(cal.timeInMillis)
        var logsToday = habitDao.getLogsForDateDirect(checkDate).filter { it.status }

        // If today has completions, count today
        if (logsToday.isNotEmpty()) {
            streak++
            cal.add(Calendar.DAY_OF_YEAR, -1)
        } else {
            // Check yesterday
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }

        // Loop backward
        while (true) {
            checkDate = DateUtils.formatDate(cal.timeInMillis)
            val pastLogs = habitDao.getLogsForDateDirect(checkDate).filter { it.status }
            if (pastLogs.isNotEmpty()) {
                streak++
                cal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }

        return streak
    }
}
