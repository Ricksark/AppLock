package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.data.AppDatabase
import com.example.data.SecurityPreferences

class AegisApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var preferences: SecurityPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getInstance(this)
        preferences = SecurityPreferences(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_MONITOR,
                getString(R.string.applock_service_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.applock_service_channel_desc)
                setShowBadge(false)
            }
            val alertChannel = NotificationChannel(
                CHANNEL_ID_ALERTS,
                "AegisLock Security Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Security and remote wipe alert notifications"
                setShowBadge(true)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }

    companion object {
        const val CHANNEL_ID_MONITOR = "aegis_applock_monitor"
        const val CHANNEL_ID_ALERTS = "aegis_security_alerts"

        lateinit var instance: AegisApplication
            private set
    }
}
