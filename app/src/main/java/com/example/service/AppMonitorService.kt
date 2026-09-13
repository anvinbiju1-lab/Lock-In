package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.FocusLockApp
import com.example.MainActivity
import com.example.ui.blocker.BlockScreenActivity
import com.example.util.UsageStatsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AppMonitorService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    private var isMonitoring = false
    private var isScreenInteractive = true

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenInteractive = false
                }
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> {
                    isScreenInteractive = true
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, createNotification("Lock In guardian active"))

        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        isScreenInteractive = powerManager?.isInteractive ?: true

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        try {
            registerReceiver(screenReceiver, filter)
        } catch (e: Exception) {
            // Ignore if registration fails
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isMonitoring) {
            isMonitoring = true
            startMonitoringLoop()
        }
        return START_STICKY
    }

    private fun startMonitoringLoop() {
        serviceScope.launch {
            val blockerRepository = FocusLockApp.instance.appBlockerRepository
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            var lastBlockedPackage: String? = null
            var lastObservedPackage: String? = null

            while (isActive) {
                try {
                    // Check if screen is on/interactive. If phone is untouched & screen turns off, DO NOT wake device or query
                    val isCurrentlyInteractive = powerManager?.isInteractive ?: isScreenInteractive
                    if (!isCurrentlyInteractive) {
                        // Phone is asleep / screen off. Allow device to sleep completely.
                        lastBlockedPackage = null
                        delay(3000L)
                        continue
                    }

                    val foregroundPackage = UsageStatsHelper.getForegroundPackageName(this@AppMonitorService)

                    // If foreground app changed to home screen or another allowed app, reset last blocked package
                    if (foregroundPackage != null && foregroundPackage != lastObservedPackage) {
                        lastObservedPackage = foregroundPackage
                        if (foregroundPackage == packageName ||
                            foregroundPackage.contains("launcher") ||
                            foregroundPackage.contains("systemui") ||
                            foregroundPackage.contains("home")
                        ) {
                            lastBlockedPackage = null
                        }
                    }

                    if (foregroundPackage != null &&
                        foregroundPackage != packageName &&
                        !foregroundPackage.contains("launcher") &&
                        !foregroundPackage.contains("systemui") &&
                        !foregroundPackage.contains("home")
                    ) {
                        val usageMinutes = UsageStatsHelper.getDailyUsageMinutes(this@AppMonitorService, foregroundPackage)
                        val blockResult = blockerRepository.evaluateAppBlock(foregroundPackage, usageMinutes)

                        if (blockResult.shouldBlock) {
                            // CRITICAL FIX: Only start BlockScreenActivity ONCE per app session.
                            // Do NOT re-invoke startActivity in a continuous loop when the user puts the phone down!
                            if (lastBlockedPackage != foregroundPackage) {
                                lastBlockedPackage = foregroundPackage

                                val restriction = blockerRepository.getRestriction(foregroundPackage)
                                val appName = restriction?.appName ?: foregroundPackage

                                BlockScreenActivity.start(
                                    context = this@AppMonitorService,
                                    packageName = foregroundPackage,
                                    appName = appName,
                                    reason = blockResult.reason,
                                    timeSpentMinutes = blockResult.timeSpentMinutes,
                                    limitMinutes = blockResult.limitMinutes
                                )
                            }
                        } else {
                            // Allowed app or emergency bypass
                            if (lastBlockedPackage == foregroundPackage) {
                                lastBlockedPackage = null
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Fail-safe catch
                }

                delay(1200L) // Efficient polling interval
            }
        }
    }

    private fun createNotification(statusText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, FocusLockApp.CHANNEL_MONITOR_SERVICE)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("Lock In Protection Active")
            .setContentText(statusText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onDestroy() {
        isMonitoring = false
        try {
            unregisterReceiver(screenReceiver)
        } catch (e: Exception) {
            // Ignore
        }
        serviceJob.cancel()
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, AppMonitorService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AppMonitorService::class.java)
            context.stopService(intent)
        }
    }
}
