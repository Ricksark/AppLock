package com.example.receiver

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.data.AppDatabase
import com.example.data.SecurityLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AegisDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(context, "AegisLock Device Administrator Activated", Toast.LENGTH_SHORT).show()
        CoroutineScope(Dispatchers.IO).launch {
            AppDatabase.getInstance(context).securityLogDao().insertLog(
                SecurityLogEntity(
                    eventType = "DEVICE_ADMIN_ENABLED",
                    details = "Device Administrator privileges granted for remote wipe protection"
                )
            )
        }
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(context, "AegisLock Device Administrator Deactivated", Toast.LENGTH_SHORT).show()
        CoroutineScope(Dispatchers.IO).launch {
            AppDatabase.getInstance(context).securityLogDao().insertLog(
                SecurityLogEntity(
                    eventType = "DEVICE_ADMIN_DISABLED",
                    details = "Device Administrator privileges revoked",
                    isAlert = true
                )
            )
        }
    }

    companion object {
        fun getComponentName(context: Context): ComponentName {
            return ComponentName(context, AegisDeviceAdminReceiver::class.java)
        }

        fun isDeviceAdminActive(context: Context): Boolean {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager ?: return false
            return dpm.isAdminActive(getComponentName(context))
        }

        fun createActivationIntent(context: Context): Intent {
            return Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, getComponentName(context))
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Enabling Device Admin allows AegisLock to wipe lost or stolen devices remotely to safeguard sensitive documents and photos."
                )
            }
        }

        fun executeDeviceFactoryReset(context: Context): Boolean {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager ?: return false
            val adminComponent = getComponentName(context)
            return if (dpm.isAdminActive(adminComponent)) {
                try {
                    dpm.wipeData(0)
                    true
                } catch (e: Exception) {
                    false
                }
            } else {
                false
            }
        }
    }
}
