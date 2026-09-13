package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.FocusLockApp
import com.example.service.AppMonitorService
import com.example.util.UsageStatsHelper

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED ||
            intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            FocusLockApp.instance.scheduleHabitCheckWorker()

            // If usage stats permission is active, restart foreground monitor service
            if (UsageStatsHelper.hasUsageStatsPermission(context)) {
                AppMonitorService.start(context)
            }
        }
    }
}
