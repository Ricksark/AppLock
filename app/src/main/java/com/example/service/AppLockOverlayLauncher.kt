package com.example.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.AegisApplication
import com.example.R
import com.example.ui.overlay.AppLockScreenActivity

object AppLockOverlayLauncher {

    private const val NOTIFICATION_ID_FULLSCREEN_LOCK = 7721
    @Volatile
    private var lastPromptedPackage: String? = null
    @Volatile
    private var lastPromptTime: Long = 0L

    fun launchLockScreen(context: Context, packageName: String, appName: String) {
        val now = System.currentTimeMillis()
        if (packageName == lastPromptedPackage && (now - lastPromptTime) < 1500L) {
            // Prevent spamming triggers within 1.5 seconds for the same package
            return
        }
        lastPromptedPackage = packageName
        lastPromptTime = now

        val lockIntent = Intent(context, AppLockScreenActivity::class.java).apply {
            putExtra(AppLockScreenActivity.EXTRA_PACKAGE_NAME, packageName)
            putExtra(AppLockScreenActivity.EXTRA_APP_NAME, appName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }

        // 1. Direct Activity Launch
        var startedDirectly = false
        try {
            context.startActivity(lockIntent)
            startedDirectly = true
        } catch (e: Exception) {
            startedDirectly = false
        }

        // 2. High-Priority Full-Screen Intent Notification
        // On Android 10+ (API 29+), starting an Activity from a background service or accessibility service
        // can be restricted by the OS. A fullScreenIntent notification bypasses this restriction
        // and immediately presents the lock activity over the targeted app!
        try {
            val pendingIntent = PendingIntent.getActivity(
                context,
                packageName.hashCode(),
                lockIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

            val notification = NotificationCompat.Builder(context, AegisApplication.CHANNEL_ID_ALERTS)
                .setSmallIcon(R.drawable.ic_vault_logo)
                .setContentTitle("Protected: $appName")
                .setContentText("AegisLock authentication required")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(pendingIntent, true)
                .setAutoCancel(true)
                .setOngoing(false)
                .build()

            notificationManager?.notify(NOTIFICATION_ID_FULLSCREEN_LOCK, notification)
        } catch (e: Exception) {
            // Ignore notification failure
        }
    }

    fun dismissLockNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(NOTIFICATION_ID_FULLSCREEN_LOCK)
    }
}
