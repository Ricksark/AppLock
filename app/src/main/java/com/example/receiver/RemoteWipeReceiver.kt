package com.example.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.AegisApplication
import com.example.R
import com.example.data.SecurityPreferences
import com.example.service.AppLockMonitorService
import com.example.vault.VaultManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RemoteWipeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        if (action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = SecurityPreferences(context)
            if (prefs.isAppLockProtectionEnabled) {
                AppLockMonitorService.start(context)
            }
            return
        }

        if (action == "com.aistudio.applocker.ACTION_REMOTE_WIPE") {
            val enteredCode = intent.getStringExtra("code") ?: intent.getStringExtra("secret") ?: ""
            val prefs = SecurityPreferences(context)

            if (prefs.remoteWipeEnabled && enteredCode.isNotBlank() && enteredCode == prefs.remoteWipeSecret) {
                // Execute emergency remote wipe in background
                CoroutineScope(Dispatchers.IO).launch {
                    val vaultManager = VaultManager(context)
                    val result = vaultManager.executeRemoteWipe("Authenticated Remote Wipe Command Received")

                    // Also if device admin is active, optionally wipe device
                    // AegisDeviceAdminReceiver.executeDeviceFactoryReset(context)

                    // Post High-Priority Alert Notification
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val alertNotification = NotificationCompat.Builder(context, AegisApplication.CHANNEL_ID_ALERTS)
                        .setSmallIcon(R.drawable.ic_vault_logo)
                        .setContentTitle("Emergency Remote Wipe Executed")
                        .setContentText("All encrypted vault files and sensitive data shredded (${result.filesShredded} files purged).")
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setAutoCancel(true)
                        .build()

                    notificationManager.notify(9901, alertNotification)
                }
            }
        }
    }
}
