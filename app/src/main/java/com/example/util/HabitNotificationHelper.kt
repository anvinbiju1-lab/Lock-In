package com.example.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.FocusLockApp
import com.example.MainActivity
import com.example.data.entity.HabitEntity
import com.example.receiver.HabitActionReceiver

object HabitNotificationHelper {

    fun showHabitPrompt(context: Context, habit: HabitEntity) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val notificationId = (habit.id and 0x7FFFFFFF).toInt()

        // Content intent to open app
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAV_TAB", "habits")
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1: "Yes, Done"
        val doneIntent = Intent(context, HabitActionReceiver::class.java).apply {
            action = HabitActionReceiver.ACTION_HABIT_DONE
            putExtra(HabitActionReceiver.EXTRA_HABIT_ID, habit.id)
            putExtra(HabitActionReceiver.EXTRA_HABIT_TITLE, habit.title)
            putExtra(HabitActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 1,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: "Not Today"
        val skipIntent = Intent(context, HabitActionReceiver::class.java).apply {
            action = HabitActionReceiver.ACTION_HABIT_SKIP
            putExtra(HabitActionReceiver.EXTRA_HABIT_ID, habit.id)
            putExtra(HabitActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val skipPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 2,
            skipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, FocusLockApp.CHANNEL_HABIT_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Daily Habit Check-in")
            .setContentText("Did you complete ${habit.title} today?")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Did you complete ${habit.title} today?\n${habit.description.ifEmpty { "Target time: " + habit.targetTime }}")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.checkbox_on_background, "Yes, Done", donePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Not Today", skipPendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun cancelNotification(context: Context, notificationId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(notificationId)
    }
}
