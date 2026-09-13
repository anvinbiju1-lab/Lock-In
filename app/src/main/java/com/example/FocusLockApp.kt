package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.AppDatabase
import com.example.data.repository.AppBlockerRepository
import com.example.data.repository.HabitRepository
import com.example.data.repository.SleepRepository
import com.example.worker.HabitReminderWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class FocusLockApp : Application(), Configuration.Provider {

    lateinit var database: AppDatabase
        private set

    lateinit var habitRepository: HabitRepository
        private set

    lateinit var appBlockerRepository: AppBlockerRepository
        private set

    lateinit var sleepRepository: SleepRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getDatabase(this)
        habitRepository = HabitRepository(database.habitDao())
        appBlockerRepository = AppBlockerRepository(database.appBlockerDao())
        sleepRepository = SleepRepository(database.sleepDao())

        createNotificationChannels()
        scheduleHabitCheckWorker()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Channel 1: Foreground Monitoring Service
            val monitorChannel = NotificationChannel(
                CHANNEL_MONITOR_SERVICE,
                "App Blocker Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing foreground protection and app limit enforcement"
                setShowBadge(false)
            }

            // Channel 2: Habit Daily Reminders
            val habitChannel = NotificationChannel(
                CHANNEL_HABIT_REMINDERS,
                "Daily Habit Check-ins",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily check-in prompts with quick-action responses"
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(monitorChannel)
            notificationManager.createNotificationChannel(habitChannel)
        }
    }

    fun scheduleHabitCheckWorker() {
        try {
            val habitWorkRequest = PeriodicWorkRequestBuilder<HabitReminderWorker>(
                15, TimeUnit.MINUTES // Checks habit schedules periodically
            ).build()

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "HabitReminderWorker",
                ExistingPeriodicWorkPolicy.KEEP,
                habitWorkRequest
            )
        } catch (e: Exception) {
            // Error scheduling worker
        }
    }

    companion object {
        const val CHANNEL_MONITOR_SERVICE = "focus_lock_monitor_channel"
        const val CHANNEL_HABIT_REMINDERS = "focus_lock_habit_channel"

        lateinit var instance: FocusLockApp
            private set
    }
}
