package com.example.service

import java.util.concurrent.ConcurrentHashMap

object LockStateManager {
    // Map of packageName to unlock timestamp
    private val unlockedApps = ConcurrentHashMap<String, Long>()
    
    @Volatile
    var currentForegroundPackage: String? = null

    fun isAppUnlocked(packageName: String, timeoutMillis: Long = 0L): Boolean {
        val unlockTime = unlockedApps[packageName] ?: return false
        if (timeoutMillis <= 0L) {
            // Immediate relock mode: stays unlocked while the app is in the foreground
            return true
        }
        val elapsed = System.currentTimeMillis() - unlockTime
        return elapsed < timeoutMillis
    }

    fun markAppUnlocked(packageName: String) {
        unlockedApps[packageName] = System.currentTimeMillis()
    }

    fun relockApp(packageName: String) {
        unlockedApps.remove(packageName)
    }

    fun clearAllUnlocked() {
        unlockedApps.clear()
    }
}
