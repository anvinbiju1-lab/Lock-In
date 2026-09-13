package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.FocusLockApp
import com.example.util.DateUtils
import com.example.util.HabitNotificationHelper
import kotlinx.coroutines.flow.first
import java.util.Calendar

class HabitReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val app = FocusLockApp.instance
            val habitRepository = app.habitRepository
            val activeHabits = habitRepository.activeHabits.first()
            val today = DateUtils.getTodayDateString()
            val completedLogs = habitRepository.getLogsForDate(today).first().filter { it.status }
            val completedHabitIds = completedLogs.map { it.habitId }.toSet()

            val cal = Calendar.getInstance()
            val currentHour = cal.get(Calendar.HOUR_OF_DAY)
            val currentMinute = cal.get(Calendar.MINUTE)
            val currentTotalMinutes = currentHour * 60 + currentMinute

            for (habit in activeHabits) {
                // If not already completed today
                if (!completedHabitIds.contains(habit.id)) {
                    val parts = habit.targetTime.split(":")
                    if (parts.size == 2) {
                        val targetH = parts[0].toIntOrNull() ?: 9
                        val targetM = parts[1].toIntOrNull() ?: 0
                        val targetTotalMinutes = targetH * 60 + targetM

                        // Trigger if within a 30-minute window of the target time, or if evening check-in (20:00+)
                        val diff = currentTotalMinutes - targetTotalMinutes
                        if (diff in 0..45 || currentHour >= 20) {
                            HabitNotificationHelper.showHabitPrompt(context, habit)
                        }
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
