package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.FocusLockApp
import com.example.util.DateUtils
import com.example.util.HabitNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HabitActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        val action = intent.action ?: return
        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
        val habitTitle = intent.getStringExtra(EXTRA_HABIT_TITLE) ?: "Habit"
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        if (notificationId != -1) {
            HabitNotificationHelper.cancelNotification(context, notificationId)
        }

        if (habitId == -1L) return

        when (action) {
            ACTION_HABIT_DONE -> {
                val today = DateUtils.getTodayDateString()
                val repository = FocusLockApp.instance.habitRepository

                // Use async broadcast handling to write to Room database
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        repository.logHabitCompletion(
                            habitId = habitId,
                            date = today,
                            status = true
                        )
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(
                                context,
                                "✓ Marked \"$habitTitle\" completed!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            ACTION_HABIT_SKIP -> {
                // User dismissed the habit for today
            }
        }
    }

    companion object {
        const val ACTION_HABIT_DONE = "com.example.focuslock.ACTION_HABIT_DONE"
        const val ACTION_HABIT_SKIP = "com.example.focuslock.ACTION_HABIT_SKIP"

        const val EXTRA_HABIT_ID = "EXTRA_HABIT_ID"
        const val EXTRA_HABIT_TITLE = "EXTRA_HABIT_TITLE"
        const val EXTRA_NOTIFICATION_ID = "EXTRA_NOTIFICATION_ID"
    }
}
