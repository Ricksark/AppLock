package com.example.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.example.data.AppDatabase
import com.example.data.SecurityPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppLockAccessibilityService : AccessibilityService() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    private var lastPackageName: String? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val pkgName = event.packageName?.toString() ?: return

        // Ignore self and system UI overlays
        if (pkgName == applicationContext.packageName ||
            pkgName == "com.android.systemui" ||
            pkgName == "android"
        ) {
            return
        }

        serviceScope.launch {
            try {
                val prefs = SecurityPreferences(applicationContext)
                if (!prefs.isAppLockProtectionEnabled) return@launch

                if (pkgName != lastPackageName) {
                    if (prefs.autoLockDelaySeconds == 0 && lastPackageName != null) {
                        LockStateManager.relockApp(lastPackageName!!)
                    }
                    lastPackageName = pkgName
                }

                val database = AppDatabase.getInstance(applicationContext)
                val appLock = database.appLockDao().getAppLock(pkgName)

                if (appLock != null && appLock.isLocked) {
                    val delayMillis = prefs.autoLockDelaySeconds * 1000L
                    val isUnlocked = LockStateManager.isAppUnlocked(pkgName, delayMillis)

                    if (!isUnlocked) {
                        AppLockOverlayLauncher.launchLockScreen(applicationContext, pkgName, appLock.appName)
                    }
                }
            } catch (e: Exception) {
                // Ignore transient lookup exceptions
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        serviceJob.cancel()
        super.onDestroy()
    }
}
