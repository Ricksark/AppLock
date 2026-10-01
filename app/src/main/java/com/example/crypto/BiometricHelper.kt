package com.example.crypto

import android.content.Context
import androidx.fragment.app.FragmentActivity

object BiometricHelper {

    fun isBiometricAvailable(context: Context): Boolean {
        return BiometricPromptManager.getInstance(context).isBiometricReady()
    }

    fun getBiometricStatusMessage(context: Context): String {
        return BiometricPromptManager.getInstance(context).getStatusDescription()
    }

    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = "Biometric Verification",
        subtitle: String = "Verify your fingerprint or face to proceed",
        negativeButtonText: String = "Use Passcode",
        onSuccess: () -> Unit,
        onError: (errorCode: Int, errString: CharSequence) -> Unit,
        onFailed: () -> Unit
    ) {
        val manager = BiometricPromptManager.getInstance(activity)
        manager.showPrompt(
            activity = activity,
            title = title,
            subtitle = subtitle,
            negativeButtonText = negativeButtonText
        ) { result ->
            when (result) {
                is BiometricAuthResult.Success -> onSuccess()
                is BiometricAuthResult.Error -> onError(result.errorCode, result.errString)
                is BiometricAuthResult.Failed -> onFailed()
                is BiometricAuthResult.HardwareUnavailable -> onError(-1, result.reason)
            }
        }
    }
}
