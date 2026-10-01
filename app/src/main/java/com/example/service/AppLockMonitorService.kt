package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.AegisApplication
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.SecurityPreferences
import com.example.ui.overlay.AppLockScreenActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AppLockMonitorService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    private var lastCheckedPackage: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()
        startMonitoringLoop()
    }

    private fun startForegroundNotification() {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, AegisApplication.CHANNEL_ID_MONITOR)
            .setContentTitle("AegisLock Active Shield")
            .setContentText("Real-time biometric & passcode app protection enabled")
            .setSmallIcon(R.drawable.ic_vault_logo)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startMonitoringLoop() {
        serviceScope.launch {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            val database = AppDatabase.getInstance(applicationContext)
            val preferences = SecurityPreferences(applicationContext)

            while (isActive) {
                try {
                    if (preferences.isAppLockProtectionEnabled && usageStatsManager != null) {
                        val foregroundPkg = getForegroundPackageName(usageStatsManager)

                        if (foregroundPkg != null &&
                            foregroundPkg != packageName &&
                            foregroundPkg != "com.google.android.packageinstaller" &&
                            foregroundPkg != "com.android.packageinstaller"
                        ) {
                            if (foregroundPkg != lastCheckedPackage) {
                                // Check if user switched apps; if auto-lock delay is 0, previous app relocks
                                if (preferences.autoLockDelaySeconds == 0 && lastCheckedPackage != null) {
                                    LockStateManager.relockApp(lastCheckedPackage!!)
                                }
                                lastCheckedPackage = foregroundPkg
                            }

                            // Check if this package is locked in database
                            val appLock = database.appLockDao().getAppLock(foregroundPkg)
                            if (appLock != null && appLock.isLocked) {
                                val delayMillis = preferences.autoLockDelaySeconds * 1000L
                                val isUnlocked = LockStateManager.isAppUnlocked(foregroundPkg, delayMillis)

                                if (!isUnlocked) {
                                    launchLockOverlay(foregroundPkg, appLock.appName)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Suppress and continue monitoring
                }
                delay(800)
            }
        }
    }

    private fun getForegroundPackageName(usageStatsManager: UsageStatsManager): String? {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 10000L
        val events = usageStatsManager.queryEvents(startTime, endTime)
        var foregroundPkg: String? = null
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
            ) {
                foregroundPkg = event.packageName
            }
        }

        if (foregroundPkg == null) {
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )
            foregroundPkg = stats?.maxByOrNull { it.lastTimeUsed }?.packageName
        }
        return foregroundPkg
    }

    private fun launchLockOverlay(packageName: String, appName: String) {
        AppLockOverlayLauncher.launchLockScreen(this, packageName, appName)
    }

    override fun onDestroy() {
        serviceJob.cancel()
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 8801

        fun start(context: Context) {
            val intent = Intent(context, AppLockMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AppLockMonitorService::class.java)
            context.stopService(intent)
        }
    }
}
